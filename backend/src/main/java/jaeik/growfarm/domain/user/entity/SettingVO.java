package jaeik.growfarm.domain.user.entity;

import lombok.Builder;

/**
 * <h3>설정 값 객체</h3>
 * <p>
 * 사용자 알림 설정 정보를 담는 도메인 값 객체
 * </p>
 *
 * @param messageNotification 롤링페이퍼 메시지 알림 여부
 * @param commentNotification 댓글 알림 여부  
 * @param postFeaturedNotification 게시글 추천 알림 여부
 * @author Jaeik
 * @since 2.0.0
 */
public record SettingVO(
        boolean messageNotification,
        boolean commentNotification, 
        boolean postFeaturedNotification
) {

    @Builder
    public SettingVO {
    }

    /**
     * <h3>설정 값 객체 생성</h3>
     * <p>알림 설정 값들로 SettingVO를 생성합니다.</p>
     *
     * @param messageNotification 롤링페이퍼 메시지 알림 여부
     * @param commentNotification 댓글 알림 여부
     * @param postFeaturedNotification 게시글 추천 알림 여부
     * @return SettingVO 객체
     */
    public static SettingVO of(boolean messageNotification, boolean commentNotification, boolean postFeaturedNotification) {
        return new SettingVO(messageNotification, commentNotification, postFeaturedNotification);
    }

    /**
     * <h3>기본 설정 값 객체 생성</h3>
     * <p>모든 알림이 활성화된 기본 설정 값 객체를 생성합니다.</p>
     *
     * @return 기본 설정 SettingVO 객체
     */
    public static SettingVO defaultSetting() {
        return new SettingVO(true, true, true);
    }
}