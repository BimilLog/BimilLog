package jaeik.bimillog.infrastructure.adapter.out.redis;

import jaeik.bimillog.domain.user.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.entity.TempUserData;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthCookieManager;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>Redis 사용자 데이터 어댑터</h2>
 * <p>Redis를 사용하여 소셜 로그인 과정에서의 임시 사용자 데이터 관리를 담당합니다.</p>
 * <p>임시 데이터 저장/조회/삭제, 임시 쿠키 생성 기능을 구현합니다.</p>
 * <p>소셜 프로필 정보와 OAuth 토큰(액세스/리프레시)을 5분간 임시 보관합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUserDataAdapter implements RedisUserDataPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthCookieManager authCookieManager;

    private static final String TEMP_KEY_PREFIX = "temp:user:";
    private static final Duration TTL = Duration.ofMinutes(5);
    
    private static final String NULL_UUID_MESSAGE = "유효하지 않은 임시 UUID 제공됨: {}";
    private static final String NULL_PROFILE_MESSAGE = "UUID {}에 대해 유효하지 않은 사용자 프로필 제공됨";
    private static final String NULL_TOKEN_MESSAGE = "UUID {}에 대해 유효하지 않은 토큰 데이터 제공됨";

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     * <p>소셜 로그인 첫 단계에서 획득한 사용자 프로필 정보와 OAuth 토큰을 Redis에 임시 저장합니다.</p>
     * <p>소셜 로그인 인증 완료 후 회원가입 페이지로 리다이렉트되기 전에 사용자 데이터를 보관하기 위해 소셜 로그인 플로우에서 호출합니다.</p>
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>UUID는 필수 (null, 빈 문자열 불허)</li>
     *   <li>userProfile는 필수 (null 불허, OAuth 토큰 포함)</li>
     *   <li>fcmToken은 선택적 (null 허용)</li>
     *   <li>동일 UUID 재저장 시 덮어쓰기</li>
     *   <li>Redis TTL로 자동 만료 (5분)</li>
     * </ul>
     *
     * @param uuid 임시 사용자 식별 UUID 키
     * @param userProfile 소셜 사용자 프로필 (OAuth 액세스/리프레시 토큰 포함)
     * @param fcmToken FCM 토큰 (선택적)
     * @throws CustomException UUID, userProfile이 유효하지 않은 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void saveTempData(String uuid, SocialUserProfile userProfile, String fcmToken) {
        validateTempDataInputs(uuid, userProfile);

        executeRedisOperation(() -> {
            String key = buildTempKey(uuid);
            TempUserData tempData = TempUserData.from(userProfile, fcmToken);
            redisTemplate.opsForValue().set(key, tempData, TTL);
            log.debug("UUID {}에 대한 임시 데이터가 Redis에 성공적으로 저장됨", uuid);
        }, uuid);
    }

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     * <p>UUID를 사용하여 Redis에서 임시 사용자 데이터를 조회합니다.</p>
     * <p>소셜 로그인 두 번째 단계(회원가입 페이지)에서 사용자가 입력한 닉네임과 함께 저장된 소셜 사용자 정보를 조회하기 위해 회원가입 플로우에서 호출합니다.</p>
     *
     * @param uuid 임시 사용자 식별 UUID 키
     * @return Optional<TempUserData> Optional로 감싼 임시 사용자 데이터
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<TempUserData> getTempData(String uuid) {
        if (uuid == null) {
            log.debug("임시 데이터 조회에 null UUID 제공됨, 빈 결과 반환");
            return Optional.empty();
        }

        try {
            String key = buildTempKey(uuid);
            Object data = redisTemplate.opsForValue().get(key);
            return convertRedisDataToDomain(uuid, data);
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 Redis 조회 실패: {}", uuid, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * <h3>임시 사용자 데이터 삭제</h3>
     * <p>UUID를 사용하여 Redis에서 임시 사용자 데이터를 삭제합니다.</p>
     * <p>소셜 로그인 회원가입 완료 후 임시 데이터를 정리하기 위해 회원가입 성공 플로우에서 호출합니다.</p>
     *
     * @param uuid 삭제할 임시 사용자 식별 UUID 키
     * @author Jaeik
     * @since 2.0.0
     */
    public void removeTempData(String uuid) {
        if (uuid == null) {
            log.debug("임시 데이터 제거에 null UUID 제공됨, 무시");
            return;
        }

        executeRedisOperationSafely(() -> {
            Boolean deleted = redisTemplate.delete(buildTempKey(uuid));
            log.debug(deleted
                    ? "UUID {}에 대한 임시 데이터가 Redis에서 성공적으로 제거됨"
                    : "UUID {}에 대해 Redis에서 제거할 임시 데이터가 발견되지 않음", uuid);
        }, uuid);
    }

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     * <p>소셜 로그인 첫 단계 완료 후 사용자를 회원가입 페이지로 리다이렉트할 때 임시 UUID를 담는 쿠키를 생성합니다.</p>
     * <p>소셜 로그인 인증 성공 후 임시 데이터 연결을 위해 소셜 로그인 플로우에서 호출합니다.</p>
     *
     * @param uuid 임시 사용자 식별 UUID
     * @return ResponseCookie 임시 사용자 ID 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public ResponseCookie createTempCookie(String uuid) {
        return authCookieManager.createTempCookie(uuid);
    }

    /* ===================== Private Helpers ===================== */

    /**
     * <h3>임시 키 생성</h3>
     * <p>UUID를 사용하여 Redis 저장용 임시 키를 생성합니다.</p>
     * <p>임시 데이터 저장, 조회, 삭제 시 일관된 키 형식을 보장합니다.</p>
     *
     * @param uuid 사용자 식별 UUID
     * @return "temp:user:" 접두어가 붙은 Redis 키
     * @author Jaeik
     * @since 2.0.0
     */
    private String buildTempKey(String uuid) {
        return TEMP_KEY_PREFIX + uuid;
    }

    /**
     * <h3>Redis 작업 실행</h3>
     * <p>Redis 작업을 실행하고 실패 시 AuthCustomException을 발생시킵니다.</p>
     * <p>임시 데이터 저장 작업에서 예외 발생 시 사용자에게 적절한 오류 응답을 제공하기 위해 사용됩니다.</p>
     *
     * @param operation 실행할 Redis 작업
     * @param uuid 작업 대상 UUID (로깅용)
     * @throws AuthCustomException Redis 작업 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    private void executeRedisOperation(Runnable operation, String uuid) {
        try {
            operation.run();
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 Redis {} 실패: {}", uuid, "저장", e.getMessage(), e);
            throw new AuthCustomException(AuthErrorCode.INVALID_USER_DATA);
        }
    }

    /**
     * <h3>안전한 Redis 작업 실행</h3>
     * <p>Redis 작업을 실행하되 예외 발생 시에도 프로그램이 중단되지 않도록 합니다.</p>
     * <p>임시 데이터 삭제처럼 실패해도 전체 플로우에 영향을 주지 않아야 하는 작업에 사용됩니다.</p>
     *
     * @param operation 실행할 Redis 작업
     * @param uuid 작업 대상 UUID (로깅용)
     * @author Jaeik
     * @since 2.0.0
     */
    private void executeRedisOperationSafely(Runnable operation, String uuid) {
        try {
            operation.run();
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 Redis {} 실패: {}", uuid, "제거", e.getMessage(), e);
        }
    }

    /**
     * <h3>임시 데이터 입력 검증</h3>
     * <p>임시 데이터 저장 시 필요한 모든 파라미터의 유효성을 검증합니다.</p>
     * <p>saveTempData 메서드에서 Redis 저장 전 데이터 무결성을 보장하기 위해 호출됩니다.</p>
     *
     * @param uuid 임시 사용자 식별 UUID
     * @param userProfile 소셜 사용자 프로필 (토큰 포함)
     * @throws AuthCustomException 유효하지 않은 데이터가 있는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateTempDataInputs(String uuid, SocialUserProfile userProfile) {
        if (isInvalidUuid(uuid)) {
            log.warn(NULL_UUID_MESSAGE, uuid);
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_UUID);
        }
        if (userProfile == null) {
            log.warn(NULL_PROFILE_MESSAGE, uuid);
            throw new AuthCustomException(AuthErrorCode.INVALID_USER_DATA);
        }
        if (userProfile.token() == null) {
            log.warn(NULL_TOKEN_MESSAGE, uuid);
            throw new AuthCustomException(AuthErrorCode.INVALID_TOKEN_DATA);
        }
    }

    /**
     * <h3>UUID 유효성 검사</h3>
     * <p>UUID가 null이거나 공백 문자열인지 확인합니다.</p>
     * <p>validateTempDataInputs 메서드에서 UUID 파라미터 검증을 위해 호출됩니다.</p>
     *
     * @param uuid 검사할 UUID 문자열
     * @return UUID가 유효하지 않으면 true, 유효하면 false
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean isInvalidUuid(String uuid) {
        return uuid == null || uuid.trim().isEmpty();
    }

    /**
     * <h3>Redis 데이터를 도메인 모델로 변환</h3>
     * <p>Redis에서 조회한 Object 데이터를 도메인 모델인 TempUserData로 변환합니다.</p>
     * <p>getTempData 메서드에서 Redis 조회 결과를 도메인 계층으로 전달하기 위해 호출됩니다.</p>
     *
     * @param uuid 작업 대상 UUID (로깅용)
     * @param data Redis에서 조회한 원시 데이터
     * @return 변환된 도메인 모델 (Optional로 래핑)
     * @throws AuthCustomException 데이터 변환 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    private Optional<TempUserData> convertRedisDataToDomain(String uuid, Object data) {
        if (data == null) {
            log.debug("UUID {}에 대한 임시 데이터가 Redis에서 발견되지 않음", uuid);
            return Optional.empty();
        }

        try {
            TempUserData tempUserData = convertToTempUserData(uuid, data);
            return Optional.of(tempUserData);
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 변환 실패: {}", uuid, e.getMessage(), e);
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }
    }

    /**
     * <h3>Object를 TempUserData로 변환</h3>
     * <p>Redis에서 조회한 Object를 예상되는 타입별로 처리하여 TempUserData로 변환합니다.</p>
     * <p>convertRedisDataToDomain 메서드에서 타입 안전 변환을 위해 호출됩니다.</p>
     *
     * @param uuid 작업 대상 UUID (로깅용)
     * @param data Redis에서 조회한 원시 데이터 (TempUserData 또는 LinkedHashMap)
     * @return 변환된 TempUserData 객체
     * @throws AuthCustomException 예상되지 않은 타입이거나 변환 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    private TempUserData convertToTempUserData(String uuid, Object data) {
        return switch (data) {
            case TempUserData tempData -> tempData;
            case Map<?, ?> map -> {
                log.debug("UUID {}에 대해 LinkedHashMap 타입 데이터를 TempUserData로 변환 시도", uuid);
                yield convertMapToTempUserData(map);
            }
            default -> {
                log.warn("UUID {}에 대해 조회된 데이터가 예상 타입이 아님, 실제: {}", uuid, data.getClass().getSimpleName());
                throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
            }
        };
    }

    /**
     * <h3>Map을 TempUserData로 변환</h3>
     * <p>Redis에서 LinkedHashMap 형태로 역직렬화된 데이터를 TempUserData로 변환합니다.</p>
     * <p>convertToTempUserData 메서드에서 Map 타입 데이터에 대한 변환 처리를 위해 호출됩니다.</p>
     *
     * @param map LinkedHashMap 형태의 Redis 데이터
     * @return 변환된 TempUserData 객체
     * @throws AuthCustomException Map 구조 파싱이나 변환 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    @SuppressWarnings("unchecked")
    private TempUserData convertMapToTempUserData(Map<?, ?> map) {
        try {
            String fcmToken = (String) map.get("fcmToken");

            // 새로운 형식: socialUserProfile 객체가 직접 저장된 경우
            if (map.containsKey("socialUserProfile")) {
                Map<String, Object> profileData = (Map<String, Object>) map.get("socialUserProfile");

                String socialId = (String) profileData.get("socialId");
                String email = (String) profileData.get("email");
                String provider = (String) profileData.get("provider");
                String nickname = (String) profileData.get("nickname");
                String profileImageUrl = (String) profileData.get("profileImageUrl");

                Token token = null;
                if (profileData.containsKey("token")) {
                    token = extractTokenFromMap((Map<String, Object>) profileData.get("token"));
                }

                SocialUserProfile profile = new SocialUserProfile(
                    socialId,
                    email,
                    SocialProvider.valueOf(provider),
                    nickname,
                    profileImageUrl,
                    token
                );

                return new TempUserData(profile, fcmToken);
            }

            // 이전 형식과의 호환성: 필드가 직접 저장된 경우
            Token token = extractTokenFromMap((Map<String, Object>) map.get("token"));
            String socialId = (String) map.get("socialId");
            String email = (String) map.get("email");
            String provider = (String) map.get("provider");
            String nickname = (String) map.get("nickname");
            String profileImageUrl = (String) map.get("profileImageUrl");

            SocialUserProfile profile = new SocialUserProfile(
                socialId,
                email,
                SocialProvider.valueOf(provider),
                nickname,
                profileImageUrl,
                token
            );

            return new TempUserData(profile, fcmToken);
        } catch (Exception e) {
            log.error("LinkedHashMap -> TempUserData 변환 실패: {}", e.getMessage(), e);
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }
    }

    /**
     * <h3>Map에서 Token 추출</h3>
     * <p>LinkedHashMap 형태의 토큰 데이터에서 Token 도메인 객체를 생성합니다.</p>
     * <p>convertMapToDTO 메서드에서 중첩된 토큰 데이터 추출을 위해 호출됩니다.</p>
     *
     * @param tokenMap 토큰 정보가 담긴 Map
     * @return 추출된 Token 도메인 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private Token extractTokenFromMap(Map<String, Object> tokenMap) {
        return Token.createTemporaryToken(
                (String) tokenMap.get("accessToken"),
                (String) tokenMap.get("refreshToken")
        );
    }

}
