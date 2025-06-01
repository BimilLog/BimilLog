package jaeik.growfarm.util;

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
    private final ConcurrentHashMap<String, TempUserData> tempDataMap = new ConcurrentHashMap<>();

    @Setter
    @Getter
    @AllArgsConstructor
    public static class TempUserData {
        private TokenDTO tokenDTO;
        private KakaoInfoDTO kakaoInfoDTO;
        private String fcmToken;
        private LocalDateTime createdTime;
    }

    public String saveTempData(KakaoInfoDTO kakaoInfoDTO, TokenDTO tokenDTO, String fcmToken) {
        String uuid = UUID.randomUUID().toString();
        TempUserData tempUserData = new TempUserData(tokenDTO, kakaoInfoDTO, fcmToken, LocalDateTime.now());
        tempDataMap.put(uuid, tempUserData);
        scheduleCleanup(uuid);
        return uuid;
    }

    public TempUserData getTempData(String uuid) {
        return tempDataMap.get(uuid);
    }

    public void removeTempData(String uuid) {
        tempDataMap.remove(uuid);
    }

    private void scheduleCleanup(String uuid) {
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(() -> tempDataMap.remove(uuid));
    }
}
