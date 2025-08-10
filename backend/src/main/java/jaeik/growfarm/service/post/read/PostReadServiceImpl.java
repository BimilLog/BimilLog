package jaeik.growfarm.service.post.read;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.post.read.PostReadRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.service.post.PostInteractionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <h2>게시글 조회 서비스 구현체</h2>
 * <p>
 * 게시글 조회 관련 기능을 구현하는 서비스 클래스
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class PostReadServiceImpl implements PostReadService {

    private final PostReadRepository postReadRepository;
    private final PostRepository postRepository;
    private final PostInteractionService postInteractionService;

    /**
     * <h3>게시판 조회</h3>
     * <p>
     * 최신순으로 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     *
     * @param page 페이지 번호
     * @param size 페이지 사이즈
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> getBoard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postReadRepository.findSimplePost(pageable);
    }

    /**
     * <h3>게시글 조회</h3>
     * <p>
     * 게시글 ID를 통해 게시글 상세 정보를 조회한다.
     * </p>
     *
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인 한 사용자 정보
     * @return 게시글 상세 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public FullPostResDTO getPost(Long postId, CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        return postReadRepository.findPostById(postId, userId);
    }

    /**
     * <h3>게시글 조회수 증가</h3>
     * <p>
     * 게시글 조회 시 조회수를 증가시키고, 쿠키에 해당 게시글 ID를 저장한다.
     * </p>
     *
     * @param postId   게시글 ID
     * @param request  HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void incrementViewCount(Long postId, HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();

        if (!hasViewedPost(cookies, postId)) {
            postInteractionService.incrementViewCount(postId);
            updateViewCookie(response, cookies, postId);
        }
    }

    /**
     * <h3>게시글 조회 쿠키 업데이트</h3>
     * <p>
     * 사용자가 본 게시글 ID를 쿠키에 저장한다. 최대 100개까지만 저장하며, 오래된 게시글은 제거한다.
     * </p>
     *
     * @param response HTTP 응답 객체
     * @param cookies  현재 요청의 쿠키 배열
     * @param postId   현재 조회한 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void updateViewCookie(HttpServletResponse response, Cookie[] cookies, Long postId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Long> viewedPostIds = new ArrayList<>();

            if (cookies != null) {
                Optional<Cookie> existingCookie = Arrays.stream(cookies)
                        .filter(cookie -> "post_views".equals(cookie.getName())).findFirst();

                if (existingCookie.isPresent()) {
                    try {
                        String jsonValue = new String(Base64.getDecoder().decode(existingCookie.get().getValue()));
                        viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<List<Long>>() {
                        });
                    } catch (Exception e) {
                        viewedPostIds = new ArrayList<>();
                    }
                }
            }

            if (!viewedPostIds.contains(postId)) {
                viewedPostIds.add(postId);

                if (viewedPostIds.size() > 100) {
                    viewedPostIds = viewedPostIds.subList(viewedPostIds.size() - 100, viewedPostIds.size());
                }

                String jsonValue = objectMapper.writeValueAsString(viewedPostIds);
                String encodedValue = Base64.getEncoder().encodeToString(jsonValue.getBytes());

                Cookie viewCookie = new Cookie("post_views", encodedValue);
                viewCookie.setMaxAge(24 * 60 * 60);
                viewCookie.setPath("/");
                viewCookie.setHttpOnly(true);
                viewCookie.setSecure(false);
                response.addCookie(viewCookie);
            }
        } catch (Exception ignored) {
        }
    }


    /**
     * <h3>사용자가 게시글을 본 적 있는지 확인</h3>
     * <p>
     * 쿠키를 통해 사용자가 해당 게시글을 본 적 있는지 확인한다.
     * </p>
     *
     * @param cookies 현재 요청의 쿠키 배열
     * @param postId  게시글 ID
     * @return true: 본 적 있음, false: 본 적 없음
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean hasViewedPost(Cookie[] cookies, Long postId) {
        if (cookies == null)
            return false;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return Arrays.stream(cookies).filter(cookie -> "post_views".equals(cookie.getName())).anyMatch(cookie -> {
                try {
                    String jsonValue = new String(Base64.getDecoder().decode(cookie.getValue()));
                    List<Long> viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<>() {});
                    return viewedPostIds.contains(postId);
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (Exception e) {
            return false;
        }
    }
}