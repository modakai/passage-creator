## 1. 后端数据模型与迁移

- [x] 1.1 调整 MySQL `rednote_note` 表迁移脚本，迁移为 `content -> SearchAgent -> RednoteBrief` 字段模型，包含 `content`、`subject`、`context`、`content_length`、`target_word_count`、`keywords`、`tag_count`、`image_count`、`search_results` 和 `cover_prompt`
- [x] 1.2 调整 PostgreSQL `rednote_note` 表迁移脚本，并补齐新字段注释和查询索引
- [x] 1.3 将调整后的 `rednote_note` 表结构同步到 MySQL 与 PostgreSQL 初始化建表脚本
- [x] 1.4 调整 rednote 状态枚举、阶段枚举、实体、Mapper、DTO、VO 和查询请求对象，使其匹配单字段 `content` 输入和 RednoteBrief 输出模型

## 2. Rednote Workflow 编排

- [ ] 2.1 新增 `RednoteWorkflowNodeType`，覆盖 SearchAgent、文案生成 Agent、图片提示词 Agent 和图片生成节点
- [ ] 2.2 新增 rednote workflow state 字段定义，明确 taskId、content、subject、context、contentLength、targetWordCount、keywords、tagCount、imageCount、searchResults、copywriting、coverPrompt、imagePrompts、images、coverImage、tags 等状态键
- [ ] 2.3 新增 `RednoteWorkflowGraphFactory`，使用独立 StateGraph 串联 Agent-as-Node 和普通 NodeAction，并复用 Redis checkpoint saver
- [ ] 2.4 新增 rednote workflow lifecycle listener，将节点开始、节点结果、完成和失败事件同步到 `workflow_task` 与 SSE 事件

## 3. Rednote Agent 与节点处理

- [ ] 3.1 新增 SearchAgent 节点，接收用户原始 `content`，调用搜索/摘要/素材工具，并输出 subject、context、contentLength、targetWordCount、keywords、tagCount、imageCount、searchResults
- [ ] 3.2 新增文案生成 Agent 节点，仅基于 RednoteBrief 输出开头钩子、正文主体、行动引导、标签建议和封面标题建议
- [ ] 3.3 新增图片提示词 Agent 节点，输出封面提示词和普通图片提示词列表，并保证普通图片提示词数量最多 5 条
- [ ] 3.4 新增图片生成普通节点，复用现有图片服务并行生成普通配图和封面图，保存图片 URL、提示词、位置、状态和失败原因，并在完成后结束 workflow

## 4. 后端接口与业务服务

- [ ] 4.1 新增 rednote 创建接口，接收单字段 `content`，创建 rednote 业务记录和 `bizType = rednote` 的 workflow 任务
- [ ] 4.2 新增 rednote 详情接口，从 rednote 业务表返回输入、阶段、状态、小红书内容、图片、封面和标签
- [ ] 4.3 新增 rednote 列表接口，支持按用户、状态、创建时间分页查询
- [ ] 4.4 新增 rednote 失败任务重新生成接口，明确创建新的可执行 workflow 状态
- [ ] 4.5 接入积分和 AI 用量记录，确保文本与图片节点调用可计费、可追踪

## 5. 前端入口与结果展示

- [ ] 5.1 新增小红书创作 API 类型和请求封装
- [ ] 5.2 新增小红书创作入口表单，使用单个自然语言 `content` 输入框承载主题、字数、关键词、标签数和图片数量等描述
- [ ] 5.3 新增小红书生成进度展示，按 rednote 节点展示全自动执行状态
- [ ] 5.4 新增小红书结果页，分开展示小红书内容、普通配图、封面和标签
- [ ] 5.5 在用户入口或侧边栏加入小红书创作入口

## 6. 验证

- [ ] 6.1 增加表结构脚本校验，确认 MySQL 与 PostgreSQL 字段语义一致
- [ ] 6.2 增加 `RednoteWorkflowGraphFactory` 测试，验证节点顺序从 START 到 SearchAgent、文案 Agent、图片提示词 Agent、图片生成再到 END，且不创建人工任务
- [ ] 6.3 增加 rednote 服务层测试，覆盖单字段 content 创建任务、RednoteBrief 落库、图片数量上限、图片生成完成即完成 workflow、失败状态记录和详情查询
- [ ] 6.4 增加前端关键逻辑测试或临时验证，覆盖创建表单、进度事件处理和结果展示
- [ ] 6.5 运行后端相关测试与前端类型检查/构建，确认文章 workflow 和 rednote workflow 互不破坏
