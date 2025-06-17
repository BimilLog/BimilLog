package jaeik.growfarm.entity.user;

import jaeik.growfarm.dto.user.SettingDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
    private boolean farmNotification = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean commentNotification = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean postFeaturedNotification = true;


    public void updateSetting(boolean farmNotification, boolean commentNotification, boolean PostIsFeaturedNotification) {
        this.farmNotification = farmNotification;
        this.commentNotification = commentNotification;
        this.postFeaturedNotification = PostIsFeaturedNotification;
    }

    public static Setting createSetting() {
        return Setting.builder()
                .farmNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
    }

    public static Setting createSetting(SettingDTO settingDTO) {
        return Setting.builder()
                .farmNotification(settingDTO.isFarmNotification())
                .commentNotification(settingDTO.isCommentNotification())
                .postFeaturedNotification(settingDTO.isPostFeaturedNotification())
                .build();
    }
}
