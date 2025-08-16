package jaeik.growfarm.infrastructure.adapter.notification.in.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 알림 읽음 및 삭제 요청 시 전달 받는 DTO
@Getter
@Setter
public class UpdateNotificationDTO {
    private List<Long> readIds;
    private List<Long> deletedIds;
}
