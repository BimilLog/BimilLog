package jaeik.bimillog.infrastructure.adapter.in.member.dto;

import jaeik.bimillog.domain.member.entity.Setting;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "메시지 알림 설정은 필수입니다")
    private Boolean messageNotification;

    @NotNull(message = "댓글 알림 설정은 필수입니다") 
    private Boolean commentNotification;

    @NotNull(message = "게시글 추천 알림 설정은 필수입니다")
    private Boolean postFeaturedNotification;

    /**
     * <h3>DTO를 Setting 엔티티로 변환</h3>
     * <p>DTO의 알림 설정 정보를 Setting 엔티티로 변환합니다.</p>
     *
     * @return Setting 엔티티 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public Setting toSettingEntity() {
        return Setting.builder()
                .messageNotification(messageNotification.booleanValue())
                .commentNotification(commentNotification.booleanValue())
                .postFeaturedNotification(postFeaturedNotification.booleanValue())
                .build();
    }

    /**
     * <h3>Setting 엔티티로부터 SettingDTO 생성</h3>
     * <p>도메인 엔티티를 DTO로 변환합니다.</p>
     *
     * @param setting 설정 엔티티
     * @return SettingDTO 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static SettingDTO fromSetting(Setting setting) {
        return SettingDTO.builder()
                .messageNotification(Boolean.valueOf(setting.isMessageNotification()))
                .commentNotification(Boolean.valueOf(setting.isCommentNotification()))
                .postFeaturedNotification(Boolean.valueOf(setting.isPostFeaturedNotification()))
                .build();
    }
}
