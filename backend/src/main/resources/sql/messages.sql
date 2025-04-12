CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    dialog_id TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, dialog_id)
);