CREATE TABLE IF NOT EXISTS agent_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_type VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    trigger_source VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    input_text VARCHAR(1000),
    result_json TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    completed_at BIGINT
);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_created_at ON agent_tasks(created_at);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_type_status ON agent_tasks(task_type, status);

CREATE TABLE IF NOT EXISTS agent_notifications (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT,
    title VARCHAR(128) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    level VARCHAR(32) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_agent_notifications_created_at ON agent_notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_agent_notifications_read_delivered ON agent_notifications(is_read, is_delivered);
