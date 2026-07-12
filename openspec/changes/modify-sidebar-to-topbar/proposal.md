## Why
The current sidebar navigation occupies significant horizontal space (220px), reducing the available screen real estate for core product views. Moving the navigation to a topbar increases the effective workspace and aligns with modern responsive web patterns.

## What Changes
- **BREAKING**: Replace the fixed left-side `Sidebar` component with a horizontal `Topbar` component.
- Update `App.tsx` layout logic to remove the left margin (`app-main-area`) and integrate the topbar.
- Modify the navigation menu to render horizontally.
- Update CSS to handle topbar positioning, responsiveness, and layout shifting.
- Implement a mobile-friendly dropdown or hamburger menu for the topbar on small screens.

## Capabilities
### New Capabilities
- `top-navigation`: Provide a horizontal navigation bar with role-based access control.

### Modified Capabilities
- `ui-layout`: Change the primary layout from sidebar-driven to topbar-driven.

## Impact
- **Frontend Components**: `Sidebar.tsx` (replaced by `Topbar.tsx`), `App.tsx` (layout change).
- **Styling**: `index.css` (layout classes, responsive breakpoints).
- **UX**: Navigation flow changes from vertical to horizontal.