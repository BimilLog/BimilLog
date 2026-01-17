package jaeik.bimillog.infrastructure.redis.member;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
public class RedisMemberDataAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TEMP_KEY_PREFIX = "temp:member:";
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
     *   <li>동일 UUID 재저장 시 덮어쓰기</li>
     *   <li>Redis TTL로 자동 만료 (5분)</li>
     * </ul>
     *
     * @param uuid 임시 사용자 식별 UUID 키
     * @param userProfile 소셜 사용자 프로필 (OAuth 액세스/리프레시 토큰 포함)
     * @throws CustomException UUID, userProfile이 유효하지 않은 경우
     */
    public void saveTempData(String uuid, SocialMemberProfile userProfile) {
        validateTempDataInputs(uuid, userProfile);

        executeRedisOperation(() -> {
            String key = buildTempKey(uuid);
            redisTemplate.opsForValue().set(key, userProfile, TTL);
            log.debug("UUID {}에 대한 임시 데이터가 Redis에 성공적으로 저장됨", uuid);
        }, uuid);
    }

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     * <p>UUID를 사용하여 Redis에서 임시 사용자 데이터를 조회합니다.</p>
     * <p>소셜 로그인 두 번째 단계(회원가입 페이지)에서 사용자가 입력한 닉네임과 함께 저장된 소셜 사용자 정보를 조회하기 위해 회원가입 플로우에서 호출합니다.</p>
     *
     * @param uuid 임시 사용자 식별 UUID 키
     * @return Optional<SocialMemberProfile> Optional로 감싼 소셜 사용자 프로필
     */
    public Optional<SocialMemberProfile> getTempData(String uuid) {
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


    /* ===================== Private Helpers ===================== */

    /**
     * <h3>임시 키 생성</h3>
     * <p>UUID를 사용하여 Redis 저장용 임시 키를 생성합니다.</p>
     * <p>임시 데이터 저장, 조회, 삭제 시 일관된 키 형식을 보장합니다.</p>
     *
     * @param uuid 사용자 식별 UUID
     * @return "temp:member:" 접두어가 붙은 Redis 키
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
     * @author Jaeik
     * @since 2.0.0
     */
    private void executeRedisOperation(Runnable operation, String uuid) {
        try {
            operation.run();
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 Redis {} 실패: {}", uuid, "저장", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_INVALID_USER_DATA);
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
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateTempDataInputs(String uuid, SocialMemberProfile userProfile) {
        if (isInvalidUuid(uuid)) {
            log.warn(NULL_UUID_MESSAGE, uuid);
            throw new CustomException(ErrorCode.AUTH_INVALID_TEMP_UUID);
        }
        if (userProfile == null) {
            log.warn(NULL_PROFILE_MESSAGE, uuid);
            throw new CustomException(ErrorCode.AUTH_INVALID_USER_DATA);
        }
        if (userProfile.getAccessToken() == null || userProfile.getRefreshToken() == null) {
            log.warn(NULL_TOKEN_MESSAGE, uuid);
            throw new CustomException(ErrorCode.AUTH_INVALID_TOKEN_DATA);
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
     * <p>Redis에서 조회한 Object 데이터를 도메인 모델인 SocialUserProfile로 변환합니다.</p>
     * <p>getTempData 메서드에서 Redis 조회 결과를 도메인 계층으로 전달하기 위해 호출됩니다.</p>
     *
     * @param uuid 작업 대상 UUID (로깅용)
     * @param data Redis에서 조회한 원시 데이터
     * @return 변환된 도메인 모델 (Optional로 래핑)
     * @author Jaeik
     * @since 2.0.0
     */
    private Optional<SocialMemberProfile> convertRedisDataToDomain(String uuid, Object data) {
        if (data == null) {
            log.debug("UUID {}에 대한 임시 데이터가 Redis에서 발견되지 않음", uuid);
            CacheMetricsLogger.miss(log, "auth:temp-member", uuid, "value_not_found");
            return Optional.empty();
        }

        try {
            SocialMemberProfile socialUserProfile = convertToSocialUserProfile(uuid, data);
            CacheMetricsLogger.hit(log, "auth:temp-member", uuid);
            return Optional.of(socialUserProfile);
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 변환 실패: {}", uuid, e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_INVALID_TEMP_DATA);
        }
    }

    /**
     * <h3>Object를 SocialUserProfile로 변환</h3>
     * <p>Redis에서 조회한 Object를 예상되는 타입별로 처리하여 SocialUserProfile로 변환합니다.</p>
     * <p>convertRedisDataToDomain 메서드에서 타입 안전 변환을 위해 호출됩니다.</p>
     *
     * @param uuid 작업 대상 UUID (로깅용)
     * @param data Redis에서 조회한 원시 데이터 (SocialMemberProfile 또는 LinkedHashMap)
     * @return 변환된 SocialMemberProfile 객체
     * @author Jaeik
     * @since 2.0.0
     */
    private SocialMemberProfile convertToSocialUserProfile(String uuid, Object data) {
        return switch (data) {
            case SocialMemberProfile profile -> profile;
            case Map<?, ?> map -> {
                log.debug("UUID {}에 대해 LinkedHashMap 타입 데이터를 SocialUserProfile로 변환 시도", uuid);
                yield convertMapToSocialUserProfile(map);
            }
            default -> {
                log.warn("UUID {}에 대해 조회된 데이터가 예상 타입이 아님, 실제: {}", uuid, data.getClass().getSimpleName());
                throw new CustomException(ErrorCode.AUTH_INVALID_TEMP_DATA);
            }
        };
    }

    /**
     * <h3>Map을 SocialUserProfile로 변환</h3>
     * <p>Redis에서 LinkedHashMap 형태로 역직렬화된 데이터를 SocialUserProfile로 변환합니다.</p>
     * <p>convertToSocialUserProfile 메서드에서 Map 타입 데이터에 대한 변환 처리를 위해 호출됩니다.</p>
     *
     * @param map LinkedHashMap 형태의 Redis 데이터
     * @return 변환된 SocialMemberProfile 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @SuppressWarnings("unchecked")
    private SocialMemberProfile convertMapToSocialUserProfile(Map<?, ?> map) {
        try {
            String socialId = (String) map.get("socialId");
            String email = (String) map.get("email");
            String provider = (String) map.get("provider");
            String nickname = (String) map.get("nickname");
            String profileImageUrl = (String) map.get("profileImageUrl");
            String kakaoAccessToken = (String) map.get("kakaoAccessToken");
            String kakaoRefreshToken = (String) map.get("kakaoRefreshToken");

            return new SocialMemberProfile(
                    socialId,
                    email,
                    SocialProvider.valueOf(provider),
                    nickname,
                    profileImageUrl,
                    kakaoAccessToken,
                    kakaoRefreshToken
            );
        } catch (Exception e) {
            log.error("LinkedHashMap -> SocialMemberProfile 변환 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTH_INVALID_TEMP_DATA);
        }
    }

}
