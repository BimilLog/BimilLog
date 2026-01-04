package jaeik.bimillog.infrastructure.api.fcm;

import jaeik.bimillog.infrastructure.api.dto.FcmMessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * <h2>FCM API Feign Client</h2>
 * <p>Firebase Cloud Messaging API 호출을 담당하는 Feign Client입니다.</p>
 * <p>푸시 알림 메시지 전송</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@FeignClient(name = "fcm-api", url = "https://fcm.googleapis.com")
public interface FcmApiClient {

    /**
     * <h3>FCM 메시지 전송</h3>
     * <p>Firebase Cloud Messaging API를 통해 푸시 알림을 전송합니다.</p>
     *
     * @param authorization Bearer 토큰 (형식: "Bearer {accessToken}")
     * @param contentType   Content-Type 헤더 (application/json)
     * @param message       FCM 메시지 DTO
     */
    @PostMapping(value = "/v1/projects/growfarm-6cd79/messages:send", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    void sendMessage(@RequestHeader("Authorization") String authorization,
                      @RequestHeader("Content-Type") String contentType,
                      @RequestBody FcmMessageDTO message);
}