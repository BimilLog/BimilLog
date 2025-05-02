package jaeik.growfarm.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingDTO {

    private boolean FarmNotification;

    private boolean CommentNotification;

    private boolean PostFeaturedNotification;

    private boolean CommentFeaturedNotification;
}
