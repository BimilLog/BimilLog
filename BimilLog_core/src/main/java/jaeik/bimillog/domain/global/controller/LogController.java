package jaeik.bimillog.domain.global.controller;

import jaeik.bimillog.domain.global.dto.ClientErrorLogDTO;
import jaeik.bimillog.domain.global.service.LogService;
import jaeik.bimillog.infrastructure.log.Log;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>로그 컨트롤러</h2>
 * <p>클라이언트 에러 로깅을 담당하는 REST API 컨트롤러입니다.</p>
 * <p>프론트엔드/안드로이드에서 발생한 에러를 수집하여 로그 파일에 기록합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false)
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/global")
public class LogController {

    private final LogService logService;

    /**
     * <h3>클라이언트 에러 로깅</h3>
     * <p>프론트엔드/안드로이드에서 발생한 에러를 백엔드 로그에 기록합니다.</p>
     * <p>인증 불필요 (익명 사용자의 에러도 수집)</p>
     *
     * @param errorLog 클라이언트 에러 정보
     * @return ResponseEntity<Void> HTTP 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/client-error")
    public ResponseEntity<Void> logClientError(@Valid @RequestBody ClientErrorLogDTO errorLog) {
        log.info("클라이언트 에러 로그 수신: platform={}, message={}",
            errorLog.getPlatform(), errorLog.getErrorMessage());

        logService.logClientError(errorLog);

        return ResponseEntity.ok().build();
    }
}
