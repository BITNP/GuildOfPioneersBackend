ALTER TABLE todo_projects ADD COLUMN updated_date TIMESTAMP WITH TIME ZONE;
UPDATE todo_projects SET updated_date = created_date;
ALTER TABLE todo_projects ALTER COLUMN updated_date SET NOT NULL;

ALTER TABLE todo_tasks ADD COLUMN updated_date TIMESTAMP WITH TIME ZONE;
UPDATE todo_tasks SET updated_date = created_date;
ALTER TABLE todo_tasks ALTER COLUMN updated_date SET NOT NULL;

ALTER TABLE todo_actions ADD COLUMN updated_date TIMESTAMP WITH TIME ZONE;
UPDATE todo_actions SET updated_date = created_date;
ALTER TABLE todo_actions ALTER COLUMN updated_date SET NOT NULL;
