## Why

文章创作已经拆成标题、大纲、正文、配图分析、配图生成、图文合成等阶段，并且标题选择、大纲确认天然需要 Human-in-the-Loop。前一版“通用创作 workflow 内核 + 文章适配器”抽象过早：它会把 rednote 等未来业务强行套进同一套模板，也没有真正让 Spring AI Alibaba `StateGraph` 成为执行入口。

本次调整为更克制的方向：先把文章 workflow 做实，使用 Spring AI Alibaba Workflow 编排文章节点，并用框架 checkpoint + `threadId` 支撑人工中断后的恢复。未来新增 rednote 时，单独定义 rednote workflow，不复用文章的流程模板。

## What Changes

- 新增文章专属 `ArticleWorkflowGraphFactory`，直接构建 Spring AI Alibaba `StateGraph`。
- 标题生成和大纲生成后设置框架级中断点，由后端创建 `workflow_human_task` 并进入 `WAITING_USER`。
- 用户确认标题/大纲后，服务端校验人工任务、写入 Graph checkpoint，再用相同 `threadId` 恢复 `CompiledGraph`。
- 将 Graph checkpoint 从内存迁移到 Redis，并通过配置化 TTL 控制等待确认的最长恢复窗口，默认 7 天。
- checkpoint 过期后，不能继续消费旧人工任务；系统应把 workflow 和 human task 标记为过期，并让用户显式重新生成。
- 保留 workflow 任务、人工任务、事件和 SSE 兼容映射作为横向基础设施。
- 删除自定义 `WorkflowEngine`、`WorkflowDefinition`、`WorkflowNode` 等通用模板抽象。
- 保留 `rednote` 命名规范：如果后续新增枚举，使用 `REDNOTE("rednote")`。

## Capabilities

### New Capabilities

- `creation-workflow-template`: 现在限定为“文章 Spring AI Alibaba workflow + Human-in-the-Loop 基础设施”，不再承诺通用模板内核。

### Modified Capabilities

- None.

## Impact

- 文章创建、标题确认、大纲确认 API 对外契约保持不变，内部改为 `ArticleWorkflowFacade -> CompiledGraph`。
- `workflow_task`、`workflow_human_task` 继续用于状态恢复、人工任务校验、过期判断和 SSE 重连补发。
- 需要新增 workflow / human task 过期状态，以及 checkpoint TTL 配置。
- 未来 rednote 不继承文章 workflow；它应定义自己的 `StateGraph`、节点动作和人工交互表单。
