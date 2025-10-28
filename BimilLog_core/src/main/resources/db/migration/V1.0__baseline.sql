-- BimilLog v1.0 운영 데이터베이스 Flyway Baseline 스크립트
-- 현재 운영 데이터베이스 스키마 상태를 나타냄
-- 날짜: 2025-09-14
-- 경고: 이 스크립트는 baseline 전용입니다. 기존 운영 데이터베이스에서 실행하지 마세요.

-- 설정 테이블
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
  FULLTEXT KEY `post_title_content_IDX` (`title`,`content`) /*!50100 WITH PARSER `ngram` */ ,
  FULLTEXT KEY `post_title_IDX` (`title`) /*!50100 WITH PARSER `ngram` */ ,
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

-- 댓글 클로저 테이블 (운영 DB 구조와 일치)
CREATE TABLE IF NOT EXISTS `comment_closure` (
  `depth` int NOT NULL,
  `ancestor_id` bigint DEFAULT NULL,
  `descendant_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comment_closure_ancestor_depth` (`descendant_id`,`depth`),
  KEY `FKs0fq4gba8fnn9ig1lvwfedvuw` (`ancestor_id`),
  CONSTRAINT `FKhgtsb6jr6v1e4wlndlrodjhd4` FOREIGN KEY (`descendant_id`) REFERENCES `comment` (`comment_id`),
  CONSTRAINT `FKs0fq4gba8fnn9ig1lvwfedvuw` FOREIGN KEY (`ancestor_id`) REFERENCES `comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 댓글 클로저 시퀀스
CREATE TABLE IF NOT EXISTS `comment_closure_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 댓글 좋아요 테이블 (운영 DB 구조와 일치)
CREATE TABLE IF NOT EXISTS `comment_like` (
  `comment_id` bigint DEFAULT NULL,
  `comment_like_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`comment_like_id`),
  KEY `uk_comment_like_user_comment` (`comment_id`,`user_id`),
  KEY `FKl5wrmp8eoy5uegdo3473jqqi` (`user_id`),
  CONSTRAINT `FKl5wrmp8eoy5uegdo3473jqqi` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `FKqlv8phl1ibeh0efv4dbn3720p` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`comment_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 게시글 좋아요 테이블 (운영 DB 구조와 일치)
CREATE TABLE IF NOT EXISTS `post_like` (
  `post_id` bigint DEFAULT NULL,
  `post_like_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`post_like_id`),
  KEY `idx_postlike_user_post` (`user_id`,`post_id`),
  KEY `FKj7iy0k7n3d0vkh8o7ibjna884` (`post_id`),
  CONSTRAINT `FKijnjmw0imnatadr3agtk0udip` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `FKj7iy0k7n3d0vkh8o7ibjna884` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 메시지 테이블 (운영 DB 구조와 일치)
CREATE TABLE IF NOT EXISTS `message` (
  `height` int NOT NULL,
  `width` int NOT NULL,
  `anonymity` varchar(8) NOT NULL,
  `created_at` timestamp NOT NULL,
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` timestamp NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `deco_type` enum('POTATO','CARROT','CABBAGE','TOMATO','STRAWBERRY','BLUEBERRY','WATERMELON','PUMPKIN','APPLE','GRAPE','BANANA','GOBLIN','SLIME','ORC','DRAGON','PHOENIX','WEREWOLF','ZOMBIE','KRAKEN','CYCLOPS','DEVIL','ANGEL','COFFEE','MILK','WINE','SOJU','BEER','BUBBLETEA','SMOOTHIE','BORICHA','STRAWBERRYMILK','BANANAMILK','BREAD','BURGER','CAKE','SUSHI','PIZZA','CHICKEN','NOODLE','EGG','SKEWER','KIMBAP','SUNDAE','MANDU','SAMGYEOPSAL','FROZENFISH','HOTTEOK','COOKIE','PICKLE','CAT','DOG','RABBIT','FOX','TIGER','PANDA','LION','ELEPHANT','SQUIRREL','HEDGEHOG','CRANE','SPARROW','CHIPMUNK','GIRAFFE','HIPPO','POLARBEAR','BEAR','STAR','SUN','MOON','VOLCANO','CHERRY','MAPLE','BAMBOO','SUNFLOWER','STARLIGHT','CORAL','ROCK','WATERDROP','WAVE','RAINBOW','DOLL','BALLOON','SNOWMAN','FAIRY','BUBBLE') DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  UNIQUE KEY `unique_user_x_y` (`user_id`,`width`,`height`),
  CONSTRAINT `FKpdrb79dg3bgym7pydlf9k3p1n` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 알림 테이블 (운영 DB 구조와 일치)
CREATE TABLE IF NOT EXISTS `notification` (
  `is_read` bit(1) NOT NULL,
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `data` varchar(255) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `notification_type` enum('ADMIN','COMMENT','COMMENT_FEATURED','FARM','INITIATE','POST_FEATURED') NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `idx_notification_user_created` (`user_id`,`created_at` DESC),
  CONSTRAINT `FKnk4ftb5am9ubmkv1661h15ds9` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- FCM 토큰 테이블 (운영 DB 구조와 일치)
CREATE TABLE IF NOT EXISTS `fcm_token` (
  `created_at` timestamp NOT NULL,
  `fcm_token_id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` timestamp NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `fcm_registration_token` varchar(255) NOT NULL,
  PRIMARY KEY (`fcm_token_id`),
  KEY `FKtel499bxk6jvp3fg43e1l2ka8` (`user_id`),
  CONSTRAINT `FKtel499bxk6jvp3fg43e1l2ka8` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 토큰 테이블 (운영 DB 구조와 일치)
CREATE TABLE IF NOT EXISTS `token` (
  `created_at` timestamp NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  `token_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `jwt_refresh_token` varchar(255) DEFAULT NULL,
  `kakao_access_token` varchar(255) NOT NULL,
  `kakao_refresh_token` varchar(255) NOT NULL,
  PRIMARY KEY (`token_id`),
  KEY `FKj8rfw4x0wjjyibfqq566j4qng` (`user_id`),
  CONSTRAINT `FKj8rfw4x0wjjyibfqq566j4qng` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 신고 테이블
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

-- 블랙리스트 테이블
CREATE TABLE IF NOT EXISTS `black_list` (
  `created_at` timestamp NOT NULL,
  `kakao_id` bigint NOT NULL,
  `modified_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`kakao_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;