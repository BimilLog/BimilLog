package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import lombok.RequiredArgsConstructor;
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
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class TempDataAdapter implements ManageTemporaryDataPort {

    private final Map<String, TemporaryUserDataDTO> tempUserDataStore = new ConcurrentHashMap<>();
    private final AuthCookieManager authCookieManager;

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     * <p>소셜 로그인 사용자 정보를 임시 데이터로 저장합니다.</p>
     *
     * @param uuid UUID 키
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void saveTempData(String uuid, SocialLoginUserData userData, TokenDTO tokenDTO) {
        // fcmToken은 TemporaryUserDataDTO 생성 시에 전달.
        tempUserDataStore.put(uuid, new TemporaryUserDataDTO(userData, tokenDTO, userData.fcmToken())); // fcmToken은 userData에서 가져옴
        scheduleCleanup(uuid);
    }

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     * <p>UUID를 사용하여 임시 사용자 데이터를 조회합니다.</p>
     *
     * @param uuid UUID 키
     * @return Optional로 감싼 임시 사용자 데이터
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Optional<TemporaryUserDataDTO> getTempData(String uuid) {
        return Optional.ofNullable(tempUserDataStore.get(uuid));
    }

    /**
     * <h3>임시 사용자 데이터 삭제</h3>
     * <p>UUID를 사용하여 임시 사용자 데이터를 삭제합니다.</p>
     *
     * @param uuid UUID 키
     * @since 2.0.0
     * @author Jaeik
     */
    public void removeTempData(String uuid) {
        tempUserDataStore.remove(uuid);
    }

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     * <p>신규 회원가입 시 사용자의 임시 UUID를 담는 쿠키를 생성</p>
     *
     * @param uuid 임시 사용자 ID
     * @return 임시 사용자 ID 쿠키
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public ResponseCookie createTempCookie(String uuid) {
        return authCookieManager.createTempCookie(uuid);
    }

    /**
     * <h3>임시 사용자 데이터 정리 스케줄링</h3>
     * <p>5분 후에 임시 사용자 데이터를 정리하는 작업을 스케줄링합니다.</p>
     *
     * @param uuid UUID 키
     * @since 2.0.0
     * @author Jaeik
     */
    private void scheduleCleanup(String uuid) {
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(() -> tempUserDataStore.remove(uuid));
    }
}