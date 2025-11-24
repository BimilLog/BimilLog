-- ============================================
-- V2.6: 친구 관련 테이블 추가
-- ============================================
-- 목적: 친구 요청, 친구 관계, 친구 추천 기능 구현
-- 작성일: 2025-11-22
--
-- 배경:
--   - 사용자 간 친구 요청 및 관리 기능 필요
--   - 친구 관계는 양방향 (1,10 존재 시 10,1 중복 방지는 비즈니스 로직에서 처리)
--   - 친구 추천은 단방향 (서로 추천 목록에 나타날 수 있음)
--   - 1촌, 2촌, 3촌 관계 추천 시스템
--
-- 주요 기능:
--   - 친구 요청 송수신 및 관리
--   - 친구 관계 설정 및 삭제
--   - 점수 기반 친구 추천 시스템
--   - Member 삭제 시 자동으로 관련 데이터 삭제 (CASCADE)
--   - UNIQUE 제약조건으로 중복 방지
-- ============================================

-- ============================================
-- Step 1: friend_request 테이블 생성 (친구 요청)
-- ============================================
CREATE TABLE IF NOT EXISTS `friend_request` (
    `friend_request_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '친구 요청 고유 ID',

    `sender_id` BIGINT NOT NULL
        COMMENT '친구 요청을 보낸 회원 ID',

    `receiver_id` BIGINT NOT NULL
        COMMENT '친구 요청을 받은 회원 ID',

    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '친구 요청 생성 일시',

    `modified_at` TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6)
        COMMENT '친구 요청 수정 일시',

    PRIMARY KEY (`friend_request_id`),

    -- UNIQUE 제약조건 (sender_id, receiver_id 조합 중복 방지)
    -- 양방향 중복(1->10, 10->1)은 비즈니스 로직에서 처리
    CONSTRAINT `unique_friend_request`
        UNIQUE (`sender_id`, `receiver_id`),

    -- 외래키 제약조건 (CASCADE 삭제)
    CONSTRAINT `fk_friend_request_sender`
        FOREIGN KEY (`sender_id`)
        REFERENCES `member` (`member_id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT `fk_friend_request_receiver`
        FOREIGN KEY (`receiver_id`)
        REFERENCES `member` (`member_id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    -- 인덱스 (조회 성능 최적화)
    INDEX `idx_friend_request_sender` (`sender_id`)
        COMMENT '보낸 친구 요청 조회 시 성능 최적화',

    INDEX `idx_friend_request_receiver` (`receiver_id`)
        COMMENT '받은 친구 요청 조회 시 성능 최적화'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='친구 요청 테이블 (양방향 중복은 비즈니스 로직에서 처리)';

-- ============================================
-- Step 2: friendship 테이블 생성 (친구 관계)
-- ============================================
CREATE TABLE IF NOT EXISTS `friendship` (
    `friendship_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '친구 관계 고유 ID',

    `member_id` BIGINT NOT NULL
        COMMENT '회원 ID',

    `friend_id` BIGINT NOT NULL
        COMMENT '친구 회원 ID',

    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '친구 관계 생성 일시',

    `modified_at` TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6)
        COMMENT '친구 관계 수정 일시',

    PRIMARY KEY (`friendship_id`),

    -- UNIQUE 제약조건 (member_id, friend_id 조합 중복 방지)
    -- 양방향 중복(1,10 과 10,1)은 비즈니스 로직에서 처리
    CONSTRAINT `unique_friend_pair`
        UNIQUE (`member_id`, `friend_id`),

    -- 외래키 제약조건 (CASCADE 삭제)
    CONSTRAINT `fk_friendship_member`
        FOREIGN KEY (`member_id`)
        REFERENCES `member` (`member_id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT `fk_friendship_friend`
        FOREIGN KEY (`friend_id`)
        REFERENCES `member` (`member_id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    -- 인덱스 (조회 성능 최적화)
    INDEX `idx_friendship_member` (`member_id`)
        COMMENT '회원의 친구 목록 조회 시 성능 최적화',

    INDEX `idx_friendship_friend` (`friend_id`)
        COMMENT '친구로 등록된 회원 조회 시 성능 최적화'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='친구 관계 테이블 (양방향 중복은 비즈니스 로직에서 처리)';

-- ============================================
-- Step 3: friend_recommendation 테이블은 Redis로 관리
-- ============================================
-- dev_friend_cache 브랜치에서는 추천친구를 Redis로 관리합니다.
-- DB 테이블은 생성하지 않습니다.
