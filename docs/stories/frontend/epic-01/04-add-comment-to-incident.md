Status: Draft

# Story
As a Self-Service Portal User,
I want to be able to add comments to my existing incidents,
So that I can provide additional information or ask for updates.

## Acceptance Criteria
1. On the Incident Detail View, there shall be a form to add a new comment.
2. The comment form shall include a text area and a submit button.
3. After submitting a comment, the incident's history/timeline shall be updated in near real-time to show the new comment.
4. The UI for adding a comment must be responsive and accessible (WCAG 2.1 AA).

## Dev Notes
- **Backend API:** `/api/v1/incidents/{id}/comments` (POST)
- **Frontend:**
  - Add a comment form component to the Incident Detail page.
  - On submission, optimistically update the UI and then re-fetch the comments to ensure consistency.
  - Handle submission errors gracefully.
- **PRD Alignment:** 2.5 Self-Service Portal (End users can view history and communications)

## Testing
- Unit and integration tests for the comment form.
- E2E test to verify that a submitted comment appears in the history.
- Test optimistic UI updates and error handling.
- Accessibility audit.
