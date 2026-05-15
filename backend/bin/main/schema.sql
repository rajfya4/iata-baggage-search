DROP TABLE IF EXISTS audit_log;

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(100)  NOT NULL,
    query_text  CLOB          NOT NULL,
    response_text CLOB        NOT NULL,
    citations   VARCHAR(2000),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
