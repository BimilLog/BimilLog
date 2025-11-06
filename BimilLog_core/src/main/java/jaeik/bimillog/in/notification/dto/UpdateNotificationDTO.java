package jaeik.bimillog.in.notification.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>알림 업데이트 요청 DTO</h2>
 * <p>알림 읽음 처리 및 삭제 요청 시 전달받는 데이터 전송 객체</p>
 * <p>삭제와 읽음 처리가 동시에 요청될 경우, 삭제가 우선 처리됩니다.</p>
 * <p>유효하지 않은 ID(null, 0 이하)는 자동으로 필터링되며 중복 ID는 제거됩니다.</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
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

    /**
     * <h3>최소 하나의 작업 필수 검증</h3>
     * <p>읽음 처리나 삭제 중 최소 하나의 작업은 요청되어야 합니다.</p>
     * 
     * @return 최소 하나의 작업이 요청되면 true
     */
    @AssertTrue(message = "읽음 처리 또는 삭제 중 최소 하나의 작업은 요청되어야 합니다.")
    public boolean isAtLeastOneOperationRequested() {
        return (readIds != null && !readIds.isEmpty()) || 
               (deletedIds != null && !deletedIds.isEmpty());
    }

    /**
     * <h3>읽음 ID 목록 설정</h3>
     * <p>Jackson deserialize 시 호출되어 자동으로 필터링 및 정제를 수행합니다.</p>
     */
    @JsonSetter("readIds")
    public void setReadIds(List<Long> readIds) {
        this.readIds = filterAndDeduplicateIds(readIds);
        applyDeletionPriority();
    }

    /**
     * <h3>삭제 ID 목록 설정</h3>
     * <p>Jackson deserialize 시 호출되어 자동으로 필터링 및 정제를 수행합니다.</p>
     */
    @JsonSetter("deletedIds")
    public void setDeletedIds(List<Long> deletedIds) {
        this.deletedIds = filterAndDeduplicateIds(deletedIds);
        applyDeletionPriority();
    }

    /**
     * <h3>삭제 우선 정책 적용</h3>
     * <p>동일 ID가 읽음과 삭제에 모두 있을 경우 읽음 목록에서 제거합니다.</p>
     */
    private void applyDeletionPriority() {
        if (deletedIds == null || deletedIds.isEmpty() || readIds == null || readIds.isEmpty()) {
            return;
        }

        Set<Long> deletedIdSet = new HashSet<>(deletedIds);
        this.readIds = readIds.stream()
                .filter(id -> !deletedIdSet.contains(id))
                .collect(Collectors.toList());
    }

    /**
     * <h3>ID 필터링 및 중복 제거</h3>
     * <p>null이나 0 이하의 ID를 제거하고 중복을 제거합니다.</p>
     * 
     * @param ids 필터링할 ID 목록
     * @return 필터링되고 중복이 제거된 ID 목록
     */
    private List<Long> filterAndDeduplicateIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }
}
