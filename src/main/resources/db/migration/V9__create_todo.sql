CREATE TABLE todo_projects (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date     TIMESTAMP WITH TIME ZONE
);

CREATE TABLE todo_project_leaders (
    project_id BIGINT NOT NULL REFERENCES todo_projects (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE todo_project_members (
    project_id BIGINT NOT NULL REFERENCES todo_projects (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE todo_tasks (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL REFERENCES todo_projects (id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date     TIMESTAMP WITH TIME ZONE
);

CREATE TABLE todo_task_leaders (
    task_id BIGINT NOT NULL REFERENCES todo_tasks (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE todo_task_members (
    task_id BIGINT NOT NULL REFERENCES todo_tasks (id) ON DELETE CASCADE,
    user_id  BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE todo_actions (
    id           BIGSERIAL PRIMARY KEY,
    task_id      BIGINT NOT NULL REFERENCES todo_tasks (id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date     TIMESTAMP WITH TIME ZONE
);

CREATE TABLE todo_action_members (
    action_id BIGINT NOT NULL REFERENCES todo_actions (id) ON DELETE CASCADE,
    user_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (action_id, user_id)
);
