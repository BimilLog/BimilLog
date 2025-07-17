package jaeik.growfarm.controller;

import jaeik.growfarm.dto.openai.ServerAnalysisRequestDTO;
import jaeik.growfarm.service.openai.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class OpenAiController {

    private final OpenAiService openAiService;

    /**
     * 서버 상태 통합 분석 엔드포인트
     * 서버 매트릭과 에러 로그를 통합 분석하여 Slack으로 전송
     * 
     * @param request 서버 분석 요청 (매트릭 + 에러로그)
     * @return 분석 결과
     */
    @PostMapping("/analyze-server")
    public ResponseEntity<String> analyzeServer(@RequestBody ServerAnalysisRequestDTO request) {
        try {
            String analysis = openAiService.analyzeServerStatusAndSendToSlack(
                    request.getServerMetrics(),
                    request.getErrorLogs());
            return ResponseEntity.ok(
                    "✅ 서버 상태 통합 분석이 완료되어 Slack으로 전송되었습니다.\n\n🔍 분석 결과:\n" + analysis);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ 서버 상태 분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
