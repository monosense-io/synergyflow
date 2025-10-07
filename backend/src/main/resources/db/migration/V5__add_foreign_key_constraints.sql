-- Migration V5: Add Foreign Key Constraints
-- Establishes referential integrity after all tables created in V1-V4
-- Ensures data consistency and enables cascade operations

-- ITSM Module Foreign Keys (from V1 tables)

-- tickets table
ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    ADD CONSTRAINT fk_tickets_assignee FOREIGN KEY (assignee_id) REFERENCES users(id);

-- incidents table
ALTER TABLE incidents
    ADD CONSTRAINT fk_incidents_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

-- service_requests table
ALTER TABLE service_requests
    ADD CONSTRAINT fk_service_requests_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_service_requests_approved_by FOREIGN KEY (approved_by) REFERENCES users(id);

-- ticket_comments table
ALTER TABLE ticket_comments
    ADD CONSTRAINT fk_ticket_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ticket_comments_author FOREIGN KEY (author_id) REFERENCES users(id);

-- routing_rules table
ALTER TABLE routing_rules
    ADD CONSTRAINT fk_routing_rules_team FOREIGN KEY (target_team_id) REFERENCES teams(id),
    ADD CONSTRAINT fk_routing_rules_user FOREIGN KEY (target_user_id) REFERENCES users(id);

-- PM Module Foreign Keys (from V2 tables)

-- issues table
ALTER TABLE issues
    ADD CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    ADD CONSTRAINT fk_issues_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
    ADD CONSTRAINT fk_issues_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id),
    ADD CONSTRAINT fk_issues_parent FOREIGN KEY (parent_issue_id) REFERENCES issues(id);

-- sprints table
ALTER TABLE sprints
    ADD CONSTRAINT fk_sprints_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE;

-- boards table
ALTER TABLE boards
    ADD CONSTRAINT fk_boards_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    ADD CONSTRAINT fk_boards_team FOREIGN KEY (team_id) REFERENCES teams(id);

-- board_columns table
ALTER TABLE board_columns
    ADD CONSTRAINT fk_board_columns_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE;

-- issue_links table
ALTER TABLE issue_links
    ADD CONSTRAINT fk_issue_links_source FOREIGN KEY (source_issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_issue_links_target FOREIGN KEY (target_issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_issue_links_created_by FOREIGN KEY (created_by) REFERENCES users(id);

-- Read Models Foreign Keys (from V4 tables)

-- ticket_card table
ALTER TABLE ticket_card
    ADD CONSTRAINT fk_ticket_card_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

-- sla_tracking table
ALTER TABLE sla_tracking
    ADD CONSTRAINT fk_sla_tracking_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

-- related_incidents table
ALTER TABLE related_incidents
    ADD CONSTRAINT fk_related_incidents_incident FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_related_incidents_parent FOREIGN KEY (parent_incident_id) REFERENCES incidents(id);

-- queue_row table
ALTER TABLE queue_row
    ADD CONSTRAINT fk_queue_row_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

-- issue_card table
ALTER TABLE issue_card
    ADD CONSTRAINT fk_issue_card_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE;

-- sprint_summary table
ALTER TABLE sprint_summary
    ADD CONSTRAINT fk_sprint_summary_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE;
