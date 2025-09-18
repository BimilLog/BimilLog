package jaeik.bimillog.infrastructure.adapter.in.post.web.util;

import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <h2>PostViewCookieUtil</h2>
 * <p>
 * 게시글 중복 조회 방지를 위한 HTTP 쿠키 처리 로직을 담당하는 유틸리티 컴포넌트입니다.
 * </p>
 * <p>
 * 사용자 경험 향상을 위해 동일한 게시글에 대한 중복 조회수 증가를 방지하고,
 * 쿠키 기반으로 24시간 동안 조회 이력을 관리하여 정확한 게시글 조회수 통계를 제공합니다.
 * </p>
 * <p>
 * PostQueryController에서 게시글 상세 조회 시 중복 검사와 조회 이력 저장을 위해 호출되며,
 * HTTP 쿠키 의존성으로 인해 웹 레이어 유틸리티로 분류됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class PostViewCookieUtil {

    private static final String COOKIE_NAME = "post_views";
    private static final int MAX_VIEWS = 100;
    private static final int COOKIE_MAX_AGE = 24 * 60 * 60; // 24시간

    /**
     * <h3>게시글 조회 여부 확인</h3>
     * <p>쿠키에서 해당 게시글의 조회 이력을 확인합니다.</p>
     *
     * @param cookies HTTP 요청 쿠키 배열
     * @param postId  확인할 게시글 ID
     * @return 조회한 적이 있으면 true, 없으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean hasViewed(Cookie[] cookies, Long postId) {
        if (cookies == null || postId == null) {
            return false;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(Objects::nonNull)
                .filter(value -> !value.trim().isEmpty())
                .anyMatch(value -> Arrays.asList(value.split(",")).contains(postId.toString()));
    }

    /**
     * <h3>조회 이력 쿠키 생성</h3>
     * <p>기존 조회 이력에 새로운 게시글 ID를 추가하여 쿠키를 생성합니다.</p>
     *
     * @param cookies HTTP 요청 쿠키 배열
     * @param postId  추가할 게시글 ID
     * @return 업데이트된 조회 이력 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    public Cookie createViewCookie(Cookie[] cookies, Long postId) {
        if (postId == null) {
            return createEmptyCookie();
        }

        String currentViews = extractCurrentViews(cookies);
        String updatedViews = addPostIdToViews(currentViews, postId);

        Cookie cookie = new Cookie(COOKIE_NAME, updatedViews);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

    /**
     * <h3>현재 조회 이력 추출</h3>
     * <p>쿠키에서 현재 조회 이력 문자열을 추출합니다.</p>
     *
     * @param cookies HTTP 요청 쿠키 배열
     * @return 조회 이력 문자열 (없으면 빈 문자열)
     * @author Jaeik
     * @since 2.0.0
     */
    private String extractCurrentViews(Cookie[] cookies) {
        if (cookies == null) {
            return "";
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(Objects::nonNull)
                .filter(value -> !value.trim().isEmpty())
                .findFirst()
                .orElse("");
    }

    /**
     * <h3>조회 이력에 게시글 ID 추가</h3>
     * <p>기존 조회 이력에 새로운 게시글 ID를 추가하고 최대 개수 제한을 적용합니다.</p>
     *
     * @param currentViews 현재 조회 이력
     * @param postId       추가할 게시글 ID
     * @return 업데이트된 조회 이력
     * @author Jaeik
     * @since 2.0.0
     */
    private String addPostIdToViews(String currentViews, Long postId) {
        List<String> viewList = new ArrayList<>();
        
        // 기존 이력이 있으면 추가
        if (currentViews != null && !currentViews.trim().isEmpty()) {
            viewList.addAll(Arrays.asList(currentViews.split(",")));
        }
        
        // 중복 제거 후 새로운 ID 추가 (이미 있으면 추가하지 않음)
        String postIdStr = postId.toString();
        if (!viewList.contains(postIdStr)) {
            viewList.add(postIdStr);
        }
        
        // 최대 개수 제한 적용 (오래된 것부터 제거)
        if (viewList.size() > MAX_VIEWS) {
            viewList = viewList.subList(viewList.size() - MAX_VIEWS, viewList.size());
        }
        
        return String.join(",", viewList);
    }

    /**
     * <h3>빈 쿠키 생성</h3>
     * <p>오류 상황에서 사용할 빈 쿠키를 생성합니다.</p>
     *
     * @return 빈 값의 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    private Cookie createEmptyCookie() {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}