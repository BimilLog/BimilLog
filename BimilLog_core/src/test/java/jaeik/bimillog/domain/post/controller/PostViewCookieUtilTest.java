package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.post.controller.util.PostViewCookieUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostViewCookieUtil 테스트</h2>
 * <p>게시글 조회 쿠키 유틸리티의 중복 검증 로직을 검증하는 단위 테스트</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostViewCookieUtil 테스트")
@Tag("unit")
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

    @ParameterizedTest(name = "postId={0} → viewed={1}")
    @CsvSource({"999, false", "123, true"})
    @DisplayName("조회 여부 확인")
    void shouldCheckViewStatus(Long postId, boolean expected) {
        // When
        boolean result = postViewCookieUtil.hasViewed(cookies, postId);

        // Then
        assertThat(result).isEqualTo(expected);
    }


    @ParameterizedTest
    @MethodSource("provideCookieSizeScenarios")
    @DisplayName("쿠키 생성 - 다양한 쿠키 크기")
    void shouldCreateCookie_VariousSizes(String description, Cookie[] inputCookies, Long postId, int expectedMaxSize) {
        // When
        Cookie result = postViewCookieUtil.createViewCookie(inputCookies, postId);

        // Then
        assertThat(result.getName()).isEqualTo("post_views");
        assertThat(result.getMaxAge()).isEqualTo(24 * 60 * 60); // 24시간
        assertThat(result.getPath()).isEqualTo("/");
        assertThat(result.isHttpOnly()).isTrue();
        assertThat(result.getValue()).contains(postId.toString());

        String[] viewIds = result.getValue().split("_");
        assertThat(viewIds.length).isLessThanOrEqualTo(expectedMaxSize);
    }

    static Stream<Arguments> provideCookieSizeScenarios() {
        // 빈 쿠키 (처음 조회)
        Cookie[] emptyCookies = new Cookie[]{new Cookie("other_cookie", "other_value")};

        // 기존 쿠키에 3개 ID (추가)
        Cookie[] cookiesWithViews = new Cookie[]{
                new Cookie("other_cookie", "other_value"),
                new Cookie("post_views", "123_456_789")
        };

        // 100개 ID가 있는 쿠키 (제한 테스트)
        StringBuilder existingViews = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            if (i > 1) existingViews.append("_");
            existingViews.append(i);
        }
        Cookie[] fullCookies = new Cookie[]{new Cookie("post_views", existingViews.toString())};

        return Stream.of(
            Arguments.of("처음 조회", emptyCookies, 999L, 100),
            Arguments.of("기존 조회 이력에 추가", cookiesWithViews, 999L, 100),
            Arguments.of("100개 제한", fullCookies, 999L, 100)
        );
    }

}