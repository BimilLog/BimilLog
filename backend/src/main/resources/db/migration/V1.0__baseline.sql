-- BimilLog v1.0 운영 데이터베이스 Flyway Baseline 스크립트
-- 현재 운영 데이터베이스 스키마 상태를 나타냄
-- 날짜: 2025-09-13
-- 경고: 이 스크립트는 baseline 전용입니다. 기존 운영 데이터베이스에서 실행하지 마세요.

-- 설정 테이블 (외래 키 의존성 때문에 먼저 생성해야 함)
CREATE TABLE IF NOT EXISTS `setting` (
  `comment_notification` tinyint(1) NOT NULL DEFAULT '1',
  `message_notification` tinyint(1) NOT NULL DEFAULT '1',
  `post_featured_notification` tinyint(1) NOT NULL DEFAULT '1',
  `setting_id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS `users` (
  `created_at` timestamp NOT NULL,
  `kakao_id` bigint NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `setting_id` bigint NOT NULL,
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `kakao_nickname` varchar(255) DEFAULT NULL,
  `thumbnail_image` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UKk4ycaj27putgcujmehwbsrmmc` (`kakao_id`),
  UNIQUE KEY `UK9hnjksuc1u7qt5o1s6xeo279f` (`setting_id`),
  UNIQUE KEY `UKk8d0f2n7n88w1a16yhua64onx` (`user_name`),
  KEY `idx_user_username` (`user_name`),
  KEY `idx_user_kakao_id_username` (`kakao_id`,`user_name`),
  CONSTRAINT `FK6g01qqmugyfy1ps5jkvbqu8to` FOREIGN KEY (`setting_id`) REFERENCES `setting` (`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 게시글 테이블
CREATE TABLE IF NOT EXISTS `post` (
  `is_notice` bit(1) NOT NULL,
  `password` int DEFAULT NULL,
  `views` int NOT NULL,
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `post_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `title` varchar(30) NOT NULL,
  `content` text NOT NULL,
  `popular_flag` enum('LEGEND','REALTIME','WEEKLY') DEFAULT NULL,
  PRIMARY KEY (`post_id`),
  KEY `idx_post_notice_created` (`is_notice`,`created_at` DESC),
  KEY `idx_post_created_at_popular` (`created_at`,`popular_flag`),
  KEY `idx_post_created` (`created_at`),
  KEY `idx_post_popular_flag` (`popular_flag`),
  KEY `FK7ky67sgi7k0ayf22652f7763r` (`user_id`),
  FULLTEXT KEY `post_title_content_IDX` (`title`,`content`) WITH PARSER `ngram`,
  FULLTEXT KEY `post_title_IDX` (`title`) WITH PARSER `ngram`,
  CONSTRAINT `FK7ky67sgi7k0ayf22652f7763r` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 댓글 테이블
CREATE TABLE IF NOT EXISTS `comment` (
  `deleted` bit(1) NOT NULL,
  `password` int DEFAULT NULL,
  `comment_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `post_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `content` varchar(255) NOT NULL,
  PRIMARY KEY (`comment_id`),
  KEY `idx_comment_post_deleted` (`post_id`,`deleted`),
  KEY `idx_comment_post_created` (`post_id`,`created_at` DESC),
  KEY `FKqm52p1v3o13hy268he0wcngr5` (`user_id`),
  CONSTRAINT `FKqm52p1v3o13hy268he0wcngr5` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKs1slvnkuemjsq2kj4h3vhx7i1` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 계층 구조를 위한 댓글 클로저 테이블
CREATE TABLE IF NOT EXISTS `comment_closure` (
  `ancestor_id` bigint NOT NULL,
  `descendant_id` bigint NOT NULL,
  `depth` int NOT NULL,
  PRIMARY KEY (`ancestor_id`,`descendant_id`),
  KEY `FKqvgqoqxs6cg00eiqxhu0h7yiq` (`descendant_id`),
  CONSTRAINT `FK4x9hqm1klpp9lh4qxr7o8xnqh` FOREIGN KEY (`ancestor_id`) REFERENCES `comment` (`comment_id`) ON DELETE CASCADE,
  CONSTRAINT `FKqvgqoqxs6cg00eiqxhu0h7yiq` FOREIGN KEY (`descendant_id`) REFERENCES `comment` (`comment_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 댓글 클로저 시퀀스
CREATE TABLE IF NOT EXISTS `comment_closure_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 댓글 좋아요 테이블
CREATE TABLE IF NOT EXISTS `comment_like` (
  `comment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`comment_id`,`user_id`),
  KEY `FKqlr0s8micj0avhupsfdjgrfqw` (`user_id`),
  CONSTRAINT `FKqlr0s8micj0avhupsfdjgrfqw` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `FKqlvjy0py8bqvgl6bc139hswse` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`comment_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 게시글 좋아요 테이블
CREATE TABLE IF NOT EXISTS `post_like` (
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`post_id`,`user_id`),
  KEY `FK8xhrvvrnaj1iih86xw2gvjnld` (`user_id`),
  CONSTRAINT `FK8xhrvvrnaj1iih86xw2gvjnld` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `FKj7iy0k7n3d0vkh8o7ibjna884` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 메시지 테이블 (롤링페이퍼 메시지)
CREATE TABLE IF NOT EXISTS `message` (
  `deleted` bit(1) NOT NULL,
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `deco_type` enum('BLUE','CREAM','CYAN','GREEN','MINT','PINK','PURPLE','RED','WHITE','YELLOW') NOT NULL,
  `content` varchar(255) NOT NULL,
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `receiver_id` bigint NOT NULL,
  `grid_x` int NOT NULL,
  `grid_y` int NOT NULL,
  `sender_name` varchar(255) NOT NULL,
  PRIMARY KEY (`message_id`),
  KEY `idx_message_receiver_deleted` (`receiver_id`,`deleted`),
  KEY `idx_message_receiver_created` (`receiver_id`,`created_at` DESC),
  CONSTRAINT `FKhdqgcc1ivl46yj4qwb9o2iks2` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 알림 테이블
CREATE TABLE IF NOT EXISTS `notification` (
  `read_status` bit(1) NOT NULL,
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `content` varchar(255) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `notification_type` enum('COMMENT','COMMENT_FEATURED','MESSAGE','POST_FEATURED') NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `idx_notification_user_read` (`user_id`,`read_status`),
  KEY `idx_notification_user_created` (`user_id`,`created_at` DESC),
  CONSTRAINT `FKb0yvoep4h4k92ipon31wmdf7e` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- FCM 토큰 테이블
CREATE TABLE IF NOT EXISTS `fcm_token` (
  `created_at` timestamp NOT NULL,
  `fcm_id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` timestamp NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `token` varchar(255) NOT NULL,
  PRIMARY KEY (`fcm_id`),
  UNIQUE KEY `UK96hl8vgfpqh6wjyetct61jnrm` (`token`),
  KEY `FKsf7yqsg6h0b8c3hxjr9wqjtvj` (`user_id`),
  CONSTRAINT `FKsf7yqsg6h0b8c3hxjr9wqjtvj` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 토큰 테이블 (JWT 리프레시 토큰)
CREATE TABLE IF NOT EXISTS `token` (
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `token_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `refresh_token` varchar(255) NOT NULL,
  PRIMARY KEY (`token_id`),
  KEY `idx_token_user` (`user_id`),
  KEY `idx_token_refresh` (`refresh_token`),
  CONSTRAINT `FKe32ek7ixanakfqsdaokm4q9y2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 신고 테이블 (v1에 이미 존재)
CREATE TABLE IF NOT EXISTS `report` (
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `target_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `content` text NOT NULL,
  `report_type` enum('COMMENT','ERROR','IMPROVEMENT','POST') NOT NULL,
  PRIMARY KEY (`report_id`),
  KEY `FKq50wsn94sc3mi90gtidk0k34a` (`user_id`),
  CONSTRAINT `FKq50wsn94sc3mi90gtidk0k34a` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 블랙리스트 테이블 (v1에 이미 존재)
CREATE TABLE IF NOT EXISTS `black_list` (
  `created_at` timestamp NOT NULL,
  `kakao_id` bigint NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`kakao_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;