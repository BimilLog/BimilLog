package jaeik.growfarm.dto.user;

import jaeik.growfarm.entity.user.Setting;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
public class SettingDTO {

    private Long settingId;

    private boolean messageNotification;

    private boolean commentNotification;

    private boolean postFeaturedNotification;

    public SettingDTO(Setting setting) {
        settingId = setting.getId();
        messageNotification = setting.isMessageNotification();
        commentNotification = setting.isCommentNotification();
        postFeaturedNotification = setting.isPostFeaturedNotification();
    }
}
