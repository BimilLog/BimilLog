package jaeik.growfarm.entity.user;

import jaeik.growfarm.dto.user.SettingDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

// 설정 엔티티
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

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean commentFeaturedNotification = true;


    public void updateSetting(boolean farmNotification, boolean commentNotification, boolean PostIsFeaturedNotification, boolean commentFeaturedNotification) {
        this.farmNotification = farmNotification;
        this.commentNotification = commentNotification;
        this.postFeaturedNotification = PostIsFeaturedNotification;
        this.commentFeaturedNotification = commentFeaturedNotification;
    }

    public static Setting createSetting() {
        return Setting.builder()
                .farmNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .commentFeaturedNotification(true)
                .build();
    }

    public static Setting createSetting(SettingDTO settingDTO) {
        return Setting.builder()
                .farmNotification(settingDTO.isFarmNotification())
                .commentNotification(settingDTO.isCommentNotification())
                .postFeaturedNotification(settingDTO.isPostFeaturedNotification())
                .commentFeaturedNotification(settingDTO.isCommentFeaturedNotification())
                .build();
    }
}
