-- H2 전용 테스트 스키마 (Flyway V1.0)
-- 운영 Flyway 스키마에서 풀텍스트 인덱스 및 MySQL 전용 옵션을 제거한 버전

CREATE TABLE IF NOT EXISTS setting (
    setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_notification BOOLEAN NOT NULL DEFAULT TRUE,
    comment_notification BOOLEAN NOT NULL DEFAULT TRUE,
    post_featured_notification BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS kakao_token (
    kakao_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kakao_access_token VARCHAR(500),
    kakao_refresh_token VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_id BIGINT NOT NULL,
    kakao_token_id BIGINT,
    social_id VARCHAR(255) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    member_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    social_nickname VARCHAR(255),
    thumbnail_image VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6),
    CONSTRAINT fk_member_setting FOREIGN KEY (setting_id) REFERENCES setting(setting_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_member_name ON member(member_name);
CREATE UNIQUE INDEX uk_provider_social_id ON member(provider, social_id);
CREATE INDEX idx_member_membername ON member(member_name);
CREATE INDEX idx_member_kakao_token ON member(kakao_token_id);

CREATE TABLE IF NOT EXISTS auth_token (
    auth_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    refresh_token VARCHAR(500) NOT NULL,
    last_used_at TIMESTAMP,
    use_count INT DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6)
);

CREATE INDEX idx_auth_token_member ON auth_token(member_id);
CREATE INDEX idx_auth_token_refresh ON auth_token(refresh_token);

CREATE TABLE IF NOT EXISTS blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    social_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6)
);

CREATE UNIQUE INDEX uk_blacklist_provider_social ON blacklist(provider, social_id);

CREATE TABLE IF NOT EXISTS post (
    post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    title VARCHAR(30) NOT NULL,
    content CLOB NOT NULL,
    views INT NOT NULL,
    is_notice BOOLEAN NOT NULL,
    post_cache_flag VARCHAR(20),
    password INT,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6),
    CONSTRAINT fk_post_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
);

CREATE INDEX idx_post_notice_created ON post(is_notice, created_at);
CREATE INDEX idx_post_created_at_popular ON post(created_at, post_cache_flag);
CREATE INDEX idx_post_created ON post(created_at);
CREATE INDEX idx_post_popular_flag ON post(post_cache_flag);

CREATE TABLE IF NOT EXISTS comment (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT,
    member_id BIGINT,
    content VARCHAR(255) NOT NULL,
    deleted BOOLEAN NOT NULL,
    password INT,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_member FOREIGN KEY (member_id) REFERENCES member(member_id)
);

CREATE INDEX idx_comment_post_deleted ON comment(post_id, deleted);
CREATE INDEX idx_comment_post_created ON comment(post_id, created_at);

CREATE TABLE IF NOT EXISTS comment_closure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ancestor_id BIGINT,
    descendant_id BIGINT,
    depth INT NOT NULL,
    CONSTRAINT fk_comment_closure_ancestor FOREIGN KEY (ancestor_id) REFERENCES comment(comment_id),
    CONSTRAINT fk_comment_closure_descendant FOREIGN KEY (descendant_id) REFERENCES comment(comment_id)
);

CREATE INDEX idx_comment_closure_descendant_depth ON comment_closure(descendant_id, depth);

CREATE TABLE IF NOT EXISTS comment_like (
    comment_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    comment_id BIGINT,
    CONSTRAINT fk_comment_like_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES comment(comment_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_comment_like_member_comment ON comment_like(comment_id, member_id);
CREATE INDEX idx_comment_like_member_comment ON comment_like(comment_id, member_id);

CREATE TABLE IF NOT EXISTS post_like (
    post_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    post_id BIGINT,
    CONSTRAINT fk_post_like_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE
);

CREATE INDEX idx_postlike_member_post ON post_like(member_id, post_id);

CREATE TABLE IF NOT EXISTS message (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    deco_type VARCHAR(50) NOT NULL,
    anonymity VARCHAR(8) NOT NULL,
    content CLOB NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6),
    CONSTRAINT fk_message_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX unique_member_x_y ON message(member_id, x, y);

CREATE TABLE IF NOT EXISTS notification (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    notification_type VARCHAR(50) NOT NULL,
    content VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6),
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_member_created ON notification(member_id, created_at);

CREATE TABLE IF NOT EXISTS fcm_token (
    fcm_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    fcm_registration_token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6),
    CONSTRAINT fk_fcm_token_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
);

CREATE INDEX idx_fcm_token_member ON fcm_token(member_id);

CREATE TABLE IF NOT EXISTS report (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    report_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    content VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6)
);

CREATE INDEX idx_report_member ON report(member_id);
