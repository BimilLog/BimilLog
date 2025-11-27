-- ================================================================================================
-- Date: 2025-11-22
-- Version: 2.6 (H2 Test)
-- ================================================================================================
-- Description:
--  친구 관련 테이블 추가
--  - friend_request: 친구 요청 테이블
--  - friendship: 친구 관계 테이블
--  - friend_recommendation: 친구 추천 테이블
--
--  친구 요청과 친구 관계는 양방향 중복 방지 (비즈니스 로직에서 처리)
--  친구 추천은 단방향 (서로 추천 목록에 나타날 수 있음)
-- ================================================================================================

-- ================================================================================================
-- Step 1: friend_request 테이블 생성 (친구 요청)
-- ================================================================================================
CREATE TABLE IF NOT EXISTS friend_request (
    friend_request_id BIGINT NOT NULL AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (friend_request_id),

    -- UNIQUE 제약조건 (sender_id, receiver_id 조합 중복 방지)
    CONSTRAINT unique_friend_request
        UNIQUE (sender_id, receiver_id),

    -- 외래키 제약조건 (CASCADE 삭제)
    CONSTRAINT fk_friend_request_sender
        FOREIGN KEY (sender_id)
        REFERENCES member (member_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_friend_request_receiver
        FOREIGN KEY (receiver_id)
        REFERENCES member (member_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
);

-- 인덱스 (조회 성능 최적화)
CREATE INDEX idx_friend_request_sender ON friend_request (sender_id);
CREATE INDEX idx_friend_request_receiver ON friend_request (receiver_id);

-- ================================================================================================
-- Step 2: friendship 테이블 생성 (친구 관계)
-- ================================================================================================
CREATE TABLE IF NOT EXISTS friendship (
    friendship_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (friendship_id),

    -- UNIQUE 제약조건 (member_id, friend_id 조합 중복 방지)
    CONSTRAINT unique_friend_pair
        UNIQUE (member_id, friend_id),

    -- 외래키 제약조건 (CASCADE 삭제)
    CONSTRAINT fk_friendship_member
        FOREIGN KEY (member_id)
        REFERENCES member (member_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_friendship_friend
        FOREIGN KEY (friend_id)
        REFERENCES member (member_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
);

-- 인덱스 (조회 성능 최적화)
CREATE INDEX idx_friendship_member ON friendship (member_id);
CREATE INDEX idx_friendship_friend ON friendship (friend_id);

-- ================================================================================================
-- Step 3: friend_recommendation 테이블은 Redis로 관리
-- ================================================================================================
-- dev_friend_cache 브랜치에서는 추천친구를 Redis로 관리합니다.
-- DB 테이블은 생성하지 않습니다.
