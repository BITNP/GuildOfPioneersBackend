ALTER TABLE user_departments DROP CONSTRAINT chk_user_departments_department;

ALTER TABLE user_departments
    ADD CONSTRAINT chk_user_departments_department
        CHECK (department IN ('CLINIC', 'TECH', 'SUPPORT', 'MEDIA', 'PRESIDIUM', 'ADMIN'));
