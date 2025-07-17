package jaeik.growfarm.service.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String openAiApiKey; // OpenAI API 키

    @Value("${slack.webhook}")
    private String slackWebhookUrl; // Slack Webhook URL

    // 통합 서버 분석용 프롬프트 템플릿
    private static final String SERVER_ANALYSIS_PROMPT_TEMPLATE = """
            당신은 서버 시스템 운영 전문가입니다.
            다음 서버 매트릭과 에러 로그를 종합적으로 분석하여 현재 서버 상태를 진단하고 조치 방안을 제시해주세요.

            [서버 매트릭]
            %s

            [에러 로그 (WARN 이상)]
            %s

            [출력 형식]
            🖥️ **서버 상태 진단**:

            ⚠️ **주요 이슈**:

            📊 **성능 지표 요약**:

            🛠️ **우선순위별 조치 방안**:
            1. **긴급**:
            2. **중요**:
            3. **일반**:

            💡 **추가 모니터링 권장사항**:
            """;

    /**
     * 서버 상태를 통합 분석하고 Slack으로 전송하는 메서드
     * 매트릭과 에러 로그를 함께 분석하여 전체적인 서버 상태 진단
     * 
     * @param serverMetrics 서버 매트릭 데이터
     * @param errorLogs     WARN 이상 에러 로그
     * @return GPT 통합 분석 결과
     */
    public String analyzeServerStatusAndSendToSlack(String serverMetrics, String errorLogs) {
        String combinedPrompt = String.format(SERVER_ANALYSIS_PROMPT_TEMPLATE,
                serverMetrics != null ? serverMetrics : "매트릭 데이터 없음",
                errorLogs != null ? errorLogs : "에러 로그 없음");

        String analysis = getGptResponse(combinedPrompt);
        sendToSlack("🚨 **서버 상태 통합 분석 결과** 🚨\n\n" + analysis);
        return analysis;
    }

    /**
     * GPT로부터 응답을 받는 내부 메서드
     * 
     * @param userMessage 사용자 메시지
     * @return GPT 응답 텍스트
     */
    private String getGptResponse(String userMessage) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(openAiApiKey)
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("gpt-4o-mini")
                .addUserMessage(userMessage)
                .temperature(0.3) // 분석 작업이므로 더 deterministic하게 설정
                .maxTokens(1500) // 분석 결과가 길어질 수 있으므로 토큰 수 증가
                .build();

        ChatCompletion chatCompletion = client.chat().completions().create(params);
        return chatCompletion.choices().getFirst().message().content().orElse("응답을 받을 수 없습니다.");
    }

    /**
     * Slack으로 메시지를 전송하는 메서드
     * 
     * @param message 전송할 메시지
     */
    private void sendToSlack(String message) {
        String payload = String.format("{ \"text\": \"%s\" }", escapeJson(message));

        try {
            URL url = new URL(slackWebhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Slack 전송 성공!");
            } else {
                System.out.println("Slack 전송 실패: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Slack 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * JSON 문자열 이스케이프 처리
     * 
     * @param input 이스케이프할 문자열
     * @return 이스케이프된 문자열
     */
    private String escapeJson(String input) {
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
