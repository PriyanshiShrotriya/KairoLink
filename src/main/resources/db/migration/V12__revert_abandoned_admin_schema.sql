DROP TABLE issue_reports;

ALTER TABLE users
    DROP CONSTRAINT ck_users_status;

ALTER TABLE users
    DROP COLUMN status;