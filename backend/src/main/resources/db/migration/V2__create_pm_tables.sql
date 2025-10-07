-- Migration V2: Create PM OLTP Tables
-- PM Module: issues, sprints, boards, board_columns, issue_links
-- Note: Foreign key constraints deferred to V5 (after shared tables created in V3)

-- Table: issues (core PM work item)
CREATE TABLE issues (
    id UUID PRIMARY KEY,
    issue_key VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    issue_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    story_points INTEGER,
    reporter_id UUID NOT NULL,  -- FK to users (added in V5)
    assignee_id UUID,            -- FK to users (added in V5)
    sprint_id UUID,              -- FK to sprints (added in V5)
    parent_issue_id UUID,        -- FK to issues (added in V5)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_issues_type CHECK (issue_type IN ('STORY', 'TASK', 'BUG', 'EPIC', 'SUBTASK')),
    CONSTRAINT chk_issues_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED')),
    CONSTRAINT chk_issues_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_issues_story_points CHECK (story_points >= 0 AND story_points <= 100)
);

CREATE INDEX idx_issues_status ON issues(status) WHERE status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW');
CREATE INDEX idx_issues_assignee ON issues(assignee_id) WHERE assignee_id IS NOT NULL;
CREATE INDEX idx_issues_sprint ON issues(sprint_id) WHERE sprint_id IS NOT NULL;
CREATE INDEX idx_issues_parent ON issues(parent_issue_id) WHERE parent_issue_id IS NOT NULL;
CREATE INDEX idx_issues_created_at ON issues(created_at DESC);

-- Table: sprints (time-boxed iteration)
CREATE TABLE sprints (
    id UUID PRIMARY KEY,
    sprint_name VARCHAR(200) NOT NULL,
    sprint_goal TEXT,
    board_id UUID NOT NULL,     -- FK to boards (added in V5)
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_sprints_status CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_sprints_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_sprints_board ON sprints(board_id);
CREATE INDEX idx_sprints_status ON sprints(status) WHERE status IN ('PLANNED', 'ACTIVE');
CREATE INDEX idx_sprints_dates ON sprints(start_date, end_date);

-- Table: boards (kanban/scrum board)
CREATE TABLE boards (
    id UUID PRIMARY KEY,
    board_name VARCHAR(200) NOT NULL,
    board_type VARCHAR(50) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,     -- FK to users (added in V5)
    team_id UUID,               -- FK to teams (added in V5)
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_boards_type CHECK (board_type IN ('SCRUM', 'KANBAN'))
);

CREATE INDEX idx_boards_owner ON boards(owner_id);
CREATE INDEX idx_boards_team ON boards(team_id) WHERE team_id IS NOT NULL;
CREATE INDEX idx_boards_active ON boards(is_active) WHERE is_active = true;

-- Table: board_columns (workflow states on a board)
CREATE TABLE board_columns (
    id UUID PRIMARY KEY,
    board_id UUID NOT NULL,     -- FK to boards (added in V5)
    column_name VARCHAR(100) NOT NULL,
    column_order INTEGER NOT NULL,
    wip_limit INTEGER,
    is_done_column BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_board_columns_order CHECK (column_order >= 0),
    CONSTRAINT chk_board_columns_wip CHECK (wip_limit IS NULL OR wip_limit > 0)
);

CREATE UNIQUE INDEX idx_board_columns_unique ON board_columns(board_id, column_order);
CREATE INDEX idx_board_columns_board ON board_columns(board_id, column_order);

-- Table: issue_links (relationships between issues)
CREATE TABLE issue_links (
    id UUID PRIMARY KEY,
    source_issue_id UUID NOT NULL,  -- FK to issues (added in V5)
    target_issue_id UUID NOT NULL,  -- FK to issues (added in V5)
    link_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,       -- FK to users (added in V5)
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_issue_links_type CHECK (link_type IN ('BLOCKS', 'BLOCKED_BY', 'RELATES_TO', 'DUPLICATES', 'CLONES')),
    CONSTRAINT chk_issue_links_self CHECK (source_issue_id != target_issue_id)
);

CREATE UNIQUE INDEX idx_issue_links_unique ON issue_links(source_issue_id, target_issue_id, link_type);
CREATE INDEX idx_issue_links_source ON issue_links(source_issue_id);
CREATE INDEX idx_issue_links_target ON issue_links(target_issue_id);
