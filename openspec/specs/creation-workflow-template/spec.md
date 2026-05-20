## Purpose
定义基于 Spring AI Alibaba StateGraph 的文章创作 workflow，以及 Human-in-the-Loop、Redis checkpoint TTL、过期恢复和未来 `rednote` 创作类型预留规则。
## Requirements
### Requirement: Article workflow is orchestrated by Spring AI Alibaba StateGraph
The system SHALL execute article creation through a Spring AI Alibaba `StateGraph` compiled into `CompiledGraph`.

#### Scenario: Article graph starts from title generation
- **WHEN** a user starts an article creation task
- **THEN** the system creates a workflow task and invokes the article `CompiledGraph` with `threadId = taskId`

#### Scenario: Article graph resumes from checkpoint
- **WHEN** a paused article workflow receives valid human input
- **THEN** the system writes the human result into the graph checkpoint and resumes the same `CompiledGraph` thread

### Requirement: Article workflow checkpoint is persisted with TTL
The system SHALL persist article workflow checkpoints in Redis and expire waiting Human-in-the-Loop checkpoints after a configurable duration.

#### Scenario: Checkpoint survives service restart within TTL
- **WHEN** the backend restarts while an article workflow is waiting for title or outline confirmation
- **AND** the checkpoint TTL has not expired
- **THEN** the user can submit the pending human task and the system resumes the same workflow thread

#### Scenario: Checkpoint expires after TTL
- **WHEN** an article workflow remains waiting for human confirmation beyond the configured checkpoint TTL
- **THEN** the Redis checkpoint is no longer used for resume and the workflow is treated as expired

#### Scenario: TTL is configurable
- **WHEN** the application starts
- **THEN** the workflow checkpoint TTL is read from configuration with a default of 7 days

### Requirement: Human-in-the-Loop pauses article workflow
The system SHALL pause article workflow after title generation and outline generation until the assigned user completes the corresponding human task.

#### Scenario: Title confirmation pauses workflow
- **WHEN** the title generation node completes
- **THEN** the system stores title options, creates a title confirmation human task, marks the workflow as `WAITING_USER`, and publishes a waiting-user event

#### Scenario: Outline confirmation pauses workflow
- **WHEN** the outline generation node completes
- **THEN** the system stores the outline draft, creates an outline confirmation human task, marks the workflow as `WAITING_USER`, and publishes a waiting-user event

### Requirement: Human task completion is validated server-side
The system SHALL validate human task ownership, node type, waiting status, and version before resuming a paused workflow.

#### Scenario: Valid human task completion resumes workflow
- **WHEN** the assigned user submits a valid confirmation result with the expected version
- **THEN** the system verifies the checkpoint exists, updates the graph checkpoint, completes the human task, stores the structured result, and resumes workflow execution

#### Scenario: Invalid human task completion is rejected
- **WHEN** a wrong user, stale version, completed task, or wrong node is submitted
- **THEN** the system rejects the submission and does not resume the workflow

#### Scenario: Expired human task completion is rejected before completion
- **WHEN** the assigned user submits a title or outline confirmation after the human task expiration time
- **THEN** the system marks the human task as expired, does not complete it, and does not resume the workflow

#### Scenario: Missing checkpoint is rejected before human task completion
- **WHEN** the assigned user submits a confirmation but the Redis checkpoint no longer exists
- **THEN** the system marks the workflow as expired and does not mark the human task as completed

### Requirement: Expired workflow returns to explicit regeneration
The system SHALL not automatically regenerate expired workflow content; it SHALL ask the user to explicitly restart the expired generation stage.

#### Scenario: Expired title confirmation returns regenerate prompt
- **WHEN** a user reconnects to an article workflow whose title confirmation checkpoint has expired
- **THEN** the system publishes an expired workflow event and the frontend can show a regenerate-title action

#### Scenario: Expired outline confirmation returns regenerate prompt
- **WHEN** a user reconnects to an article workflow whose outline confirmation checkpoint has expired
- **THEN** the system publishes an expired workflow event and the frontend can show a regenerate-outline action

#### Scenario: User explicitly regenerates after expiration
- **WHEN** the user chooses to regenerate the expired stage
- **THEN** the system creates a new valid checkpoint and human task for the regenerated stage

### Requirement: Article business state remains article-owned
The system SHALL keep article title options, selected title, outline, content, images, cover image, and final content in article-owned storage.

#### Scenario: Article node persists business output
- **WHEN** an article graph node produces a business result
- **THEN** the node action persists that result through article services and also returns structured state to the graph

### Requirement: Workflow events preserve existing article progress behavior
The system SHALL publish generic workflow events and keep existing article SSE compatibility during migration.

#### Scenario: Existing article frontend receives progress
- **WHEN** article workflow nodes start, complete, wait for user input, fail, or finish
- **THEN** the system publishes workflow events and maps article node results to existing article SSE message types

### Requirement: Future rednote workflows are independently defined
The system SHALL reserve the persisted creation type value `rednote` for future small-red-book style creation workflows.

#### Scenario: Rednote naming is reserved
- **WHEN** future rednote creation is implemented
- **THEN** the system uses `rednote` as the persisted value and `REDNOTE` as the Java enum constant if an enum is used

### Requirement: Article workflow exposes optional prompt feedback anchors
The system SHALL expose optional prompt feedback anchors at title selection, outline editing, and content merged stages without changing workflow resume semantics.

#### Scenario: Title selection exposes feedback anchor
- **WHEN** the title generation node completes and the article workflow enters title selection
- **THEN** the frontend can present an optional `TITLE_SELECTION` prompt feedback entry associated with the current workflow task

#### Scenario: Outline editing exposes feedback anchor
- **WHEN** the outline generation node completes and the article workflow enters outline editing
- **THEN** the frontend can present an optional `OUTLINE_EDITING` prompt feedback entry associated with the current workflow task

#### Scenario: Content merged exposes feedback anchor
- **WHEN** the content merge node completes and the article workflow reaches merged content output
- **THEN** the frontend can present an optional `CONTENT_MERGED` prompt feedback entry associated with the current workflow task

#### Scenario: Rednote completion exposes feedback anchor
- **WHEN** rednote full-auto generation is complete
- **THEN** the frontend can present one optional prompt feedback entry associated with `REDNOTE_CONTENT`, `REDNOTE_NORMAL_IMAGE_PROMPT`, and `REDNOTE_COVER_IMAGE_PROMPT`

#### Scenario: Feedback skip does not affect workflow state
- **WHEN** a user skips, closes, or ignores an optional prompt feedback entry
- **THEN** the article workflow state and human task completion rules remain unchanged

### Requirement: Rednote workflow reuses shared workflow infrastructure independently
The system SHALL allow `rednote` creation workflows to reuse shared workflow task, checkpoint, and event infrastructure while keeping business graph and business storage independent from article workflows.

#### Scenario: Rednote task uses shared workflow task table
- **WHEN** a rednote creation task starts
- **THEN** the system records the workflow in `workflow_task` with `biz_type = rednote`

#### Scenario: Rednote graph is independent from article graph
- **WHEN** the system executes a rednote creation workflow
- **THEN** it SHALL use a rednote-specific StateGraph instead of adding rednote branches to the article StateGraph

#### Scenario: Rednote business result is not stored only in workflow context
- **WHEN** rednote workflow nodes produce business output
- **THEN** the system SHALL persist rednote business output in rednote-owned storage and use workflow context only as execution state snapshot
