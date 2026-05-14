## 1. Data Model And Naming

- [x] 1.1 Add workflow task persistence model for task id, biz type, biz id, user id, status, current node, context snapshot, error message, and timestamps.
- [x] 1.2 Add workflow human task persistence model for task id, biz type, node type, assignee user id, form schema, input snapshot, result, status, version, and completion time.
- [x] 1.3 Define creation type constants or enum values using `article` and reserved `rednote` persisted values.
- [x] 1.4 Add migration scripts for workflow tables without changing existing article table semantics.

## 2. Spring AI Alibaba Article Workflow

- [x] 2.1 Create article `StateGraph` factory with title, title confirmation, outline, outline confirmation, content, image analysis, image generation, and merge nodes.
- [x] 2.2 Compile article graph with checkpoint saver and `interruptAfter` for title generation and outline generation.
- [x] 2.3 Convert article workflow node handlers to Spring AI Alibaba `NodeAction`.
- [x] 2.4 Remove custom `WorkflowEngine`, `WorkflowDefinition`, `WorkflowDefinitionRegistry`, and `WorkflowNode` template abstractions.
- [x] 2.5 Preserve `WorkflowContext` only as a structured snapshot helper, not as a public workflow template API.

## 3. Human-in-the-Loop

- [x] 3.1 Implement HumanTaskService for creating, loading, completing, and validating human tasks.
- [x] 3.2 Enforce assignee user id, waiting node type, task status, and version checks before human task completion.
- [x] 3.3 Define title confirmation form schema and outline confirmation form schema for article workflow.
- [x] 3.4 Store completed human task results as structured JSON.
- [x] 3.5 Write confirmed human results into Spring AI Alibaba graph checkpoint before resume.

## 4. Article Workflow Integration

- [x] 4.1 Create ArticleWorkflowAdapter to map workflow context to ArticleState and persist article title options, selected title, outline, content, images, cover image, and final Markdown.
- [x] 4.2 Wrap existing TitleGeneratorAgent, OutlineGeneratorAgent, ContentGeneratorAgent, ImageAgent, and ContentImageMerger behind article graph node actions.
- [x] 4.3 Move article status and phase progression rules out of controller-level branching and into article workflow node completion logic.
- [x] 4.4 Preserve existing article business records as the source of truth for article list/detail queries.

## 5. Events And API Integration

- [x] 5.1 Implement WorkflowEventPublisher with generic workflow events such as NODE_STARTED, NODE_RESULT, NODE_WAITING_USER, WORKFLOW_COMPLETED, and WORKFLOW_FAILED.
- [x] 5.2 Add article event mapping so existing article SSE clients remain compatible during migration.
- [x] 5.3 Refactor ArticleWorkflowFacade to create article workflow, invoke `CompiledGraph`, complete title human task, complete outline human task, and resume graph execution.
- [x] 5.4 Refactor article app controller to delegate workflow operations to ArticleWorkflowFacade while preserving public endpoint contracts.
- [x] 5.5 Ensure SSE reconnect can recover current workflow progress and pending human task payload.

## 6. Verification

- [x] 6.1 Add focused test proving article `StateGraph` interrupts after title generation and resumes from Spring AI Alibaba checkpoint.
- [x] 6.2 Add focused tests for human task validation, including duplicate submission and stale version cases.
- [x] 6.3 Add focused tests for article workflow adapter persistence behavior.
- [x] 6.4 Run backend test suite or targeted Maven tests for changed modules.
- [x] 6.5 Manually verify the article happy path: create task, generate titles, confirm title, generate outline, confirm outline, generate content/images, complete workflow.

## 7. Redis Checkpoint TTL And Expiration

- [x] 7.1 Add configurable checkpoint TTL, defaulting to 7 days, for article workflow HITL recovery.
- [x] 7.2 Replace in-memory checkpoint saver with Redis-backed checkpoint saver.
- [x] 7.3 Ensure Redis checkpoint keys and thread metadata expire together, either through a TTL-aware saver wrapper or a dedicated Redis expiration adapter.
- [x] 7.4 Add `EXPIRED` workflow status and human task status for checkpoint expiration.
- [x] 7.5 Add human task `expire_time` persistence so database state can detect expiration even after Redis keys disappear.
- [x] 7.6 Change confirm-title and confirm-outline order to check checkpoint existence and update checkpoint before completing the human task.
- [x] 7.7 On SSE reconnect or confirmation after expiration, mark task state as expired and publish `WORKFLOW_EXPIRED`.
- [x] 7.8 Add explicit regenerate behavior for expired title/outline confirmation stages; do not auto-regenerate.
- [x] 7.9 Add tests for Redis checkpoint resume after restart, checkpoint expiration, stale human task rejection, and regenerate entry behavior.
