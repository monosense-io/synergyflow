Status: Draft

# Story
As a Self-Service Portal User,
I want to see a list of all the incidents I have submitted,
So that I can track their status and quickly find incidents I need to review.

## Acceptance Criteria
1. The UI shall display a paginated list of incidents submitted by the logged-in user.
2. Each item in the list shall display the Incident ID, Title, Status, and Last Updated Date.
3. The list shall be searchable by Incident ID and Title.
4. The list shall be filterable by Status.
5. Clicking on an incident in the list shall navigate the user to the Incident Detail View.
6. The UI must be responsive and accessible (WCAG 2.1 AA).

## Dev Notes
- **Backend API:** `/api/v1/incidents` (GET)
- **Frontend:**
  - Create a new page component for the incident list.
  - Use TanStack Query for data fetching and caching.
  - Implement search and filter controls.
  - Ensure the list items link to the correct detail page.
- **PRD Alignment:** 2.5 Self-Service Portal (End users can log and track tickets)

## Testing
- Unit and integration tests for the list component, search, and filtering.
- E2E test to verify navigation from the list to the detail page.
- Accessibility audit.
