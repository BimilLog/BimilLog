package jaeik.growfarm.entity.user;

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
    private boolean isFarmNotification = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isCommentNotification = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isPostFeaturedNotification = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isCommentFeaturedNotification = true;


    public void updateSetting(boolean isFarmNotification, boolean isCommentNotification, boolean isPostIsFeaturedNotification, boolean isCommentFeaturedNotification) {
        this.isFarmNotification = isFarmNotification;
        this.isCommentNotification = isCommentNotification;
        this.isPostFeaturedNotification = isPostIsFeaturedNotification;
        this.isCommentFeaturedNotification = isCommentFeaturedNotification;
    }
}
