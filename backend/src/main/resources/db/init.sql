-- 创建数据库
CREATE DATABASE gba_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gba_db;

CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(100),
    avatar_url  VARCHAR(500),
    role        VARCHAR(20) DEFAULT 'USER',
    profession  VARCHAR(20)  COMMENT '职业：神相/素问/潮光/鸿音/龙吟/沧澜/玄机/铁衣/碎梦/九灵/血河',
    deleted     TINYINT DEFAULT 0,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_panel (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    panel_data  JSON NOT NULL,
    source      VARCHAR(20) DEFAULT 'ocr',
    panel_name  VARCHAR(100)  COMMENT '面板名称，由用户自定义',
    deleted     TINYINT DEFAULT 0,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_user_panel_user_id ON user_panel(user_id);

CREATE TABLE equipment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    slot         VARCHAR(50),
    stat_bonuses JSON NOT NULL,
    description  TEXT,
    image_url    VARCHAR(500),
    deleted      TINYINT DEFAULT 0,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trait (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    calc_formula TEXT,
    deleted      TINYINT DEFAULT 0,
    profession   VARCHAR(20)   COMMENT '所属职业，NULL 表示通用特质',
    image_url    VARCHAR(500)  COMMENT '特质图标（阿里云 OSS URL）',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE recommendation_log (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    panel_id         BIGINT,
    recommend_type   VARCHAR(20) NOT NULL,
    target_id        BIGINT,
    target_name      VARCHAR(100),
    base_damage      DECIMAL(12,4),
    new_damage       DECIMAL(12,4),
    damage_delta_pct DECIMAL(8,4),
    base_tank        DECIMAL(12,4),
    new_tank         DECIMAL(12,4),
    tank_delta_pct   DECIMAL(8,4),
    total_score      DECIMAL(8,2),
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (panel_id) REFERENCES user_panel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_rec_log_user_id ON recommendation_log(user_id);

-- 初始管理账户（密码：admin123）
INSERT INTO users (username, password, nickname, role)
VALUES ('admin', '123', '管理员', 'ADMIN');