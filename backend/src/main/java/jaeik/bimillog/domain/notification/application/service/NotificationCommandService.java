package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
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
 * <p>
 * 헥사고날 아키텍처에서 NotificationCommandUseCase를 구현하는 Application Service입니다.
 * 알림의 상태 변경과 삭제에 관한 비즈니스 로직을 처리하며, CQRS 패턴의 명령 부분을 담당합니다.
 * </p>
 * <p>
 * 사용자의 알림 관리 요청(읽음 처리, 삭제)을 처리하고, 입력값 검증과 비즈니스 규칙 적용을 수행합니다.
 * 중복 ID 제거, 유효성 검사, 트랜잭션 관리 등을 통해 데이터 무결성을 보장합니다.
 * </p>
 * <p>NotificationCommandController에서 호출되며, NotificationCommandPort를 통해 데이터 저장소에 접근합니다.</p>
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
     * <h3>사용자 알림 관리 화면에서 일괄 처리 요청 시 알림 상태 업데이트</h3>
     * <p>사용자가 알림 목록 화면에서 여러 알림을 선택한 후 '읽음 처리' 또는 '삭제' 버튼을 클릭하는 상황에서 
     * NotificationCommandController가 해당 요청을 받아 이 메서드를 호출하여 
     * 선택된 알림들의 상태를 일괄 업데이트합니다.</p>
     * <p>알림 설정 화면에서 '모든 알림 읽음' 기능을 사용하거나, 알림 목록에서 개별 알림을 체크박스로 선택한 후 
     * 일괄 처리 버튼을 누르는 사용자 액션에 대응하여 NotificationCommandPort를 통해 DB 업데이트를 수행합니다.</p>
     * <p>동일 알림에 대해 읽음과 삭제가 동시 요청된 경우 삭제가 우선되며, 중복 ID 제거와 입력값 검증을 통해 
     * 데이터 무결성을 보장하고 불필요한 DB 쿼리를 방지합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (알림 소유권 검증용)
     * @param updateCommand 일괄 처리할 알림 ID 목록 (읽음 처리/삭제 구분)
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
     * <p>알림 ID 목록의 유횤성을 검사하고 비즈니스 규칙에 맞게 정제합니다.</p>
     * <p>비즈니스 규칙: null 또는 0 이하의 ID는 제거, 중복 ID는 하나만 유지</p>
     * <p>데이터 무결성을 위해 모든 부적절한 데이터를 사전에 제거하여 예외를 방지합니다.</p>
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
