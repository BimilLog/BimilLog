package jaeik.growfarm.dto.openai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 서버 상태 통합 분석 요청 DTO
 * 매트릭과 에러 로그를 함께 전송하여 통합 분석
 */
@Getter
@Setter
@RequiredArgsConstructor
public class ServerAnalysisRequestDTO {

    /**
     * 서버 매트릭 데이터
     * (CPU, 메모리, 디스크 사용량 등)
     */
    private String serverMetrics;

    /**
     * WARN 이상 수준의 에러 로그
     */
    private String errorLogs;
}