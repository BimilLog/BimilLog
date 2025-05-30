package jaeik.growfarm.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * <h3>사용자 설정 DTO</h3>
 * <p>
 * 사용자의 알림 설정 정보를 담는 데이터 전송 객체
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Getter
@Setter
public class SettingDTO {

    private boolean FarmNotification;

    private boolean CommentNotification;

    private boolean PostFeaturedNotification;

    private boolean CommentFeaturedNotification;
}
