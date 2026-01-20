-- Friend Event DLQ (Dead Letter Queue) 테이블
-- 친구 관련 Redis 이벤트 처리 실패 시 임시 저장하여 재처리하기 위한 테이블

CREATE TABLE `friend_event_dlq` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `type` ENUM('FRIEND_ADD', 'FRIEND_REMOVE', 'SCORE_UP') NOT NULL COMMENT '이벤트 타입',
    `member_id` BIGINT NOT NULL COMMENT '대상자 ID',
    `target_id` BIGINT NOT NULL COMMENT '친구/상호작용 대상 ID',
    `score` DOUBLE NULL COMMENT '점수 증가분 (SCORE_UP일 때만 사용)',
    `status` ENUM('PENDING', 'PROCESSED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일',
    INDEX `idx_dlq_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='친구 이벤트 DLQ';
