-- Time Entry Schema for Story 00.2: Single-Entry Time Tray
-- Supports optimistic UI updates with mirroring to incident and task worklogs

-- Time entries table for storing user time logs
CREATE TABLE IF NOT EXISTS time_entries (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    duration_minutes BIGINT NOT NULL CHECK (duration_minutes > 0),
    description TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'CONFIRMED', 'MIRRORING', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_time_entries_user_id ON time_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_time_entries_status ON time_entries(status);
CREATE INDEX IF NOT EXISTS idx_time_entries_occurred_at ON time_entries(occurred_at);
CREATE INDEX IF NOT EXISTS idx_time_entries_created_at ON time_entries(created_at);
CREATE INDEX IF NOT EXISTS idx_time_entries_user_created ON time_entries(user_id, created_at DESC);

-- Time entry audit trail for immutable tracking
CREATE TABLE IF NOT EXISTS time_entry_audit (
    id BIGSERIAL PRIMARY KEY,
    time_entry_id VARCHAR(36) NOT NULL REFERENCES time_entries(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATED', 'CONFIRMED', 'MIRRORED', 'COMPLETED', 'FAILED')),
    target_entity_type VARCHAR(20),
    target_entity_id VARCHAR(255),
    mirrored_by VARCHAR(255),
    mirror_status VARCHAR(20),
    error_message TEXT,
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for audit trail queries
CREATE INDEX IF NOT EXISTS idx_time_entry_audit_time_entry_id ON time_entry_audit(time_entry_id);
CREATE INDEX IF NOT EXISTS idx_time_entry_audit_created_at ON time_entry_audit(created_at);
CREATE INDEX IF NOT EXISTS idx_time_entry_audit_correlation_id ON time_entry_audit(correlation_id);

-- Update trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_time_entries_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER IF NOT EXISTS trigger_time_entries_updated_at
    BEFORE UPDATE ON time_entries
    FOR EACH ROW
    EXECUTE FUNCTION update_time_entries_updated_at();

-- Incident worklogs table for mirroring time entries to incidents
CREATE TABLE IF NOT EXISTS incident_worklogs (
    id VARCHAR(36) PRIMARY KEY,
    incident_id VARCHAR(255) NOT NULL,
    time_entry_id VARCHAR(36) NOT NULL REFERENCES time_entries(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    duration_minutes BIGINT NOT NULL CHECK (duration_minutes > 0),
    description TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),
    UNIQUE(time_entry_id, incident_id)
);

-- Task worklogs table for mirroring time entries to tasks
CREATE TABLE IF NOT EXISTS task_worklogs (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    time_entry_id VARCHAR(36) NOT NULL REFERENCES time_entries(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    duration_minutes BIGINT NOT NULL CHECK (duration_minutes > 0),
    description TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),
    UNIQUE(time_entry_id, task_id)
);

-- Indexes for worklog performance
CREATE INDEX IF NOT EXISTS idx_incident_worklogs_incident_id ON incident_worklogs(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_worklogs_time_entry_id ON incident_worklogs(time_entry_id);
CREATE INDEX IF NOT EXISTS idx_incident_worklogs_user_id ON incident_worklogs(user_id);
CREATE INDEX IF NOT EXISTS idx_incident_worklogs_occurred_at ON incident_worklogs(occurred_at);

CREATE INDEX IF NOT EXISTS idx_task_worklogs_task_id ON task_worklogs(task_id);
CREATE INDEX IF NOT EXISTS idx_task_worklogs_time_entry_id ON task_worklogs(time_entry_id);
CREATE INDEX IF NOT EXISTS idx_task_worklogs_user_id ON task_worklogs(user_id);
CREATE INDEX IF NOT EXISTS idx_task_worklogs_occurred_at ON task_worklogs(occurred_at);

-- Add total_time_minutes column to incidents table if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'incidents' AND column_name = 'total_time_minutes') THEN
        ALTER TABLE incidents ADD COLUMN total_time_minutes BIGINT DEFAULT 0;
    END IF;
END $$;

-- Add total_time_minutes column to tasks table if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'tasks' AND column_name = 'total_time_minutes') THEN
        ALTER TABLE tasks ADD COLUMN total_time_minutes BIGINT DEFAULT 0;
    END IF;
END $$;

-- Update triggers for worklog timestamps
CREATE OR REPLACE FUNCTION update_worklog_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER IF NOT EXISTS trigger_incident_worklogs_updated_at
    BEFORE UPDATE ON incident_worklogs
    FOR EACH ROW
    EXECUTE FUNCTION update_worklog_updated_at();

CREATE TRIGGER IF NOT EXISTS trigger_task_worklogs_updated_at
    BEFORE UPDATE ON task_worklogs
    FOR EACH ROW
    EXECUTE FUNCTION update_worklog_updated_at();

-- Function to record audit trail changes
CREATE OR REPLACE FUNCTION record_time_entry_audit()
RETURNS TRIGGER AS $$
BEGIN
    -- Record creation audit
    IF TG_OP = 'INSERT' THEN
        INSERT INTO time_entry_audit (
            time_entry_id, action, correlation_id, causation_id, created_at
        ) VALUES (
            NEW.id, 'CREATED', NULL, NULL, NOW()
        );
        RETURN NEW;
    END IF;

    -- Record status change audit
    IF TG_OP = 'UPDATE' AND OLD.status != NEW.status THEN
        INSERT INTO time_entry_audit (
            time_entry_id, action, created_at
        ) VALUES (
            NEW.id, NEW.status, NOW()
        );
        RETURN NEW;
    END IF;

    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER IF NOT EXISTS trigger_time_entry_audit
    AFTER INSERT OR UPDATE ON time_entries
    FOR EACH ROW
    EXECUTE FUNCTION record_time_entry_audit();
