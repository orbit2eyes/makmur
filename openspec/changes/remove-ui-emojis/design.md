## Context
The Makmur retail inventory webapp currently uses decorative emojis in its UI components, which violates the project goal of a clean, text-only interface. This breaks visual consistency and can impact accessibility for users relying on screen readers. The current codebase includes emoji references in React components, CSS styling, and potentially in design documentation, creating a technical debt in visual standards compliance.

## Goals / Non-Goals
**Goals:**
- Remove all emoji characters from the React frontend components and their associated styling
- Update CSS and theme definitions to eliminate emoji-based visual elements
- Replace emoji placeholders with appropriate text labels or alternative visual indicators
- Ensure color contrast and accessibility standards are maintained after removal

**Non-Goals:**
- Introduce new emojis or visual elements to replace the removed ones
- Modify core functionality or business logic
- Redesign the entire visual interface or theme system
- Add complex interactive animations or effects

## Decisions

### Decision: Systematic Regex-based Emoji Removal
**Problem:** Scattered emoji usage throughout the codebase
**Why:** Efficient bulk replacement across multiple file types
**Alternatives considered:**
- Manual per-component review (time-intensive)
- Individual file-by-file editing (error-prone)
- Automated but limited to React files only (incomplete)
**Implementation:** Use `git grep -E "[\U0001F300-\U0001F9FF]"` for detection followed by `sed` replacement across all affected files

### Decision: Text Label Substitutions
**Problem:** Visual elements that contributed meaning would disappear
**Why:** Maintain user understanding of UI functionality
**Implementation:** Replace smileys with descriptive text (e.g., `:face-smile:` → `Confirm`), icons with functional labels (e.g., `🚀 → Launch`)

### Decision: Gradual Rollout
**Problem:** Unexpected breakage to production functionality
**Why:** Ensure stability through incremental changes
**Implementation:** Remove emojis from lower-level components first, then progressively work up through the component hierarchy

### Decision: Centralized Theme Updates
**Problem:** Fragmented styling across multiple CSS files
**Why:** Maintain consistent theming system and avoid duplicated effort
**Implementation:** Update main theme config files to remove emoji-related values and ensure consistent application across all components

### Decision: Alternative Visual Indicators
**Problem:** Need visual feedback without emojis
**Why:** Preserve user experience quality while meeting text-only requirements
**Implementation:** Use CSS classes, icon fonts, or lightweight SVG alternatives for critical visual cues

## Risks / Trade-offs

[Rust.tsx Risk] → Test each component individually for unintended side effects
- Cost: Double testing effort
- Benefit: catches regression issues before release

[Migration Cost] → Phase-based rollout
- Risk: Extended timeline to complete changes
- Trade-off: Better error detection and recovery

[User Impact] → Sudden appearance changes
- Risk: Users momentarily confused by interface changes
- Mitigation: Communicate changes through release notes and UI highlights

## Migration Plan
1. **Phase 1:** Systematically identify all emoji occurrences in codebase
2. **Phase 2:** Create replacement text/function labels for each identified emoji
3. **Phase 3:** Update React components to remove emojis and apply substitutions
4. **Phase 4:** Remove emoji references from CSS theme files
5. **Phase 5:** Update component documentation to reflect changes
6. **Phase 6:** Comprehensive testing and code review before deployment
7. **Phase 7:** Gradual rollout to production with user feedback monitoring

## Open Questions
- Are there any emoji-style or icon components in development that should be replaced with native icon libraries?
- Should any functional behavior associated with emojis be preserved through alternative interaction mechanisms?
- Are there existing design documentation files that reference emojis and need updating?
- Which alternative visual indicators would be best suited for complex interactive elements?
- What fallback approach should be used if text labels cannot effectively replace emojis for certain UI elements?