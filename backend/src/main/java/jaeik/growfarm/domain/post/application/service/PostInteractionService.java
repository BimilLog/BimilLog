package jaeik.growfarm.domain.post.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.application.port.out.LoadUserInfoPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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
     * <h3>게시글 조회수 증가 (간단)</h3>
     * <p>게시글의 조회수를 1 증가시킵니다.</p>
     * <p>엔티티의 더티 체킹에 의해 자동으로 저장됩니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void incrementViewCount(Long postId) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        postCommandPort.incrementView(post);
        log.debug("Post view count incremented: postId={}, newViewCount={}", postId, post.getViews());
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
     * <p>JSON 배열을 Base64로 인코딩하여 저장하며, 최대 100개까지 유지합니다.</p>
     *
     * @param response HTTP 응답 (쿠키 설정용)
     * @param cookies  현재 요청의 쿠키 배열
     * @param postId   추가할 게시글 ID
     * @since 2.0.0
     * @author Jaeik
     */
    private void updateViewCookie(HttpServletResponse response, Cookie[] cookies, Long postId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Long> viewedPostIds = new ArrayList<>();

            if (cookies != null) {
                Optional<Cookie> existingCookie = Arrays.stream(cookies)
                        .filter(cookie -> "post_views".equals(cookie.getName()))
                        .findFirst();

                if (existingCookie.isPresent()) {
                    try {
                        String jsonValue = new String(Base64.getDecoder().decode(existingCookie.get().getValue()));
                        viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<List<Long>>() {
                        });
                    } catch (Exception e) {
                        // 기존 쿠키 값이 유효하지 않은 경우 빈 리스트로 시작
                        viewedPostIds = new ArrayList<>();
                        log.warn("Invalid cookie value, starting with empty list", e);
                    }
                }
            }

            // 이미 본 게시글이면 추가하지 않음
            if (!viewedPostIds.contains(postId)) {
                viewedPostIds.add(postId);

                // 최대 100개까지만 유지
                if (viewedPostIds.size() > 100) {
                    viewedPostIds = viewedPostIds.subList(viewedPostIds.size() - 100, viewedPostIds.size());
                }

                // JSON으로 직렬화 - Base64로 인코딩
                String jsonValue = objectMapper.writeValueAsString(viewedPostIds);
                String encodedValue = Base64.getEncoder().encodeToString(jsonValue.getBytes());

                Cookie viewCookie = new Cookie("post_views", encodedValue);
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
            ObjectMapper objectMapper = new ObjectMapper();
            return Arrays.stream(cookies)
                    .filter(cookie -> "post_views".equals(cookie.getName()))
                    .anyMatch(cookie -> {
                        try {
                            String jsonValue = new String(Base64.getDecoder().decode(cookie.getValue()));
                            List<Long> viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<>() {
                            });
                            return viewedPostIds.contains(postId);
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
}