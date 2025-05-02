package jaeik.growfarm.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingDTO {

    private boolean isAllNotification;

    private boolean isFarmNotification;

    private boolean isCommentNotification;

    private boolean isPostFeaturedNotification;

    private boolean isCommentFeaturedNotification;
}
