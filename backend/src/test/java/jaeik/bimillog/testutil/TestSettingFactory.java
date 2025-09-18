package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.Setting;

/**
 * <h2>테스트 설정 생성 팩토리</h2>
 * <p>테스트에서 사용할 Setting 엔티티를 생성하는 유틸리티 클래스</p>
 * <p>중복되는 설정 생성 코드를 중앙화하여 DRY 원칙 준수</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestSettingFactory {

    /**
     * 기본 설정 생성 (모든 알림 true)
     */
    public static Setting createDefaultSetting() {
        return Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
    }

    /**
     * 특정 ID를 가진 기본 설정 생성
     */
    public static Setting createDefaultSettingWithId(Long id) {
        return Setting.builder()
                .id(id)
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
    }

    /**
     * 모든 알림 비활성화 설정 생성
     */
    public static Setting createAllDisabledSetting() {
        return Setting.builder()
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(false)
                .build();
    }

    /**
     * 메시지 알림만 활성화된 설정 생성
     */
    public static Setting createMessageOnlySetting() {
        return Setting.builder()
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(false)
                .build();
    }

    /**
     * 댓글 알림만 활성화된 설정 생성
     */
    public static Setting createCommentOnlySetting() {
        return Setting.builder()
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();
    }

    /**
     * 게시글 추천 알림만 활성화된 설정 생성
     */
    public static Setting createPostFeaturedOnlySetting() {
        return Setting.builder()
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();
    }

    /**
     * 커스터마이징 가능한 설정 생성
     */
    public static Setting createCustomSetting(boolean messageNotification,
                                             boolean commentNotification,
                                             boolean postFeaturedNotification) {
        return Setting.builder()
                .messageNotification(messageNotification)
                .commentNotification(commentNotification)
                .postFeaturedNotification(postFeaturedNotification)
                .build();
    }

    /**
     * 빌더 패턴으로 커스터마이징 가능한 설정 생성
     */
    public static TestSettingBuilder builder() {
        return new TestSettingBuilder();
    }

    /**
     * 테스트 설정 빌더 클래스
     */
    public static class TestSettingBuilder {
        private Long id;
        private boolean messageNotification = true;
        private boolean commentNotification = true;
        private boolean postFeaturedNotification = true;

        public TestSettingBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public TestSettingBuilder withMessageNotification(boolean messageNotification) {
            this.messageNotification = messageNotification;
            return this;
        }

        public TestSettingBuilder withCommentNotification(boolean commentNotification) {
            this.commentNotification = commentNotification;
            return this;
        }

        public TestSettingBuilder withPostFeaturedNotification(boolean postFeaturedNotification) {
            this.postFeaturedNotification = postFeaturedNotification;
            return this;
        }

        public TestSettingBuilder disableAllNotifications() {
            this.messageNotification = false;
            this.commentNotification = false;
            this.postFeaturedNotification = false;
            return this;
        }

        public TestSettingBuilder enableAllNotifications() {
            this.messageNotification = true;
            this.commentNotification = true;
            this.postFeaturedNotification = true;
            return this;
        }

        public Setting build() {
            return Setting.builder()
                    .id(id)
                    .messageNotification(messageNotification)
                    .commentNotification(commentNotification)
                    .postFeaturedNotification(postFeaturedNotification)
                    .build();
        }
    }
}