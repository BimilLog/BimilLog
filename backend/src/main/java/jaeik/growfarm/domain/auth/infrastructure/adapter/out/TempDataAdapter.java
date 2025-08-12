package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.dto.user.TokenDTO;
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
public class TempDataAdapter implements ManageTemporaryDataPort {

    private final Map<String, TemporaryUserDataDTO> tempUserDataStore = new ConcurrentHashMap<>();

    /**
     * <h3>임시 사용자 데이터를 저장</h3>
     *
     * @param uuid UUID 키
     * @param userData 소셜 로그인 사용자 정보
     * @param tokenDTO 토큰 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void saveTempData(String uuid, SocialLoginUserData userData, TokenDTO tokenDTO) {
        // fcmToken은 TemporaryUserDataDTO 생성 시에 전달. saveTempData 메서드 인자에서 제거.
        tempUserDataStore.put(uuid, new TemporaryUserDataDTO(userData, tokenDTO, userData.getFcmToken())); // fcmToken은 userData에서 가져옴
        scheduleCleanup(uuid);
    }

    @Override
    public Optional<TemporaryUserDataDTO> getTempData(String uuid) {
        return Optional.ofNullable(tempUserDataStore.get(uuid));
    }

    public void removeTempData(String uuid) {
        tempUserDataStore.remove(uuid);
    }

    private void scheduleCleanup(String uuid) {
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(() -> tempUserDataStore.remove(uuid));
    }
}