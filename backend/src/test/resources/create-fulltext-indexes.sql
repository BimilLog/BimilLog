-- FULLTEXT 인덱스 생성 (InnoDB with ngram parser)
-- MySQL 8.0 InnoDB는 FULLTEXT를 지원합니다

-- 기존 인덱스가 있을 수 있으므로 무시
-- MySQL은 IF NOT EXISTS를 지원하지 않으므로 에러를 무시해야 함

CREATE FULLTEXT INDEX idx_post_title ON post(title) WITH PARSER ngram;
CREATE FULLTEXT INDEX idx_post_title_content ON post(title, content) WITH PARSER ngram;