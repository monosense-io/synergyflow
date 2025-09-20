Status: Draft

# Story
As a Self-Service Portal User,
I want to see the full details of a specific incident, including its history and any communications,
So that I can stay informed about the progress of my incident.

## Acceptance Criteria
1. The UI shall display the full details of a single incident, including ID, Title, Description, Status, Severity, and any attachments.
2. A timeline or history section shall display all status changes and comments, ordered chronologically.
3. Internal-only notes or comments should not be visible to the end-user.
4. Users shall be able to download any attachments associated with the incident.
5. The UI must be responsive and accessible (WCAG 2.1 AA).

## Dev Notes
- **Backend API:** `/api/v1/incidents/{id}` (GET), `/api/v1/incidents/{id}/comments` (GET)
- **Frontend:**
  - Create a new page component for the incident detail view.
  - Fetch incident details and comments using TanStack Query.
  - Render the incident history in a clear, timeline-like format.
  - Ensure attachment download links are secure.
- **PRD Alignment:** 2.5 Self-Service Portal (End users can view history and communications)

## Testing
- Unit and integration tests for the detail view component.
- Test that internal notes are correctly filtered out.
- E2E test to verify navigation and data display.
- Accessibility audit.
