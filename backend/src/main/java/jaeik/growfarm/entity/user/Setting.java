package jaeik.growfarm.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor
@Getter
@SuperBuilder
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "setting_id")
    private Long id;

    @Column(nullable = false)
    private boolean isAllNotification = true;

    @Column(nullable = false)
    private boolean isFarmNotification = true;

    @Column(nullable = false)
    private boolean isCommentNotification = true;

    @Column(nullable = false)
    private boolean isPostFeaturedNotification = true;

    @Column(nullable = false)
    private boolean isCommentFeaturedNotification = true;


    public void updateSetting(boolean isAllNotification, boolean isFarmNotification, boolean isCommentNotification, boolean isPostIsFeaturedNotification, boolean isCommentFeaturedNotification) {
        this.isAllNotification = isAllNotification;
        this.isFarmNotification = isFarmNotification;
        this.isCommentNotification = isCommentNotification;
        this.isPostFeaturedNotification = isPostIsFeaturedNotification;
        this.isCommentFeaturedNotification = isCommentFeaturedNotification;
    }
}
