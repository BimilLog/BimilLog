package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * <h2>사용자 데이터 임시 관리 클래스</h2>
 *
 * <p>신규 회원 가입시 사용자 데이터를 저장하고 관리한다.</p>
 * <p>로그인 API와 회원가입 API 사이의 정보 유지를 위해 도입한 메모리다.</p>
 * <p>UUID를 키로 사용하여 임시 데이터를 저장하며, 5분 후 자동으로 삭제된다</p>
 * <p>UUID는 사용자 브라우저에 쿠키로 전달된다.</p>
 *
 * @since 1.0.0
 * @author Jaeik
 */
@Component
public class TempUserDataManager {
    private final Map<String, TempUserData> tempUserDataStore = new ConcurrentHashMap<>();

    @Setter
    @Getter
    @AllArgsConstructor
    public static class TempUserData {
        private final SocialLoginUserData socialLoginUserData;
        private final TokenDTO tokenDTO;
        private final String fcmToken;

        public TempUserData(SocialLoginUserData socialLoginUserData, TokenDTO tokenDTO, String fcmToken) {
            this.socialLoginUserData = socialLoginUserData;
            this.tokenDTO = tokenDTO;
            this.fcmToken = fcmToken;
        }
    }

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     *
     * <p>신규 회원 가입시 사용자 데이터를 임시로 저장한다.</p>
     * <p>UUID를 키로 사용하여 데이터를 저장하며, 5분 후 자동으로 삭제된다.</p>
     *
     * @param socialLoginUserData 소셜 로그인 사용자 정보 DTO
     * @param tokenDTO 토큰 DTO
     * @param fcmToken FCM 토큰 (선택)
     * @return UUID 키
     * @since 1.0.0
     * @author Jaeik
     */
    public String saveTempData(SocialLoginUserData socialLoginUserData, TokenDTO tokenDTO, String fcmToken) {
        String uuid = UUID.randomUUID().toString();
        tempUserDataStore.put(uuid, new TempUserData(socialLoginUserData, tokenDTO, fcmToken));
        return uuid;
    }

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     *
     * <p>UUID를 사용하여 임시 사용자 데이터를 조회한다.</p>
     *
     * @param uuid UUID 키
     * @return TempUserData 임시 사용자 데이터
     * @since 1.0.0
     * @author Jaeik
     */
    public TempUserData getTempData(String uuid) {
        return tempUserDataStore.get(uuid);
    }

    /**
     * <h3>임시 사용자 데이터 삭제</h3>
     *
     * <p>UUID를 사용하여 임시 사용자 데이터를 삭제한다.</p>
     *
     * @param uuid UUID 키
     * @since 1.0.0
     * @author Jaeik
     */
    public void removeTempData(String uuid) {
        tempUserDataStore.remove(uuid);
    }

    /**
     * <h3>임시 사용자 데이터 정리 스케줄링</h3>
     *
     * <p>5분 후에 임시 사용자 데이터를 자동으로 삭제하도록 스케줄링한다.</p>
     *
     * @param uuid UUID 키
     * @since 1.0.0
     * @author Jaeik
     */
    private void scheduleCleanup(String uuid) {
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(() -> tempUserDataStore.remove(uuid));
    }
}
