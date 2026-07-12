## ADDED Requirements

### Requirement: UI excludes decorative emojis
The system SHALL render no decorative emoji characters in any user-facing UI component, template, or view.

#### Scenario: Emoji-free component render
- **WHEN** any UI component is rendered in the browser
- **THEN** the rendered DOM contains no emoji Unicode code points (range U+1F300–U+1FAFF and standalone emoji symbols)

#### Scenario: CI emoji check passes
- **WHEN** the lint or static check pipeline runs over `client/src`
- **THEN** it reports zero emoji matches and fails the build otherwise

### Requirement: Functional meaning uses text labels
The system SHALL replace any emoji that conveyed functional meaning with a clear text label or CSS-only shape.

#### Scenario: Icon intent preserved
- **WHEN** an emoji previously indicated an action (for example a confirmation or launch)
- **THEN** the UI shows an equivalent text label (for example "Confirm" or "Launch") instead

#### Scenario: Visual feedback without emoji
- **WHEN** the UI needs to signal status or state
- **THEN** it uses text, color, or CSS shapes rather than emoji glyphs

### Requirement: Accessibility preserved after removal
The system SHALL maintain or improve screen-reader and contrast behavior after emoji removal.

#### Scenario: Screen reader output
- **WHEN** a screen reader traverses a previously emoji-decorated control
- **THEN** it announces the text label and no empty or decorative emoji artifact

#### Scenario: Contrast compliance
- **WHEN** replacement text labels or CSS shapes are applied
- **THEN** they meet the existing contrast and sizing standards defined for the Makmur UI