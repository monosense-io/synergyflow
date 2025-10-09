-- Migration V8: Add reopen_count column to tickets table (Story 2.2, Task 4)
-- Changes:
--   1. Add reopen_count INTEGER column to track number of times ticket has been reopened
--   2. Default value is 0 for existing and new tickets

-- Add reopen_count column (nullable initially)
ALTER TABLE tickets ADD COLUMN reopen_count INTEGER;

-- Set default value for existing rows
UPDATE tickets SET reopen_count = 0 WHERE reopen_count IS NULL;

-- Make column NOT NULL with default value
ALTER TABLE tickets ALTER COLUMN reopen_count SET NOT NULL;
ALTER TABLE tickets ALTER COLUMN reopen_count SET DEFAULT 0;
