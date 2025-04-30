package jaeik.growfarm.dto.notification;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateNotificationDTO {
    private List<Long> readIds;
    private List<Long> deletedIds;
}
