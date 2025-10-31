package jaeik.bimillog.domain.log.application.service;

import jaeik.bimillog.domain.log.application.port.in.LogClientErrorUseCase;
import jaeik.bimillog.infrastructure.adapter.in.log.web.dto.ClientErrorLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <h2>로깅 서비스</h2>
 * <p>클라이언트 에러 로깅 비즈니스 로직을 구현한 서비스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService implements LogClientErrorUseCase {

    /**
     * <h3>클라이언트 에러 로깅</h3>
     * <p>클라이언트에서 발생한 에러를 구조화된 형식으로 로그에 기록합니다.</p>
     * <p>로그 형식: [CLIENT-ERROR] [플랫폼] 에러 메시지 | URL | 스택 트레이스</p>
     *
     * @param errorLog 클라이언트 에러 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logClientError(ClientErrorLogDTO errorLog) {
        // 구조화된 로그 메시지 생성
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[CLIENT-ERROR] ");
        logMessage.append("[").append(errorLog.getPlatform()).append("] ");
        logMessage.append(errorLog.getErrorMessage());

        if (errorLog.getUrl() != null && !errorLog.getUrl().isEmpty()) {
            logMessage.append(" | URL: ").append(errorLog.getUrl());
        }

        if (errorLog.getUserAgent() != null && !errorLog.getUserAgent().isEmpty()) {
            logMessage.append(" | UserAgent: ").append(errorLog.getUserAgent());
        }

        if (errorLog.getAdditionalInfo() != null && !errorLog.getAdditionalInfo().isEmpty()) {
            logMessage.append(" | Additional: ").append(errorLog.getAdditionalInfo());
        }

        // 에러 로그 기록
        log.error(logMessage.toString());

        // 스택 트레이스가 있는 경우 별도로 기록
        if (errorLog.getStackTrace() != null && !errorLog.getStackTrace().isEmpty()) {
            log.error("[CLIENT-ERROR-STACKTRACE] [{}] {}",
                errorLog.getPlatform(),
                errorLog.getStackTrace());
        }
    }
}
