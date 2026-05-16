## ADDED Requirements

### Requirement: 用户可以创建全自动小红书爆款任务
系统 SHALL 允许已登录用户提交一段自然语言创作需求 `content`，并创建 `rednote` 类型的 workflow 任务。

#### Scenario: 创建小红书任务
- **WHEN** 已登录用户提交小红书创作需求 `content`
- **THEN** 系统创建 rednote 业务记录和 `bizType = rednote` 的 workflow 任务

#### Scenario: 小红书任务拥有独立业务主键
- **WHEN** 系统创建 rednote workflow 任务
- **THEN** `workflow_task.biz_id` SHALL 指向 rednote 业务记录主键，而不是文章表主键

### Requirement: 小红书 workflow 全自动执行
系统 SHALL 使用独立 StateGraph 自动执行小红书创作节点，第一版不得要求用户在中间阶段人工确认。

#### Scenario: 自动执行完整节点链路
- **WHEN** rednote workflow 启动
- **THEN** 系统依次执行搜索整理 Agent、文案生成 Agent、图片提示词 Agent 和图片生成节点

#### Scenario: 全自动流程不创建人工任务
- **WHEN** rednote workflow 正常执行到中间节点
- **THEN** 系统 MUST NOT 创建 `workflow_human_task` 或把 workflow 状态标记为 `WAITING_USER`

### Requirement: SearchAgent 将自然语言输入转换为结构化创作简报
系统 SHALL 让 SearchAgent 接收用户原始 `content`，并在搜索和分析后输出稳定的结构化 RednoteBrief。

#### Scenario: SearchAgent 解析用户原始输入
- **WHEN** 用户提交包含主题、字数描述、关键词偏好或图片数量描述的 `content`
- **THEN** SearchAgent SHALL 解析或推断 `subject`、`context`、`contentLength`、`targetWordCount`、`keywords`、`tagCount` 和 `imageCount`

#### Scenario: SearchAgent 保存搜索结果摘要
- **WHEN** SearchAgent 调用搜索或素材工具并得到结果
- **THEN** 系统 SHALL 保存结构化 `searchResults`，每条结果包含标题、摘要、来源名称和可选来源链接

#### Scenario: 搜索不可用时降级生成简报
- **WHEN** 搜索工具不可用或没有有效搜索结果
- **THEN** SearchAgent SHALL 基于用户原始 `content` 生成 RednoteBrief，并记录空搜索结果或错误摘要

#### Scenario: 文案 Agent 不直接消费未清洗搜索结果
- **WHEN** CopywritingAgent 开始生成文案
- **THEN** 系统 SHALL 只向其提供 RednoteBrief 和必要上下文，而不是未清洗的搜索结果原文

### Requirement: 小红书业务结果独立存储
系统 SHALL 将小红书创作输入、中间结构化结果和图片生成结果保存到 rednote 业务表。

#### Scenario: 节点输出保存到业务表
- **WHEN** rednote workflow 节点产生 RednoteBrief、钩子文案、正文、标签、图片提示词、普通配图或封面
- **THEN** 系统将该节点结果保存到 rednote 业务表，并同步必要状态到 workflow 上下文

#### Scenario: 详情查询读取业务表
- **WHEN** 用户查询小红书任务详情
- **THEN** 系统从 rednote 业务表返回用户原始输入、RednoteBrief、生成状态、阶段、小红书内容、图片、封面和标签

### Requirement: 小红书创作保留结构化爆款内容要素
系统 SHALL 将小红书笔记拆分为可追踪的结构化要素，而不是只保存一段不可解析文本。

#### Scenario: 生成结构化文案
- **WHEN** 文案生成节点完成
- **THEN** 系统保存开头钩子、正文主体、行动引导、标签建议和封面标题建议

#### Scenario: 生成结构化图片计划
- **WHEN** 图片提示词生成节点完成
- **THEN** 系统保存图片提示词列表，且每个提示词 SHALL 包含用途、位置、提示词文本和优先级

#### Scenario: 生成封面图片提示词
- **WHEN** 图片提示词生成节点完成
- **THEN** 系统 SHALL 保存封面标题和封面图片提示词，且封面不计入普通配图数量上限

### Requirement: 普通配图数量受后端强制限制
系统 SHALL 在后端强制限制普通配图数量最多 5 张，且该限制不包含封面图。

#### Scenario: 用户要求超过 5 张普通配图
- **WHEN** 用户原始 `content` 或 SearchAgent 输出要求普通配图数量超过 5
- **THEN** 系统 SHALL 将普通配图数量截断为 5，并继续生成封面图

#### Scenario: 用户未指定图片数量
- **WHEN** 用户原始 `content` 未指定图片数量
- **THEN** SearchAgent SHALL 使用默认普通配图数量，并且默认值 MUST NOT 超过 5

### Requirement: 图片生成节点并行生成图片并结束 workflow
系统 SHALL 根据图片提示词并行生成普通配图和封面图，并在图片生成完成后结束 workflow。

#### Scenario: 并行生成普通配图
- **WHEN** 图片生成节点收到普通图片提示词列表
- **THEN** 系统并行调用图片生成服务，保存每张图片的 URL、提示词、位置和生成状态

#### Scenario: 生成封面图
- **WHEN** 图片生成节点收到封面图片提示词
- **THEN** 系统生成封面图，并保存封面图 URL

#### Scenario: 图片生成完成后 workflow 完成
- **WHEN** 普通配图和封面图生成节点完成
- **THEN** 系统 SHALL 将 workflow 标记为完成，并允许前端分开展示小红书内容、图片和封面

#### Scenario: 部分图片生成失败
- **WHEN** 普通配图或封面图部分生成失败
- **THEN** 系统 SHALL 保存成功图片和失败原因，并由 workflow 失败策略决定是否整体失败或允许后续重试

### Requirement: 小红书 workflow 失败可追踪
系统 SHALL 在 rednote workflow 失败时记录失败阶段和错误信息。

#### Scenario: 节点失败后记录错误
- **WHEN** rednote workflow 任一节点执行失败
- **THEN** 系统将 `workflow_task` 和 rednote 业务记录标记为失败，并保存当前节点和错误信息

#### Scenario: 用户可重新生成失败任务
- **WHEN** 用户对失败的 rednote 任务发起重新生成
- **THEN** 系统创建新的可执行 workflow 状态，且不得静默复用已失败的不完整结果作为最终结果

### Requirement: Agent 节点可以按职责使用工具
系统 SHALL 允许 rednote workflow 中的 Agent 节点按职责配置 tools 或 skills，但每个 Agent 只能访问完成自身任务所需的工具。

#### Scenario: SearchAgent 使用搜索工具
- **WHEN** SearchAgent 执行搜索整理节点
- **THEN** SearchAgent MAY 使用搜索、摘要或素材清洗工具，并将结果整理为 RednoteBrief

#### Scenario: CopywritingAgent 不访问图片生成工具
- **WHEN** CopywritingAgent 生成小红书文案
- **THEN** 系统 MUST NOT 为其提供图片生成工具

#### Scenario: ImagePromptAgent 不直接生成图片
- **WHEN** ImagePromptAgent 生成封面和普通配图提示词
- **THEN** 系统 MUST NOT 让该 Agent 直接调用图片生成服务
