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


@Component
public class TempUserDataManager {
    private final ConcurrentHashMap<String, TempUserData> tempDataMap = new ConcurrentHashMap<>();

    @Setter
    @Getter
    @AllArgsConstructor
    public static class TempUserData {
        private TokenDTO tokenDTO;
        private KakaoInfoDTO kakaoInfoDTO;
        private LocalDateTime createdTime;
    }

    public String saveTempData(KakaoInfoDTO kakaoInfoDTO, TokenDTO tokenDTO) {
        String uuid = UUID.randomUUID().toString();
        TempUserData tempData = new TempUserData(tokenDTO, kakaoInfoDTO, LocalDateTime.now());
        tempDataMap.put(uuid, tempData);
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
