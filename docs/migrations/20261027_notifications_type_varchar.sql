-- Run against the application's MySQL database before retrying Review Reply.
-- Changing ENUM to VARCHAR preserves existing type names and allows future Java enum values.
ALTER TABLE notifications
    MODIFY COLUMN type VARCHAR(50) NULL;
