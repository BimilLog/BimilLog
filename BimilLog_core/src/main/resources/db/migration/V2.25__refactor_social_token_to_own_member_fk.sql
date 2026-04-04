-- SocialToken이 member_id FK를 보유하도록 관계 방향 변경
-- 기존: member.social_token_id → social_token
-- 변경: social_token.member_id → member (NOT NULL, FK 제약 포함)

-- 1. social_token 테이블에 member_id 컬럼 추가 (백필을 위해 우선 NULL 허용)
ALTER TABLE social_token ADD COLUMN member_id BIGINT NULL;

-- 2. 기존 데이터 백필: member.social_token_id를 이용해 역방향으로 채움
UPDATE social_token st
    INNER JOIN member m ON m.social_token_id = st.social_token_id
    SET st.member_id = m.member_id;

-- 2.5. 어떤 member도 참조하지 않는 고아 소셜 토큰 삭제 (NOT NULL 적용 전 필수)
DELETE FROM social_token WHERE member_id IS NULL;

-- 3. NOT NULL 제약 적용
ALTER TABLE social_token MODIFY COLUMN member_id BIGINT NOT NULL;

-- 4. UNIQUE 제약 (1:1 관계)
ALTER TABLE social_token ADD CONSTRAINT uk_social_token_member UNIQUE (member_id);

-- 5. FK 제약 추가 (Member 삭제 시 SocialToken 자동 삭제)
ALTER TABLE social_token
    ADD CONSTRAINT fk_social_token_member
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE;

-- 6. member 테이블에서 social_token_id 컬럼 삭제
ALTER TABLE member DROP COLUMN social_token_id;
