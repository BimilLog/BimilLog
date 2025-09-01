package jaeik.bimillog.domain.post.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.application.port.out.*;
import jaeik.bimillog.domain.post.application.port.out.LoadUserInfoPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * <h2>게시글 상호작용 서비스</h2>
 * <p>PostInteractionUseCase의 구현체로, 게시글의 추천 및 조회수 관련 비즈니스 로직을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostInteractionService implements PostInteractionUseCase {

    private final PostCommandPort postCommandPort;
    private final PostQueryPort postQueryPort;
    private final PostLikeCommandPort postLikeCommandPort;
    private final PostLikeQueryPort postLikeQueryPort;
    private final LoadUserInfoPort loadUserInfoPort;
    private final ObjectMapper objectMapper;

    /**
     * <h3>게시글 추천</h3>
     * <p>게시글을 추천하거나 추천 취소합니다.</p>
     * <p>이미 추천한 게시글인 경우 추천을 취소하고, 추천하지 않은 게시글인 경우 추천을 추가합니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 추천할 게시글 ID
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void likePost(Long userId, Long postId) {
        User user = loadUserInfoPort.getReferenceById(userId);
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (postLikeQueryPort.existsByUserAndPost(user, post)) {
            postLikeCommandPort.deleteByUserAndPost(user, post);
            log.debug("Post like removed: userId={}, postId={}", userId, postId);
        } else {
            PostLike postLike = PostLike.builder().user(user).post(post).build();
            postLikeCommandPort.save(postLike);
            log.debug("Post like added: userId={}, postId={}", userId, postId);
        }
    }

    /**
     * <h3>게시글 조회수 증가 (쿠키 기반 중복 방지)</h3>
     * <p>게시글의 조회수를 1 증가시킵니다.</p>
     * <p>쿠키를 이용하여 동일한 사용자의 중복 조회를 방지합니다.</p>
     * <p>24시간 동안 최대 100개의 게시글 조회 기록을 유지합니다.</p>
     *
     * @param postId   조회수를 증가시킬 게시글 ID
     * @param request  HTTP 요청 (쿠키 확인용)
     * @param response HTTP 응답 (쿠키 설정용)
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void incrementViewCountWithCookie(Long postId, HttpServletRequest request, HttpServletResponse response) {
        // 1. 게시글 조회 (한 번만 조회하여 성능 개선)
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 쿠키 확인
        Cookie[] cookies = request.getCookies();

        // 3. 쿠키가 없거나 해당 게시글을 아직 조회하지 않은 경우에만 조회수 증가
        if (!hasViewedPost(cookies, postId)) {
            postCommandPort.incrementView(post); // Post 엔티티를 직접 사용하여 중복 조회 방지
            updateViewCookie(response, cookies, postId);
            log.debug("Post view count incremented with cookie protection: postId={}, newViewCount={}", postId, post.getViews());
        } else {
            log.debug("Post view count not incremented (already viewed): postId={}", postId);
        }
    }

    /**
     * <h3>조회 쿠키 업데이트</h3>
     * <p>사용자가 조회한 게시글 ID를 쿠키에 저장합니다.</p>
     * <p>쉼표로 구분된 문자열 형태로 저장하며, 최대 100개까지 유지합니다.</p>
     *
     * @param response HTTP 응답 (쿠키 설정용)
     * @param cookies  현재 요청의 쿠키 배열
     * @param postId   추가할 게시글 ID
     * @since 2.0.0
     * @author Jaeik
     */
    private void updateViewCookie(HttpServletResponse response, Cookie[] cookies, Long postId) {
        try {
            Set<String> viewedPostIds = new HashSet<>();

            // 기존 쿠키에서 조회 이력 로드
            if (cookies != null) {
                Optional<Cookie> existingCookie = Arrays.stream(cookies)
                        .filter(cookie -> "post_views".equals(cookie.getName()))
                        .findFirst();

                if (existingCookie.isPresent()) {
                    try {
                        String cookieValue = existingCookie.get().getValue();
                        if (!cookieValue.isEmpty()) {
                            viewedPostIds.addAll(Arrays.asList(cookieValue.split(",")));
                        }
                    } catch (Exception e) {
                        log.warn("Invalid cookie value, starting with empty set", e);
                    }
                }
            }

            // 새로운 게시글 ID 추가
            String postIdStr = postId.toString();
            if (!viewedPostIds.contains(postIdStr)) {
                viewedPostIds.add(postIdStr);

                // 최대 100개까지만 유지 (오래된 것 제거)
                if (viewedPostIds.size() > 100) {
                    List<String> sortedIds = new ArrayList<>(viewedPostIds);
                    viewedPostIds = new HashSet<>(sortedIds.subList(sortedIds.size() - 100, sortedIds.size()));
                }

                // 쉼표로 구분된 문자열로 저장
                String cookieValue = String.join(",", viewedPostIds);

                Cookie viewCookie = new Cookie("post_views", cookieValue);
                viewCookie.setMaxAge(24 * 60 * 60); // 24시간
                viewCookie.setPath("/");
                viewCookie.setHttpOnly(true);
                response.addCookie(viewCookie);
            }
        } catch (Exception e) {
            log.error("Failed to update view cookie for postId: {}", postId, e);
        }
    }

    /**
     * <h3>사용자가 게시글을 본 적 있는지 확인</h3>
     * <p>쿠키를 통해 사용자가 해당 게시글을 본 적 있는지 확인합니다.</p>
     * <p>쉼표로 구분된 문자열에서 게시글 ID를 정확히 매칭하여 검색합니다.</p>
     *
     * @param cookies 현재 요청의 쿠키 배열
     * @param postId  확인할 게시글 ID
     * @return 조회한 적이 있으면 true, 없으면 false
     * @since 2.0.0
     * @author Jaeik
     */
    private boolean hasViewedPost(Cookie[] cookies, Long postId) {
        if (cookies == null) {
            return false;
        }
        
        try {
            String postIdStr = postId.toString();
            return Arrays.stream(cookies)
                    .filter(cookie -> "post_views".equals(cookie.getName()))
                    .anyMatch(cookie -> {
                        try {
                            String cookieValue = cookie.getValue();
                            if (cookieValue == null || cookieValue.trim().isEmpty()) {
                                return false;
                            }
                            // 정확한 매칭을 위해 배열로 파싱하여 검색
                            List<String> viewedPostIds = Arrays.asList(cookieValue.split(","));
                            return viewedPostIds.contains(postIdStr);
                        } catch (Exception e) {
                            log.warn("Failed to parse view cookie", e);
                            return false;
                        }
                    });
        } catch (Exception e) {
            log.error("Error checking viewed posts for postId: {}", postId, e);
            return false;
        }
    }

    /**
     * <h3>게시글 조회수 증가 (이벤트 기반 중복 방지)</h3>
     * <p>게시글의 조회수를 1 증가시킵니다.</p>
     * <p>사용자 식별자와 조회 이력을 이용하여 동일한 사용자의 중복 조회를 방지합니다.</p>
     * <p>헥사고날 아키텍처를 준수하여 HTTP 의존성 없이 처리합니다.</p>
     *
     * @param postId         조회수를 증가시킬 게시글 ID
     * @param userIdentifier 사용자 식별자 (IP, 세션 ID 등)
     * @param viewHistory    사용자의 조회 이력 맵
     * @return 업데이트된 조회 이력 (쿠키 설정용)
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Map<String, String> incrementViewCountWithHistory(Long postId, String userIdentifier, Map<String, String> viewHistory) {
        // 1. 게시글 조회 (한 번만 조회하여 성능 개선)
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 조회 이력 확인
        String existingViews = viewHistory.getOrDefault("viewed_posts", "");
        
        // 3. 중복 조회 검사
        if (!hasViewedPostFromHistory(existingViews, postId)) {
            postCommandPort.incrementView(post);
            // 4. 조회 이력 업데이트
            String updatedViews = updateViewHistory(existingViews, postId);
            Map<String, String> updatedHistory = new HashMap<>(viewHistory);
            updatedHistory.put("viewed_posts", updatedViews);
            
            log.debug("Post view count incremented with history protection: postId={}, newViewCount={}", postId, post.getViews());
            return updatedHistory;
        } else {
            log.debug("Post view count not incremented (already viewed): postId={}", postId);
            return viewHistory; // 기존 이력 그대로 반환
        }
    }

    /**
     * <h3>조회 이력에서 게시글 조회 여부 확인</h3>
     * <p>쉼표로 구분된 문자열에서 게시글 ID를 정확히 매칭하여 검색합니다.</p>
     *
     * @param viewedPosts 쉼표로 구분된 조회한 게시글 ID 목록
     * @param postId      확인할 게시글 ID
     * @return 조회한 적이 있으면 true, 없으면 false
     * @since 2.0.0
     * @author Jaeik
     */
    private boolean hasViewedPostFromHistory(String viewedPosts, Long postId) {
        if (viewedPosts == null || viewedPosts.trim().isEmpty()) {
            return false;
        }
        
        try {
            String postIdStr = postId.toString();
            List<String> viewedPostIds = Arrays.asList(viewedPosts.split(","));
            return viewedPostIds.contains(postIdStr);
        } catch (Exception e) {
            log.warn("Failed to parse view history: {}", viewedPosts, e);
            return false;
        }
    }

    /**
     * <h3>조회 이력 업데이트</h3>
     * <p>새로운 게시글 ID를 조회 이력에 추가하고 최대 100개까지 유지합니다.</p>
     *
     * @param existingViews 기존 조회 이력
     * @param postId        추가할 게시글 ID
     * @return 업데이트된 조회 이력
     * @since 2.0.0
     * @author Jaeik
     */
    private String updateViewHistory(String existingViews, Long postId) {
        try {
            Set<String> viewedPostIds = new LinkedHashSet<>(); // 순서 유지를 위해 LinkedHashSet 사용
            
            // 기존 이력 로드
            if (existingViews != null && !existingViews.trim().isEmpty()) {
                viewedPostIds.addAll(Arrays.asList(existingViews.split(",")));
            }
            
            // 새로운 게시글 ID 추가
            String postIdStr = postId.toString();
            if (!viewedPostIds.contains(postIdStr)) {
                viewedPostIds.add(postIdStr);
                
                // 최대 100개까지만 유지 (오래된 것 제거)
                if (viewedPostIds.size() > 100) {
                    List<String> viewedList = new ArrayList<>(viewedPostIds);
                    viewedPostIds = new LinkedHashSet<>(viewedList.subList(viewedList.size() - 100, viewedList.size()));
                }
            }
            
            // 쉼표로 구분된 문자열로 변환
            return String.join(",", viewedPostIds);
        } catch (Exception e) {
            log.error("Failed to update view history for postId: {}", postId, e);
            // 오류 발생 시 기존 이력에 새 ID만 추가
            return existingViews.isEmpty() ? postId.toString() : existingViews + "," + postId;
        }
    }
}