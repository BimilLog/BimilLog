package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
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
public class TempDataAdapter implements ManageTemporaryDataPort {

    private final Map<String, TemporaryUserDataDTO> tempUserDataStore = new ConcurrentHashMap<>();

    /**
     * <h3>임시 사용자 데이터를 저장</h3>
     *
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @param fcmToken FCM 토큰
     * @return UUID 키
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public String saveTempData(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        String uuid = UUID.randomUUID().toString();
        tempUserDataStore.put(uuid, new TemporaryUserDataDTO(userData, tokenDTO, fcmToken));
        scheduleCleanup(uuid);
        return uuid;
    }

    @Override
    public TemporaryUserDataDTO getTempData(String uuid) {
        return tempUserDataStore.get(uuid);
    }

    public void removeTempData(String uuid) {
        tempUserDataStore.remove(uuid);
    }

    private void scheduleCleanup(String uuid) {
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(() -> tempUserDataStore.remove(uuid));
    }
}