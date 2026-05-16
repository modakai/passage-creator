## Why

当前系统已经有文章创作 workflow，但小红书爆款笔记的输入理解、搜索整理、封面目标、标签策略和输出形态都不同。如果把小红书能力塞进文章 workflow，会让文章流程和后续 rednote 流程互相绑死，违背现有文档中“rednote 独立定义 StateGraph”的设计边界。

## What Changes

- 新增全自动小红书爆款创作能力，用户提交一段自然语言 `content` 后，系统自动完成搜索整理、结构化创作简报生成、文案生成、图片提示词生成、普通配图和封面生成。
- 新增 `rednote` 业务数据表，用于保存用户原始输入、SearchAgent 结构化简报、小红书内容、图片提示词、普通配图、封面、标签、状态和错误信息。
- 新增独立的 `RednoteWorkflowGraphFactory` 设计，复用通用 `workflow_task`、Redis checkpoint 和 workflow 事件基础设施，但不复用文章 Graph。
- 允许将 SearchAgent、CopywritingAgent、ImagePromptAgent 作为 StateGraph node 运行，Agent 可按职责配置必要 tools 或 skills。
- 小红书第一版不引入人工确认节点，不创建 `workflow_human_task`，失败后由用户显式重试或重新生成。
- 图片生成节点根据图片提示词并行生成配图，普通配图数量最多 5 张，不包含封面图；图片生成完成后 workflow 结束。

## Capabilities

### New Capabilities

- `rednote-workflow`: 定义全自动小红书爆款创作 workflow、业务存储、节点输出和失败重试规则。

### Modified Capabilities

- `creation-workflow-template`: 明确 `rednote` 创作类型复用通用 workflow 任务、checkpoint 和事件基础设施，但拥有独立业务表和独立 StateGraph。

## Impact

- 后端新增 rednote 业务模块、业务表 SQL、实体/Mapper、DTO/VO、Agent node、普通 NodeAction、服务、控制器和 workflow 节点处理器。
- 数据库新增 MySQL 与 PostgreSQL 的 rednote 表结构迁移脚本。
- 需要调整已经创建的 rednote 表和模型，从 `topic/targetAudience/style/referenceMaterials` 输入模型迁移为 `content -> SearchAgent -> RednoteBrief` 模型。
- workflow 事件发布层需要识别 `bizType = rednote`，后续前端可复用通用进度事件展示自动生成阶段。
- 现有文章创作接口、文章业务表和文章 StateGraph 不做破坏性调整。
