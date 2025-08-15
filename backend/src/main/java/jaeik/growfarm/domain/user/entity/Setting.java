package jaeik.growfarm.domain.user.entity;

import jaeik.growfarm.dto.user.SettingDTO;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * <h2>설정 엔티티</h2>
 * <p>
 * 사용자의 알림 설정을 저장하는 엔티티
 * </p>
 * <p>
 * 농장, 댓글, 게시글 추천, 댓글 추천에 대한 알림 설정을 포함
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "setting_id")
    private Long id;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean messageNotification = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean commentNotification = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean postFeaturedNotification = true;

    /**
     * <h3>설정 업데이트</h3>
     * <p>주어진 DTO를 사용하여 알림 설정을 업데이트합니다.</p>
     *
     * @param settingDTO 업데이트할 설정 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateSetting(SettingDTO settingDTO) {
        messageNotification = settingDTO.isMessageNotification();
        commentNotification = settingDTO.isCommentNotification();
        postFeaturedNotification = settingDTO.isPostFeaturedNotification();
    }

    /**
     * <h3>기본 설정 생성</h3>
     * <p>기본값(모두 true)으로 새로운 설정 엔티티를 생성합니다.</p>
     *
     * @return 기본 설정 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Setting createSetting() {
        return Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
    }
}
