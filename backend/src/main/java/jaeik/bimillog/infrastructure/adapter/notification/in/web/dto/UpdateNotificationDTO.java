package jaeik.bimillog.infrastructure.adapter.notification.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * <h2>알림 업데이트 요청 DTO</h2>
 * <p>알림 읽음 처리 및 삭제 요청 시 전달받는 데이터 전송 객체</p>
 * <p>삭제와 읽음 처리가 동시에 요청될 경우, 삭제가 우선 처리됩니다.</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class UpdateNotificationDTO {
    
    /**
     * 읽음 처리할 알림 ID 목록
     * 최대 100개까지 처리 가능
     */
    @Size(max = 100, message = "읽음 처리는 한 번에 최대 100개까지 가능합니다.")
    private List<@Min(value = 1, message = "알림 ID는 1 이상이어야 합니다.") Long> readIds;
    
    /**
     * 삭제할 알림 ID 목록
     * 최대 100개까지 처리 가능
     */
    @Size(max = 100, message = "삭제는 한 번에 최대 100개까지 가능합니다.")
    private List<@Min(value = 1, message = "알림 ID는 1 이상이어야 합니다.") Long> deletedIds;
}
