-- ================================================================================================
-- Date: 2025-11-17
-- Version: 2.5 (H2 Test)
-- ================================================================================================
-- Description:
--  멤버 블랙리스트 테이블 추가
--  사용자 간 차단 기능 구현을 위한 테이블 생성
--  차단 관계는 단방향 (A가 B를 차단해도 B는 A를 차단하지 않음)
-- ================================================================================================

-- Step 1: member_blacklist 테이블 생성
CREATE TABLE IF NOT EXISTS member_blacklist (
    member_black_list_id BIGINT NOT NULL AUTO_INCREMENT,
    request_member_id BIGINT NOT NULL,
    black_member_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (member_black_list_id),

    -- Step 2: UNIQUE 제약조건 (중복 차단 방지)
    CONSTRAINT uk_member_blacklist_request_black
        UNIQUE (request_member_id, black_member_id),

    -- Step 3: 외래키 제약조건 (CASCADE 삭제)
    CONSTRAINT fk_member_blacklist_request_member
        FOREIGN KEY (request_member_id)
        REFERENCES member (member_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_member_blacklist_black_member
        FOREIGN KEY (black_member_id)
        REFERENCES member (member_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
);

-- Step 4: 인덱스 (조회 성능 최적화)
CREATE INDEX idx_member_blacklist_request ON member_blacklist (request_member_id);
CREATE INDEX idx_member_blacklist_black ON member_blacklist (black_member_id);
