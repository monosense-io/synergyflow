Status: Draft

# Story
As a Self-Service Portal User,
I want a simple, guided form to submit a new incident with all the necessary information,
So that I can report issues quickly and accurately.

## Acceptance Criteria
1. The UI shall present a form to create a new incident, with fields for Title, Description, and Severity.
2. The form shall support adding one or more attachments, with validation for file size and type.
3. Upon successful submission, the user shall be redirected to the new incident's detail page and a confirmation message shall be displayed.
4. Form validation errors from the backend shall be displayed to the user next to the relevant fields.
5. The UI must be responsive and accessible (WCAG 2.1 AA).

## Dev Notes
- **Backend API:** `/api/v1/incidents` (POST)
- **Frontend:**
  - Create a new page and component for the incident creation form.
  - Use a form library like React Hook Form with Zod for client-side validation.
  - Handle file uploads and display progress.
  - Redirect to the new incident's detail page on success.
- **PRD Alignment:** 2.5 Self-Service Portal (Guided ticket submission)

## Testing
- Unit and integration tests for the form component and validation.
- E2E test for the complete form submission flow, including attachment upload and redirection.
- Test handling of backend validation errors.
- Accessibility audit.
