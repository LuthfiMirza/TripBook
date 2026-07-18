CREATE TABLE failed_events (
    id BIGSERIAL PRIMARY KEY,
    payload TEXT NOT NULL,
    error_message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
