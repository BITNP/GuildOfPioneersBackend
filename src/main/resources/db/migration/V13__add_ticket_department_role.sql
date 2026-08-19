ALTER TABLE registration_tickets ADD COLUMN department VARCHAR(255);
ALTER TABLE registration_tickets ADD COLUMN role VARCHAR(255);

UPDATE registration_tickets
SET department = 'CLINIC', role = 'MEMBER'
WHERE department IS NULL;

ALTER TABLE registration_tickets ALTER COLUMN department SET NOT NULL;
ALTER TABLE registration_tickets ALTER COLUMN role SET NOT NULL;

ALTER TABLE registration_tickets
    ADD CONSTRAINT chk_registration_tickets_department CHECK (department IN ('CLINIC', 'TECH', 'SUPPORT', 'MEDIA', 'PRESIDIUM'));
ALTER TABLE registration_tickets
    ADD CONSTRAINT chk_registration_tickets_role CHECK (role IN ('LEADER', 'VICE', 'ADVISOR', 'MEMBER'));
