## Why
The UI currently includes decorative emojis that violate the project's visual consistency and accessibility standards. Removing them aligns with the clean text-only interface requirement and improves readability.

## What Changes
- Remove all emoji characters from frontend templates, component markup, and UI-related text.
- Update themed CSS classes to no longer reference emoji placeholders.
- Refactor any hardcoded emoji references to use text labels instead.

## Capabilities
### New Capabilities
- `ui-clean`: Deliver a text-only UI that excludes all decorative emojis and icons.

### Modified Capabilities
- `ui-themes` (if any exist): Adjust theming specifications to omit emoji-based styles.

## Impact
- Affected front-end files: `client/src/components/*` where emojis appear in JSX or HTML templates.
- No breaking changes to functionality; only visual presentation changes.
- Design system documentation must be updated to reflect the new text-only style guide.