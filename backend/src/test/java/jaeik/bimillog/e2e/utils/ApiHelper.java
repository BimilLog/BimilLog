package jaeik.bimillog.e2e.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * API 직접 호출 헬퍼
 * UI를 거치지 않고 백엔드 API를 직접 호출하여 테스트 데이터 준비 및 정리
 */
public class ApiHelper {

    private final APIRequestContext apiContext;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private String authToken;

    public ApiHelper(Playwright playwright, String baseUrl) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
        this.apiContext = playwright.request().newContext(
            new APIRequest.NewContextOptions()
                .setBaseURL(baseUrl)
                .setIgnoreHTTPSErrors(true)
        );
    }

    /**
     * JWT 토큰 설정
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * 로그인 API 호출
     */
    public APIResponse login(String kakaoAccessToken) {
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", kakaoAccessToken);

        return post("/api/auth/login", data);
    }

    /**
     * GET 요청
     */
    public APIResponse get(String endpoint) {
        RequestOptions options = RequestOptions.create();
        if (authToken != null) {
            options.setHeader("Authorization", "Bearer " + authToken);
        }
        return apiContext.get(endpoint, options);
    }

    /**
     * POST 요청
     */
    public APIResponse post(String endpoint, Object data) {
        RequestOptions options = RequestOptions.create();
        if (authToken != null) {
            options.setHeader("Authorization", "Bearer " + authToken);
        }
        options.setHeader("Content-Type", "application/json");
        options.setData(data);
        return apiContext.post(endpoint, options);
    }

    /**
     * PUT 요청
     */
    public APIResponse put(String endpoint, Object data) {
        RequestOptions options = RequestOptions.create();
        if (authToken != null) {
            options.setHeader("Authorization", "Bearer " + authToken);
        }
        options.setHeader("Content-Type", "application/json");
        options.setData(data);
        return apiContext.put(endpoint, options);
    }

    /**
     * DELETE 요청
     */
    public APIResponse delete(String endpoint) {
        RequestOptions options = RequestOptions.create();
        if (authToken != null) {
            options.setHeader("Authorization", "Bearer " + authToken);
        }
        return apiContext.delete(endpoint, options);
    }

    /**
     * 파일 업로드
     */
    public APIResponse uploadFile(String endpoint, String filePath, String fieldName) {
        RequestOptions options = RequestOptions.create();
        if (authToken != null) {
            options.setHeader("Authorization", "Bearer " + authToken);
        }
        options.setMultipart(com.microsoft.playwright.options.FormData.create()
            .set(fieldName, java.nio.file.Paths.get(filePath)));
        return apiContext.post(endpoint, options);
    }

    // ===== 테스트 데이터 생성 헬퍼 메서드 =====

    /**
     * 테스트 사용자 생성
     */
    public APIResponse createTestUser(String nickname) {
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", nickname);
        data.put("email", TestDataGenerator.generateEmail());
        return post("/api/auth/signup", data);
    }

    /**
     * 테스트 게시글 생성
     */
    public APIResponse createTestPost(String title, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title != null ? title : TestDataGenerator.generatePostTitle());
        data.put("content", content != null ? content : TestDataGenerator.generatePostContent());
        return post("/api/post", data);
    }

    /**
     * 테스트 댓글 생성
     */
    public APIResponse createTestComment(Long postId, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("content", content != null ? content : TestDataGenerator.generateCommentContent());
        return post("/api/comment/write", data);
    }

    /**
     * 테스트 롤링페이퍼 메시지 생성
     */
    public APIResponse createTestRollingPaperMessage(String userName, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message != null ? message : TestDataGenerator.generateRollingPaperMessage());
        data.put("author", "익명");
        data.put("password", TestDataGenerator.generatePassword());
        return post("/api/paper/" + userName, data);
    }

    // ===== 테스트 데이터 정리 헬퍼 메서드 =====

    /**
     * 테스트 게시글 삭제
     */
    public APIResponse deleteTestPost(Long postId) {
        return delete("/api/post/" + postId);
    }

    /**
     * 테스트 댓글 삭제
     */
    public APIResponse deleteTestComment(Long commentId) {
        Map<String, Object> data = new HashMap<>();
        data.put("commentId", commentId);
        return post("/api/comment/delete", data);
    }

    /**
     * 테스트 사용자 탈퇴
     */
    public APIResponse deleteTestUser() {
        return delete("/api/user/withdraw");
    }

    // ===== 상태 확인 헬퍼 메서드 =====

    /**
     * 서버 상태 확인
     */
    public boolean isServerHealthy() {
        try {
            APIResponse response = get("/actuator/health");
            return response.status() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 응답 JSON 파싱
     */
    public <T> T parseResponse(APIResponse response, Class<T> clazz) {
        try {
            return objectMapper.readValue(response.text(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }

    /**
     * 응답 상태 확인
     */
    public void assertResponseOk(APIResponse response) {
        if (response.status() < 200 || response.status() >= 300) {
            throw new AssertionError("API request failed with status: " + response.status() +
                "\nResponse: " + response.text());
        }
    }

    /**
     * 정리
     */
    public void dispose() {
        if (apiContext != null) {
            apiContext.dispose();
        }
    }
}