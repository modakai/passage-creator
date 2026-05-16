## ADDED Requirements

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
