-- featured_post 테이블: 주간 레전드, 레전드, 공지사항 통합 관리
CREATE TABLE IF NOT EXISTS `featured_post` (
    `featured_post_id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL,  -- WEEKLY, LEGEND, NOTICE
    `featured_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `expired_at` TIMESTAMP(6) NULL,  -- WEEKLY: 7일 후, 나머지: NULL
    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `modified_at` TIMESTAMP(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (`featured_post_id`),
    UNIQUE KEY `uk_featured_post_type` (`post_id`, `type`),
    FOREIGN KEY (`post_id`) REFERENCES `post`(`post_id`) ON DELETE CASCADE,
    INDEX `idx_featured_post_type_featured` (`type`, `featured_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기존 공지사항 마이그레이션
INSERT INTO `featured_post` (`post_id`, `type`, `featured_at`)
SELECT `post_id`, 'NOTICE', `created_at`
FROM `post` WHERE `is_notice` = TRUE;
