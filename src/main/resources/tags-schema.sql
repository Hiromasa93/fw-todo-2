CREATE TABLE IF NOT EXISTS tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tags_name (name)
);

CREATE TABLE IF NOT EXISTS todo_tags (
    todo_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (todo_id, tag_id),
    KEY idx_todo_tags_tag_id (tag_id),
    CONSTRAINT fk_todo_tags_todo FOREIGN KEY (todo_id) REFERENCES todos(id) ON DELETE CASCADE,
    CONSTRAINT fk_todo_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS player_profile (
    id INT NOT NULL PRIMARY KEY,
    total_points INT NOT NULL DEFAULT 0
);
INSERT IGNORE INTO player_profile (id, total_points) VALUES (1, 0);

CREATE TABLE IF NOT EXISTS point_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    points INT NOT NULL,
    action VARCHAR(512) NOT NULL,
    todo_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_point_history_created_at (created_at)
);
