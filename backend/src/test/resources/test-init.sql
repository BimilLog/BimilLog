-- MySQL ngram parser 설정
SET GLOBAL ngram_token_size = 2;

-- FULLTEXT 인덱스는 테이블 생성 후 추가해야 함
-- JPA가 테이블을 생성한 후 이 스크립트가 실행됨