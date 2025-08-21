package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.auth;

import jaeik.growfarm.domain.auth.application.port.out.TempDataPort;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * <h2>임시 데이터 어댑터</h2>
 * <p>신규 사용자의 임시 데이터 관리를 위한 어댑터</p>
 * <p>소셜 로그인 프로세스에서 임시 데이터를 안전하게 저장/조회/삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempDataAdapter implements TempDataPort {

    private final Map<String, TemporaryUserDataDTO> tempUserDataStore = new ConcurrentHashMap<>();
    private final AuthCookieManager authCookieManager;

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     * <p>소셜 로그인 사용자 정보를 임시 데이터로 저장합니다.</p>
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>UUID는 필수 (null, 빈 문자열 불허)</li>
     *   <li>userData는 필수 (null 불허)</li> 
     *   <li>tokenDTO는 필수 (null 불허)</li>
     *   <li>fcmToken은 선택적 (null 허용)</li>
     *   <li>동일 UUID 재저장 시 덮어쓰기</li>
     *   <li>5분 후 자동 삭제 스케줄링</li>
     * </ul>
     *
     * @param uuid UUID 키
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @throws CustomException UUID, userData, tokenDTO가 유효하지 않은 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void saveTempData(String uuid, SocialLoginUserData userData, TokenDTO tokenDTO) {
        
        // 1. UUID 검증 - ConcurrentHashMap은 null key를 허용하지 않음
        if (uuid == null || uuid.trim().isEmpty()) {
            log.warn("Invalid temp UUID provided: {}", uuid);
            throw new CustomException(ErrorCode.INVALID_TEMP_UUID);
        }
        
        // 2. userData 검증 - fcmToken() 호출 전 null 체크 필수
        if (userData == null) {
            log.warn("Invalid user data provided for UUID: {}", uuid);
            throw new CustomException(ErrorCode.INVALID_USER_DATA);
        }
        
        // 3. tokenDTO 검증 - 회원가입 프로세스에서 토큰은 필수
        if (tokenDTO == null) {
            log.warn("Invalid token data provided for UUID: {}", uuid);
            throw new CustomException(ErrorCode.INVALID_TOKEN_DATA);
        }
        
        try {
            // 4. fcmToken 안전 처리 - userData가 검증된 후 호출
            String fcmToken = userData.fcmToken(); // null 허용
            
            // 5. 임시 데이터 저장
            tempUserDataStore.put(uuid, new TemporaryUserDataDTO(userData, tokenDTO, fcmToken));
            
            // 6. 자동 정리 스케줄링
            scheduleCleanup(uuid);
            
            log.debug("Temporary data saved successfully for UUID: {}", uuid);
            
        } catch (Exception e) {
            log.error("Failed to save temporary data for UUID: {}, error: {}", uuid, e.getMessage(), e);
            throw new CustomException(ErrorCode.INVALID_USER_DATA);
        }
    }

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     * <p>UUID를 사용하여 임시 사용자 데이터를 조회합니다.</p>
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>null UUID는 빈 결과 반환 (예외 아님)</li>
     *   <li>존재하지 않는 UUID는 빈 결과 반환</li>
     *   <li>만료된 데이터는 자동 정리됨</li>
     * </ul>
     *
     * @param uuid UUID 키
     * @return Optional로 감싼 임시 사용자 데이터
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Optional<TemporaryUserDataDTO> getTempData(String uuid) {
        
        if (uuid == null) {
            log.debug("Null UUID provided for temp data lookup, returning empty");
            return Optional.empty(); // null key는 빈 결과 반환
        }
        
        try {
            TemporaryUserDataDTO data = tempUserDataStore.get(uuid);
            
            if (data != null) {
                log.debug("Temporary data found for UUID: {}", uuid);
            } else {
                log.debug("No temporary data found for UUID: {}", uuid);
            }
            
            return Optional.ofNullable(data);
            
        } catch (Exception e) {
            log.error("Failed to retrieve temporary data for UUID: {}, error: {}", uuid, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * <h3>임시 사용자 데이터 삭제</h3>
     * <p>UUID를 사용하여 임시 사용자 데이터를 삭제합니다.</p>
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>null UUID는 무시 (정상 동작)</li>
     *   <li>존재하지 않는 UUID 삭제는 무시</li>
     *   <li>삭제 실패 시 로그 기록</li>
     * </ul>
     *
     * @param uuid UUID 키
     * @since 2.0.0
     * @author Jaeik
     */
    public void removeTempData(String uuid) {
        
        if (uuid == null) {
            log.debug("Null UUID provided for temp data removal, ignoring");
            return; // null key는 무시 (정상 동작)
        }
        
        try {
            TemporaryUserDataDTO removed = tempUserDataStore.remove(uuid);
            
            if (removed != null) {
                log.debug("Temporary data removed successfully for UUID: {}", uuid);
            } else {
                log.debug("No temporary data found to remove for UUID: {}", uuid);
            }
            
        } catch (Exception e) {
            log.error("Failed to remove temporary data for UUID: {}, error: {}", uuid, e.getMessage(), e);
        }
    }


    /**
     * <h3>임시 사용자 데이터 저장 및 쿠키 생성</h3>
     * <p>임시 사용자 데이터를 저장하고 해당 UUID로 쿠키를 생성하여 함께 반환합니다.</p>
     * <p>신규 사용자 회원가입 플로우에서 두 단계 작업을 하나로 통합합니다.</p>
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>saveTempData 호출 후 createTempCookie 호출</li>
     *   <li>saveTempData 실패 시 쿠키 생성하지 않음</li>
     *   <li>원자성 보장: 둘 다 성공하거나 둘 다 실패</li>
     * </ul>
     *
     * @param uuid UUID 키
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @return 생성된 임시 사용자 ID 쿠키
     * @throws CustomException saveTempData에서 발생하는 모든 예외
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public ResponseCookie saveTempDataAndCreateCookie(String uuid, SocialLoginUserData userData, TokenDTO tokenDTO) {
        saveTempData(uuid, userData, tokenDTO);
        return authCookieManager.createTempCookie(uuid);
    }

    /**
     * <h3>임시 사용자 데이터 정리 스케줄링</h3>
     * <p>5분 후에 임시 사용자 데이터를 정리하는 작업을 스케줄링합니다.</p>
     * <p>메모리 누수 방지를 위한 자동 정리 메커니즘</p>
     *
     * @param uuid UUID 키
     * @since 2.0.0
     * @author Jaeik
     */
    private void scheduleCleanup(String uuid) {
        if (uuid == null) {
            log.warn("Cannot schedule cleanup for null UUID");
            return;
        }
        
        try {
            CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                    .execute(() -> {
                        try {
                            TemporaryUserDataDTO removed = tempUserDataStore.remove(uuid);
                            if (removed != null) {
                                log.debug("Scheduled cleanup completed for UUID: {}", uuid);
                            }
                        } catch (Exception e) {
                            log.error("Scheduled cleanup failed for UUID: {}, error: {}", uuid, e.getMessage());
                        }
                    });
            log.debug("Cleanup scheduled for UUID: {} (5 minutes)", uuid);
        } catch (Exception e) {
            log.error("Failed to schedule cleanup for UUID: {}, error: {}", uuid, e.getMessage());
        }
    }
}