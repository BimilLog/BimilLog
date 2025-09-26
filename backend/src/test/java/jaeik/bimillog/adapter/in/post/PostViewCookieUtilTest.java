package jaeik.bimillog.adapter.in.post;

import jaeik.bimillog.infrastructure.adapter.in.post.web.util.PostViewCookieUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostViewCookieUtil 테스트</h2>
 * <p>게시글 조회 쿠키 유틸리티의 중복 검증 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostViewCookieUtil 테스트")
@Tag("fast")
class PostViewCookieUtilTest {

    @InjectMocks
    private PostViewCookieUtil postViewCookieUtil;

    private Cookie[] cookies;

    @BeforeEach
    void setUp() {
        // 기본 쿠키 배열 설정
        cookies = new Cookie[]{
                new Cookie("other_cookie", "other_value"),
                new Cookie("post_views", "123_456_789")
        };
    }

    @Test
    @DisplayName("조회 여부 확인 - 처음 조회하는 게시글")
    void shouldReturnFalse_WhenPostNotViewed() {
        // Given
        Long postId = 999L;

        // When
        boolean result = postViewCookieUtil.hasViewed(cookies, postId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("조회 여부 확인 - 이미 조회한 게시글")
    void shouldReturnTrue_WhenPostAlreadyViewed() {
        // Given
        Long postId = 123L;

        // When
        boolean result = postViewCookieUtil.hasViewed(cookies, postId);

        // Then
        assertThat(result).isTrue();
    }


    @Test
    @DisplayName("쿠키 생성 - 처음 조회하는 게시글")
    void shouldCreateCookieWithPostId_WhenFirstTimeViewing() {
        // Given
        Long postId = 999L;
        Cookie[] emptyCookies = new Cookie[]{new Cookie("other_cookie", "other_value")};

        // When
        Cookie result = postViewCookieUtil.createViewCookie(emptyCookies, postId);

        // Then
        assertThat(result.getName()).isEqualTo("post_views");
        assertThat(result.getValue()).isEqualTo("999");
        assertThat(result.getMaxAge()).isEqualTo(24 * 60 * 60); // 24시간
        assertThat(result.getPath()).isEqualTo("/");
        assertThat(result.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("쿠키 생성 - 기존 조회 이력에 새 게시글 추가")
    void shouldAppendPostIdToCookie_WhenAddingNewPost() {
        // Given
        Long postId = 999L;

        // When
        Cookie result = postViewCookieUtil.createViewCookie(cookies, postId);

        // Then
        assertThat(result.getName()).isEqualTo("post_views");
        assertThat(result.getValue()).contains("123", "456", "789", "999");
        String[] viewIds = result.getValue().split("_");
        assertThat(viewIds).hasSize(4);
    }

    @Test
    @DisplayName("쿠키 생성 - 100개 제한 테스트")
    void shouldLimitViewHistoryTo100_WhenExceedingLimit() {
        // Given
        Long postId = 999L;
        
        // 이미 100개의 ID가 있는 쿠키 생성
        StringBuilder existingViews = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            if (i > 1) existingViews.append("_");
            existingViews.append(i);
        }
        Cookie[] fullCookies = new Cookie[]{new Cookie("post_views", existingViews.toString())};

        // When
        Cookie result = postViewCookieUtil.createViewCookie(fullCookies, postId);

        // Then
        String[] viewIds = result.getValue().split("_");
        assertThat(viewIds.length).isLessThanOrEqualTo(100);
        assertThat(result.getValue()).contains("999"); // 새로운 ID는 포함되어야 함
        
        // 101개가 되었으므로 맨 앞의 "1"이 제거되어야 함
        assertThat(result.getValue()).startsWith("2"); // "2"로 시작해야 함 (1이 제거됨)
        assertThat(result.getValue()).endsWith("999"); // "999"로 끝나야 함
        
        // 정확한 매칭으로 "1"이 제거되었는지 확인 (부분 문자열 매칭 방지)
        java.util.List<String> idList = java.util.Arrays.asList(viewIds);
        assertThat(idList).doesNotContain("1"); // 정확한 "1" ID가 없어야 함
    }

}