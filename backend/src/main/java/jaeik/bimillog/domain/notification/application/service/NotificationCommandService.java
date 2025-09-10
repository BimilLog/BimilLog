package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.notification.in.web.NotificationCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>알림 명령 서비스</h2>
 * <p>알림 도메인의 명령 작업을 담당하는 서비스입니다.</p>
 * <p>알림 읽음 처리, 알림 삭제</p>
 * <p>입력값 검증, 중복 ID 제거, 데이터 무결성 보장</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService implements NotificationCommandUseCase {

    private final NotificationCommandPort notificationCommandPort;

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>여러 알림에 대해 읽음 처리 또는 삭제를 일괄 수행합니다.</p>
     * <p>읽음 처리: isRead 상태를 true로 변경</p>
     * <p>삭제: 알림을 완전 제거</p>
     * <p>동일 ID에 대해 읽음과 삭제가 동시 요청된 경우 삭제가 우선됩니다.</p>
     * <p>{@link NotificationCommandController}에서 사용자의 알림 관리 API 요청 시 호출됩니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param updateCommand 알림 업데이트 명령 객체 (읽음/삭제 여부와 대상 알림 ID 목록 포함)
     * @throws NotificationCustomException 사용자 정보가 유효하지 않은 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void batchUpdate(CustomUserDetails userDetails, NotificationUpdateVO updateCommand) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new NotificationCustomException(NotificationErrorCode.INVALID_USER_CONTEXT);
        }

        if (updateCommand == null) {
            log.debug("업데이트 명령이 null입니다. 처리를 건너뜁니다.");
            return;
        }

        Long userId = userDetails.getUserId();

        // 입력값 검증 및 정제
        List<Long> readIds = validateAndFilterIds(updateCommand.readIds(), "읽음 처리");
        List<Long> deletedIds = validateAndFilterIds(updateCommand.deletedIds(), "삭제");

        // 중복 ID 처리: 삭제가 우선이므로 readIds에서 deletedIds에 있는 항목 제거
        List<Long> finalReadIds = readIds;
        if (!deletedIds.isEmpty() && !readIds.isEmpty()) {
            Set<Long> deletedIdSet = new HashSet<>(deletedIds);
            finalReadIds = readIds.stream()
                    .filter(id -> !deletedIdSet.contains(id))
                    .collect(Collectors.toList());

            if (finalReadIds.size() != readIds.size()) {
                log.warn("읽음 처리 목록에서 {} 개의 중복 ID를 제거했습니다. (삭제 우선 정책)",
                        readIds.size() - finalReadIds.size());
            }
        }

        // Port 호출
        notificationCommandPort.batchUpdate(userId, NotificationUpdateVO.of(finalReadIds, deletedIds));

        log.info("사용자 {}의 알림 업데이트 완료: {} 개 읽음 처리, {} 개 삭제",
                userId, finalReadIds.size(), deletedIds.size());
    }

    /**
     * <h3>ID 목록 검증 및 필터링</h3>
     * <p>알림 ID 목록의 유효성을 검사하고 비즈니스 규칙에 맞게 정제합니다.</p>
     * <p>비즈니스 규칙: null 또는 0 이하의 ID는 제거, 중복 ID는 하나만 유지</p>
     * <p>데이터 무결성을 위해 모든 부적절한 데이터를 사전에 제거하여 예외를 방지합니다.</p>
     * <p>batchUpdate 메서드에서 입력값 검증 및 정제를 위해 호출됩니다.</p>
     *
     * @param ids 검증할 알림 ID 목록 (null 가능)
     * @param operation 작업 이름 (로깅 및 디버깅용)
     * @return 검증되고 중복이 제거된 유효한 ID 목록
     */
    private List<Long> validateAndFilterIds(List<Long> ids, String operation) {
        if (ids == null) {
            return Collections.emptyList();
        }

        List<Long> validIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());

        // 잘못된 ID가 있을 경우 로그 기록
        if (ids.size() != validIds.size()) {
            List<Long> invalidIds = ids.stream()
                    .filter(id -> id == null || id <= 0)
                    .collect(Collectors.toList());
            log.warn("{} 작업에서 {} 개의 잘못된 ID를 제거했습니다: {}",
                    operation, invalidIds.size(), invalidIds);
        }

        // 중복 제거
        List<Long> uniqueIds = validIds.stream()
                .distinct()
                .collect(Collectors.toList());

        // 중복 ID가 있을 경우 로그 기록
        if (validIds.size() != uniqueIds.size()) {
            log.debug("{} 작업에서 {} 개의 중복 ID를 제거했습니다.",
                    operation, validIds.size() - uniqueIds.size());
        }

        return uniqueIds;
    }

}
