## Context

现有文章创作已经使用 Spring AI Alibaba `StateGraph` 编排，并通过 `workflow_task`、Redis checkpoint、workflow 事件和 SSE 兼容层保存流程状态。历史设计已经明确：未来 `rednote` 不应复用文章 Graph，也不应抽一个过度通用的 workflow 模板。

小红书爆款创作和文章创作的核心差异不只是 Prompt 风格，而是输入理解和输出结构不同。文章围绕标题、大纲、正文和配图；小红书笔记围绕用户自然语言意图、搜索整理后的创作简报、小红书正文、标签、图片组和封面吸引力。若直接改造文章表或文章节点，会把两套内容模型混在一起。

## Goals / Non-Goals

**Goals:**

- 新增全自动 `rednote` 创作链路，用户提交一段自然语言 `content` 后自动产出可发布的小红书笔记结果。
- 为 rednote 建立独立业务表，保存用户原始输入、SearchAgent 结构化简报、小红书内容、图片提示词、普通配图、封面、标签、状态和错误信息。
- 新增独立 `RednoteWorkflowGraphFactory`，复用通用 workflow 任务、checkpoint 和事件基础设施。
- 支持将 SearchAgent、CopywritingAgent、ImagePromptAgent 作为 StateGraph node，并按职责配置 tools 或 skills。
- 第一版不设置 Human-in-the-Loop 节点，Graph 从开始执行到结束或失败。
- 表结构同时覆盖 MySQL 和 PostgreSQL，保持项目双数据库脚本风格一致。

**Non-Goals:**

- 不复用 `ArticleWorkflowGraphFactory`，不向文章 workflow 增加 rednote 分支。
- 不让文案 Agent 直接消费未清洗的搜索结果原文；SearchAgent 必须先产出稳定结构化简报。
- 不做用户人工确认、局部编辑、A/B 标题测试或发布到小红书平台。
- 不把 rednote 业务结果只保存到 `workflow_task.context_json`；业务表仍是结果查询主存储。

## Decisions

### Decision: Rednote 使用独立业务表

新增 `rednote_note` 表作为小红书任务主表，字段覆盖 `task_id`、`user_id`、用户原始 `content`、SearchAgent 解析出的 `subject`、结构化 `context`、`content_length`、`target_word_count`、`keywords`、`tag_count`、`image_count`、`search_results`、小红书内容、标签、图片提示词、图片列表、封面提示词、封面标题、封面图、状态、阶段和错误信息。

原因：小红书节点输出结构与文章不同，独立表能让列表、详情、重试和后台排查直接读取领域数据，而不是反复解析 Graph state。

备选方案是复用 `article` 表并加平台字段。该方案短期少建表，但会导致文章字段语义膨胀，例如 `outline`、`main_title`、`sub_title` 对 rednote 并不自然，长期会让两个流程互相污染。

### Decision: 第一版只有主表，不拆多张中间表

第一版只建 `rednote_note` 主表，结构化中间结果用 JSON 字段保存，例如 `keywords`、`search_results`、`image_prompts`、`images`、`tags`。不先拆 `rednote_image_task`、`rednote_tag` 等子表。

原因：当前目标是先引入全自动 workflow 和业务结果落库，查询主要按用户、状态、时间和 taskId。过早拆子表会增加 Mapper、事务和回滚复杂度，但暂时没有明确查询收益。

备选方案是从第一版就拆图片任务表。该方案适合图片重试、单图状态流转和素材审计，但用户当前要求先创建表结构引入能力，子表可在图片重试需求明确后追加。

### Decision: SearchAgent 接收 content 并产出 RednoteBrief

前端第一版只需要提交自然语言 `content`。`content` 可以包含主题、字数描述、关键词偏好、标签数量、图片数量、产品/场景描述等混合信息。SearchAgent 负责解析用户意图、调用搜索或素材工具、整理搜索结果，并产出稳定的 RednoteBrief：

```json
{
  "subject": "核心主体/产品/场景",
  "context": "搜索结果和用户意图整合后的创作上下文",
  "contentLength": "MEDIUM",
  "targetWordCount": 600,
  "keywords": ["关键词1", "关键词2"],
  "tagCount": 5,
  "imageCount": 3,
  "searchResults": [
    {
      "title": "参考标题",
      "summary": "可用于创作的摘要",
      "sourceName": "来源名称",
      "sourceUrl": "https://example.com"
    }
  ]
}
```

原因：自然语言输入对用户最简单，但后续节点需要稳定 schema。SearchAgent 不能把未清洗的搜索结果原文直接喂给文案 Agent，否则文案质量、可追踪性和失败定位都会变差。

备选方案是前端拆成多个表单字段。该方案利于校验，但会牺牲小红书创作的轻量输入体验，也把“理解用户意图”的工作推给用户。

### Decision: Agent 作为 StateGraph node

SearchAgent、CopywritingAgent、ImagePromptAgent 使用 Spring AI Alibaba Agent as Node 模式接入 StateGraph。每个 Agent 设置明确 `outputKey`，只暴露该节点所需 tools 或 skills：

- SearchAgent: 搜索、摘要、素材清洗工具。
- CopywritingAgent: 基于 RednoteBrief 生成结构化文案，不直接访问搜索工具。
- ImagePromptAgent: 基于结构化文案生成封面提示词和普通配图提示词。

原因：Agent as Node 适合需要工具调用和多步推理的节点，同时 StateGraph 仍负责阶段顺序、checkpoint、事件和失败状态。工具权限按 Agent 职责收窄，避免一个 Agent 拿到过多工具导致行为不可控。

SearchAgent 的业务状态同步使用 Agent Hook 完成：`BEFORE_AGENT` 将 rednote 任务推进到搜索阶段，`AFTER_AGENT` 解析 RednoteBrief、写入 `rednote_note` 并返回后续节点可读的扁平 state。网页搜索工具属于外部 I/O，第一版使用 Spring AI Alibaba 内置 `ToolRetryInterceptor` 对 `rednote_web_search` 做短暂重试和错误消息回传。

备选方案是所有节点都用普通 `NodeAction` + ChatClient。该方案更可控，但 SearchAgent 的搜索、分析和自动补全逻辑会变得僵硬，工具调用扩展性较差。

### Decision: 图片生成使用普通 NodeAction 并行执行

图片生成不作为 ReactAgent，而是普通 `NodeAction`。它读取 ImagePromptAgent 生成的 `coverPrompt` 和 `imagePrompts`，并行调用现有图片生成服务，保存封面图和普通配图 URL。

普通配图数量由后端强制限制：

```text
imageCount = min(SearchAgent 输出或默认值, 5)
```

封面图不计入 5 张普通配图上限。

原因：图片生成是确定性 I/O 编排，不需要 Agent 自主推理。将其做成普通节点更容易控制并发、重试、失败记录和成本统计。

备选方案是让 ImagePromptAgent 直接调用图片工具。该方案看似一步完成，但会把提示词设计、图片生成、失败重试和下载能力混在一个 Agent 里，难以测试。

### Decision: Rednote Graph 不设置人工中断

Rednote 第一版节点顺序为：

```text
START
  -> SEARCH_AGENT
  -> COPY_GENERATING
  -> IMAGE_PROMPT_GENERATING
  -> IMAGE_GENERATING
  -> END
```

每个节点把结果写入 `rednote_note`，同时返回 Graph state。失败时 `workflow_task` 和 `rednote_note` 都进入失败状态，用户后续可重新生成或按节点重试。封面图由 `IMAGE_GENERATING` 节点和普通配图一起处理，但封面图独立保存到 `cover_image`。图片生成完成后 workflow 结束，前端分开展示小红书内容、普通配图和封面。

原因：用户明确选择全自动生成。此时 `workflow_human_task` 不是必需能力，强行保留确认节点会让体验和实现都偏离目标。

备选方案是在文案或封面阶段暂停确认。该方案质量可控，但不是本次范围；后续如果要加入人工确认，可以沿用现有 Human-in-the-Loop 基础设施独立扩展。

## Risks / Trade-offs

- [Risk] 单表 JSON 字段后续不便于复杂统计。Mitigation: 第一版只做任务级查询；当出现明确统计需求时再拆 tags/images 子表。
- [Risk] 全自动生成质量不稳定。Mitigation: 保留结构化节点输出，失败和低质量问题可以定位到具体节点，后续再加人工确认或反馈。
- [Risk] `workflow_task.context_json` 与 `rednote_note` 双写不一致。Mitigation: 领域结果以 `rednote_note` 为准，Graph state 只负责执行恢复和节点传递。
- [Risk] SearchAgent 工具调用结果不稳定或不可用。Mitigation: SearchAgent 必须产出 RednoteBrief；搜索失败时可以基于用户 content 生成降级 brief，并记录 searchResults 为空和错误摘要。
- [Risk] 封面图和配图策略复用文章图片服务时语义不完全匹配。Mitigation: Rednote 节点先生成明确的封面提示词、普通配图提示词和图片用途，再调用图片服务，避免把正文配图占位符逻辑硬套到小红书。
- [Risk] 图片并行生成造成成本或并发压力。Mitigation: 后端强制普通配图数量最多 5 张，封面单独 1 张，并在图片节点集中做并发控制和用量记录。

## Migration Plan

1. 调整 MySQL 和 PostgreSQL 的 `rednote_note` 表结构脚本，迁移到 `content -> SearchAgent -> RednoteBrief` 字段模型。
2. 调整 rednote 实体、Mapper、DTO、VO、枚举和服务骨架。
3. 新增 rednote 节点处理器和 `RednoteWorkflowGraphFactory`。
4. 新增创建、详情、列表和进度接口，进度复用通用 workflow 事件。
5. 接入前端入口和结果展示。
6. 回滚时隐藏入口和接口调用；已创建的 rednote 表不影响文章主流程。

## Open Questions

- 封面图第一版是复用普通图片生成策略，还是需要单独使用竖版封面比例和标题排版策略。
- SearchAgent 第一版使用哪个搜索工具或素材工具；无论工具选择如何，输出 RednoteBrief schema 不变。
- 是否要对 rednote 生成消耗单独配置积分费率；第一版可沿用现有 AI 用量记录能力。
