package jaeik.growfarm.dto.user;

import jaeik.growfarm.entity.user.Setting;
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

    private Long settingId;

    private boolean farmNotification;

    private boolean commentNotification;

    private boolean postFeaturedNotification;

    private boolean commentFeaturedNotification;

    public SettingDTO(Setting setting) {
        settingId = setting.getId();
        farmNotification = setting.isFarmNotification();
        commentNotification = setting.isCommentNotification();
        postFeaturedNotification = setting.isPostFeaturedNotification();
        commentFeaturedNotification = setting.isCommentFeaturedNotification();
    }

    public SettingDTO(Long settingId, boolean farmNotification, boolean commentNotification, boolean postFeaturedNotification, boolean commentFeaturedNotification) {
        this.settingId = settingId;
        this.farmNotification = farmNotification;
        this.commentNotification = commentNotification;
        this.postFeaturedNotification = postFeaturedNotification;
        this.commentFeaturedNotification = commentFeaturedNotification;
    }
}
