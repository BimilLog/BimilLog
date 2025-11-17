-- ============================================
-- V2.5: 멤버 블랙리스트 테이블 추가
-- ============================================
-- 목적: 사용자 간 차단 기능 구현
-- 작성일: 2025-11-17
--
-- 배경:
--   - 사용자가 원치 않는 다른 사용자로부터의 상호작용을 차단할 수 있는 기능 필요
--   - 차단 관계는 단방향 (A가 B를 차단해도 B는 A를 차단하지 않음)
--   - 한 사용자가 동일한 사용자를 중복으로 차단할 수 없음
--
-- 주요 기능:
--   - 차단 요청자(request_member)와 차단 대상(black_member) 관리
--   - BaseEntity 패턴 준수 (created_at, modified_at)
--   - Member 삭제 시 자동으로 관련 블랙리스트 삭제 (CASCADE)
--   - UNIQUE 제약조건으로 중복 차단 방지
-- ============================================

-- Step 1: member_blacklist 테이블 생성
CREATE TABLE IF NOT EXISTS `member_blacklist` (
    `member_black_list_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '블랙리스트 고유 ID',

    `request_member_id` BIGINT NOT NULL
        COMMENT '블랙리스트를 요청한 회원 ID (차단하는 사람)',

    `black_member_id` BIGINT NOT NULL
        COMMENT '블랙리스트에 추가된 회원 ID (차단당한 사람)',

    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '블랙리스트 추가 일시',

    `modified_at` TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6)
        COMMENT '블랙리스트 수정 일시',

    PRIMARY KEY (`member_black_list_id`),

    -- Step 2: UNIQUE 제약조건 (중복 차단 방지)
    CONSTRAINT `uk_member_blacklist_request_black`
        UNIQUE (`request_member_id`, `black_member_id`),

    -- Step 3: 외래키 제약조건 (CASCADE 삭제)
    CONSTRAINT `fk_member_blacklist_request_member`
        FOREIGN KEY (`request_member_id`)
        REFERENCES `member` (`member_id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT `fk_member_blacklist_black_member`
        FOREIGN KEY (`black_member_id`)
        REFERENCES `member` (`member_id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    -- Step 4: 인덱스 (조회 성능 최적화)
    INDEX `idx_member_blacklist_request` (`request_member_id`)
        COMMENT '차단 요청자로 조회 시 성능 최적화',

    INDEX `idx_member_blacklist_black` (`black_member_id`)
        COMMENT '차단 대상자로 조회 시 성능 최적화'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='회원 간 차단 관계 테이블 (단방향)';
