package jaeik.growfarm.infrastructure.adapter.user.in.web.dto;

import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.SettingVO;
import lombok.*;

/**
 * <h3>사용자 설정 DTO</h3>
 * <p>
 * 사용자의 알림 설정 정보를 담는 데이터 전송 객체
 * </p>
 * 
 * @since 2.0.0
 * @author Jaeik
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    /**
     * <h3>DTO를 SettingVO로 변환</h3>
     * <p>DTO의 알림 설정 정보를 도메인 값 객체로 변환합니다.</p>
     *
     * @return SettingVO 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public SettingVO toSettingVO() {
        return SettingVO.of(messageNotification, commentNotification, postFeaturedNotification);
    }
}
