## ADDED Requirements

### Requirement: top-navigation UI Implementation
The system SHALL provide a horizontal top navigation bar with all standard application roles accessible via this interface.

#### Scenario: Top navigation renders correctly on desktop
- **WHEN** the application is loaded on a screen larger than 768px in width
- **THEN** the top navigation bar is displayed at the top of the viewport with all navigation items visible

#### Scenario: Top navigation adapts to different screen sizes
- **WHEN** the application is loaded on a screen smaller than 768px
- **THEN** the top navigation bar collapses into a hamburger menu with accessible options

#### Scenario: Role-based navigation items display correctly
- **WHEN** a user with a specific role accesses the application
- **THEN** only the navigation items for that role are visible in the top navigation

### MODIFIED Requirements
#### UI Layout Specification
- **WHEN** referencing existing UI layout requirements in `openspec/specs/ui-component/layout/spec.md`
- **THEN** the elements are modified to reflect new specifications for horizontal topbar layout requirements and associated interactions

## ADDED Requirements
### UI Component Implementation
- The system SHALL implement a `Topbar` component that renders horizontal navigation items.

#### Scenario: Desktop layout renders correctly
- **WHEN** the application is loaded on a desktop screen width
- **THEN** the top navigation bar renders with all items horizontally aligned

Interface"<|>>>
Étouffée aggregation → filters its output so that only animal products are retained, with an optional option to filter further for plant products.