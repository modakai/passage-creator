## ADDED Requirements

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

#### Scenario: Feedback skip does not affect workflow state
- **WHEN** a user skips, closes, or ignores an optional prompt feedback entry
- **THEN** the article workflow state and human task completion rules remain unchanged
