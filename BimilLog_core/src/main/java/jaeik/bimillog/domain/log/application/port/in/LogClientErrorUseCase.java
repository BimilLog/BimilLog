package jaeik.bimillog.domain.log.application.port.in;

import jaeik.bimillog.in.log.web.dto.ClientErrorLogDTO;

/**
 * <h2>클라이언트 에러 로깅 유스케이스</h2>
 * <p>프론트엔드/안드로이드에서 발생한 에러를 로깅하는 비즈니스 로직 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LogClientErrorUseCase {

    /**
     * <h3>클라이언트 에러 로깅</h3>
     * <p>클라이언트에서 발생한 에러를 로그 파일에 기록합니다.</p>
     * <p>application.log에 [CLIENT-ERROR] 태그와 함께 기록되어 CloudWatch로 전송됩니다.</p>
     *
     * @param errorLog 클라이언트 에러 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void logClientError(ClientErrorLogDTO errorLog);
}
