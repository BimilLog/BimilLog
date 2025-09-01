package jaeik.bimillog.infrastructure.adapter.user.in.web.dto;

import jaeik.bimillog.domain.user.entity.SettingVO;
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

    private boolean messageNotification;

    private boolean commentNotification;

    private boolean postFeaturedNotification;

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

    /**
     * <h3>SettingVO로부터 SettingDTO 생성</h3>
     * <p>도메인 값 객체를 DTO로 변환합니다.</p>
     *
     * @param settingVO 설정 값 객체
     * @return SettingDTO 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static SettingDTO fromSettingVO(SettingVO settingVO) {
        return SettingDTO.builder()
                .messageNotification(settingVO.messageNotification())
                .commentNotification(settingVO.commentNotification())
                .postFeaturedNotification(settingVO.postFeaturedNotification())
                .build();
    }
}
