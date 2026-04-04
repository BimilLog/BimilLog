-- ================================================================================================
-- Social Token Refactor: Move member FK from member.social_token_id to social_token.member_id - H2 Version
-- Date: 2026-04-04
-- Version: 2.25
-- ================================================================================================

-- 1. Add member_id to social_token (table is empty on fresh start in tests)
ALTER TABLE social_token ADD COLUMN member_id BIGINT NOT NULL;

-- 2. Add unique constraint and FK
ALTER TABLE social_token ADD CONSTRAINT uk_social_token_member UNIQUE (member_id);
ALTER TABLE social_token ADD CONSTRAINT fk_social_token_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE;

-- 3. Drop social_token_id from member
ALTER TABLE member DROP COLUMN social_token_id;
