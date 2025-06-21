package jaeik.growfarm.entity.user;

import jaeik.growfarm.dto.user.SettingDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>설정 엔티티</h2>
 * <p>사용자의 알림 설정을 저장하는 엔티티</p>
 * <p>농장, 댓글, 게시글 추천, 댓글 추천에 대한 알림 설정을 포함</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Entity
@NoArgsConstructor
@Getter
@SuperBuilder
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "setting_id")
    private Long id;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean messageNotification = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean commentNotification = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean postFeaturedNotification = true;

    @Transactional
    public void updateSetting(SettingDTO settingDTO) {
        messageNotification = settingDTO.isMessageNotification();
        commentNotification = settingDTO.isCommentNotification();
        postFeaturedNotification = settingDTO.isPostFeaturedNotification();
    }

    public static Setting createSetting() {
        return Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
    }

    public static Setting createSetting(SettingDTO settingDTO) {
        return Setting.builder()
                .messageNotification(settingDTO.isMessageNotification())
                .commentNotification(settingDTO.isCommentNotification())
                .postFeaturedNotification(settingDTO.isPostFeaturedNotification())
                .build();
    }
}
