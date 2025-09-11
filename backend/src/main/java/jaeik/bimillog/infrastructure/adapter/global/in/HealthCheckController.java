package jaeik.bimillog.infrastructure.adapter.global.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/global")
public class HealthCheckController {

    /**
     * <h3>서버 상태 검사 API</h3>
     *
     * @return 상태 검사 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
