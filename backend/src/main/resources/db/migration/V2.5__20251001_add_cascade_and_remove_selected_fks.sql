-- ================================================================================================
-- Add CASCADE Constraints and Remove Selected Foreign Keys
-- Date: 2025-10-01
-- Version: 2.5
-- ================================================================================================
-- Description:
--   데이터 무결성과 성능 개선을 위해:
--   1. CASCADE 추가: 부모 삭제 시 자식 자동 삭제 (comment_like, comment, post_like, member)
--   2. FK 제거: 독립적 라이프사이클 관리 (auth_token, report, member.kakao_token)
--
-- CASCADE 추가 이유:
--   - comment_like.comment_id: Comment 삭제 시 고아 레코드 방지 (버그 수정)
--   - comment_like.member_id: Member 삭제 시 CommentLike 자동 삭제 (무결성)
--   - comment.post_id: Post 삭제 시 댓글 자동 삭제 (성능)
--   - post_like.post_id: Post 삭제 시 좋아요 자동 삭제 (성능)
--   - post_like.member_id: Member 삭제 시 PostLike 자동 삭제 (무결성)
--   - member.setting_id: Member 삭제 시 Setting 자동 삭제 (생명주기 일치)
--
-- FK 제거 이유:
--   - auth_token.member_id: 로그인/로그아웃 빈번, 보안 감사용 이력 보존
--   - report.member_id: 법적/관리적 영구 보관, 악의적 사용자 추적
--   - member.kakao_token_id: 로그아웃 시 토큰 삭제, 재로그인 시 재생성
--
-- Impact:
--   - CASCADE: 애플리케이션 코드 단순화, 성능 향상, 고아 레코드 방지
--   - FK 제거: 컬럼/인덱스 유지, 참조 무결성은 애플리케이션 관리
-- ================================================================================================

DELIMITER $$

-- ============================================
-- 1. comment_like.comment_id → comment CASCADE 추가
-- ============================================
DROP PROCEDURE IF EXISTS add_cascade_comment_like_comment$$
CREATE PROCEDURE add_cascade_comment_like_comment()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    -- 현재 FK 이름과 CASCADE 여부 조회
    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment_like'
      AND REFERENCED_TABLE_NAME = 'comment'
    LIMIT 1;

    -- FK가 있고 CASCADE가 없으면 재생성
    IF fk_name IS NOT NULL AND has_cascade != 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `comment_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `comment_like`
          ADD CONSTRAINT `fk_comment_like_comment`
          FOREIGN KEY (`comment_id`) REFERENCES `comment` (`comment_id`)
          ON DELETE CASCADE;
    -- FK가 없으면 CASCADE로 생성
    ELSEIF fk_name IS NULL THEN
        ALTER TABLE `comment_like`
          ADD CONSTRAINT `fk_comment_like_comment`
          FOREIGN KEY (`comment_id`) REFERENCES `comment` (`comment_id`)
          ON DELETE CASCADE;
    END IF;
END$$

CALL add_cascade_comment_like_comment()$$
DROP PROCEDURE add_cascade_comment_like_comment$$

-- ============================================
-- 2. comment.post_id → post CASCADE 추가
-- ============================================
DROP PROCEDURE IF EXISTS add_cascade_comment_post$$
CREATE PROCEDURE add_cascade_comment_post()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment'
      AND REFERENCED_TABLE_NAME = 'post'
    LIMIT 1;

    IF fk_name IS NOT NULL AND has_cascade != 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `comment` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `comment`
          ADD CONSTRAINT `fk_comment_post`
          FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`)
          ON DELETE CASCADE;
    ELSEIF fk_name IS NULL THEN
        ALTER TABLE `comment`
          ADD CONSTRAINT `fk_comment_post`
          FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`)
          ON DELETE CASCADE;
    END IF;
END$$

CALL add_cascade_comment_post()$$
DROP PROCEDURE add_cascade_comment_post$$

-- ============================================
-- 3. post_like.post_id → post CASCADE 추가
-- ============================================
DROP PROCEDURE IF EXISTS add_cascade_post_like_post$$
CREATE PROCEDURE add_cascade_post_like_post()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post_like'
      AND REFERENCED_TABLE_NAME = 'post'
    LIMIT 1;

    IF fk_name IS NOT NULL AND has_cascade != 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `post_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `post_like`
          ADD CONSTRAINT `fk_post_like_post`
          FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`)
          ON DELETE CASCADE;
    ELSEIF fk_name IS NULL THEN
        ALTER TABLE `post_like`
          ADD CONSTRAINT `fk_post_like_post`
          FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`)
          ON DELETE CASCADE;
    END IF;
END$$

CALL add_cascade_post_like_post()$$
DROP PROCEDURE add_cascade_post_like_post$$

-- ============================================
-- 4. member.setting_id → setting CASCADE 추가
-- ============================================
DROP PROCEDURE IF EXISTS add_cascade_member_setting$$
CREATE PROCEDURE add_cascade_member_setting()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'member'
      AND REFERENCED_TABLE_NAME = 'setting'
    LIMIT 1;

    IF fk_name IS NOT NULL AND has_cascade != 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `member` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `member`
          ADD CONSTRAINT `fk_member_setting`
          FOREIGN KEY (`setting_id`) REFERENCES `setting` (`setting_id`)
          ON DELETE CASCADE;
    ELSEIF fk_name IS NULL THEN
        ALTER TABLE `member`
          ADD CONSTRAINT `fk_member_setting`
          FOREIGN KEY (`setting_id`) REFERENCES `setting` (`setting_id`)
          ON DELETE CASCADE;
    END IF;
END$$

CALL add_cascade_member_setting()$$
DROP PROCEDURE add_cascade_member_setting$$

-- ============================================
-- 5. auth_token.member_id FK 제거
-- ============================================
ALTER TABLE `auth_token`
DROP FOREIGN KEY IF EXISTS `fk_auth_token_member`;

-- ============================================
-- 5. post_like.member_id → member CASCADE 추가
-- ============================================
DROP PROCEDURE IF EXISTS add_cascade_post_like_member$$
CREATE PROCEDURE add_cascade_post_like_member()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post_like'
      AND REFERENCED_TABLE_NAME = 'member'
    LIMIT 1;

    IF fk_name IS NOT NULL AND has_cascade != 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `post_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `post_like`
          ADD CONSTRAINT `fk_post_like_member`
          FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
          ON DELETE CASCADE;
    ELSEIF fk_name IS NULL THEN
        ALTER TABLE `post_like`
          ADD CONSTRAINT `fk_post_like_member`
          FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
          ON DELETE CASCADE;
    END IF;
END$$

CALL add_cascade_post_like_member()$$
DROP PROCEDURE add_cascade_post_like_member$$

-- ============================================
-- 6. comment_like.member_id → member CASCADE 추가
-- ============================================
DROP PROCEDURE IF EXISTS add_cascade_comment_like_member$$
CREATE PROCEDURE add_cascade_comment_like_member()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment_like'
      AND REFERENCED_TABLE_NAME = 'member'
    LIMIT 1;

    IF fk_name IS NOT NULL AND has_cascade != 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `comment_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `comment_like`
          ADD CONSTRAINT `fk_comment_like_member`
          FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
          ON DELETE CASCADE;
    ELSEIF fk_name IS NULL THEN
        ALTER TABLE `comment_like`
          ADD CONSTRAINT `fk_comment_like_member`
          FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
          ON DELETE CASCADE;
    END IF;
END$$

CALL add_cascade_comment_like_member()$$
DROP PROCEDURE add_cascade_comment_like_member$$

-- ============================================
-- 7. report.member_id FK 제거
-- ============================================
ALTER TABLE `report`
DROP FOREIGN KEY IF EXISTS `fk_report_member`;

-- ============================================
-- 8. member.kakao_token_id FK 제거 및 nullable 변경
-- ============================================
ALTER TABLE `member`
DROP FOREIGN KEY IF EXISTS `fk_member_kakao_token`;

ALTER TABLE `member`
MODIFY COLUMN `kakao_token_id` BIGINT NULL;

DELIMITER ;

-- ============================================
-- 마이그레이션 검증
-- ============================================

-- CASCADE 추가 확인 (6개가 있어야 함)
SELECT
    CASE
        WHEN COUNT(*) = 6 THEN 'SUCCESS: 6 CASCADE constraints added'
        ELSE CONCAT('WARNING: Expected 6 CASCADE, found ', COUNT(*))
    END AS cascade_status,
    GROUP_CONCAT(CONCAT(TABLE_NAME, '.', CONSTRAINT_NAME) SEPARATOR ', ') AS cascade_list
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND DELETE_RULE = 'CASCADE'
  AND (
    CONSTRAINT_NAME = 'fk_comment_like_comment' OR
    CONSTRAINT_NAME = 'fk_comment_like_member' OR
    CONSTRAINT_NAME = 'fk_comment_post' OR
    CONSTRAINT_NAME = 'fk_post_like_post' OR
    CONSTRAINT_NAME = 'fk_post_like_member' OR
    CONSTRAINT_NAME = 'fk_member_setting'
  );

-- FK 제거 확인 (3개가 제거되어야 함)
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN 'SUCCESS: 3 FK constraints removed'
        ELSE CONCAT('WARNING: ', COUNT(*), ' FK constraints still exist')
    END AS fk_removal_status
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND (
    CONSTRAINT_NAME = 'fk_auth_token_member' OR
    CONSTRAINT_NAME = 'fk_report_member' OR
    CONSTRAINT_NAME = 'fk_member_kakao_token'
  );

-- 인덱스 유지 확인 (FK 제거된 컬럼만)
SELECT
    CONCAT('INFO: Indexes preserved for ', COUNT(*), ' columns') AS index_status
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'auth_token' AND COLUMN_NAME = 'member_id') OR
    (TABLE_NAME = 'report' AND COLUMN_NAME = 'member_id') OR
    (TABLE_NAME = 'member' AND COLUMN_NAME = 'kakao_token_id')
  );

-- ================================================================================================
-- Migration Notes:
-- 1. CASCADE 추가 (6개):
--    - comment_like.comment_id → comment (고아 레코드 방지)
--    - comment_like.member_id → member (무결성 보장)
--    - comment.post_id → post (성능 개선)
--    - post_like.post_id → post (성능 개선)
--    - post_like.member_id → member (무결성 보장)
--    - member.setting_id → setting (생명주기 일치)
--
-- 2. FK 제거 (3개):
--    - auth_token.member_id (보안 감사 이력 보존)
--    - report.member_id (법적 보관)
--    - member.kakao_token_id (독립적 라이프사이클)
--
-- 3. 애플리케이션 영향:
--    - PostCommandService.deletePost(): Comment, PostLike 삭제 코드 제거 가능
--    - CommentDeleteAdapter: CommentLike 삭제 코드 제거 가능
--    - MemberCommandAdapter.deleteMemberAndSetting(): Setting 삭제 코드 단순화 가능
--    - MemberWithdrawListener: Member 탈퇴 시 Like 자동 삭제 (DB CASCADE)
--    - JPA 엔티티: CASCADE FK는 NO_CONSTRAINT 제거, FK 제거는 NO_CONSTRAINT 유지
--
-- 4. 멱등성: 여러 번 실행해도 안전 (IF EXISTS, IF NOT NULL 사용)
-- 5. H2 호환: H2 테스트에서도 동일하게 CASCADE 적용
-- ================================================================================================
