package jaeik.growfarm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.PostReqDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.PostFeaturedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.BoardUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>게시판 서비스</h2>
 * <p>
 * 게시글 CRUD, 검색, 추천 기능을 제공하는 서비스 클래스
 * </p>
 * <p>
 * 게시글 조회수 증가, 인기글 등록 등의 기능도 포함되어 있다.
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final BoardUtil boardUtil;
    private final ApplicationEventPublisher eventPublisher;

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
     * @since 1.0.0
     */
    public Page<SimplePostDTO> getBoard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findPostsWithCommentAndLikeCounts(pageable);
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
     * @since 1.0.0
     */
    public PostDTO getPost(Long postId, CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        return postRepository.findPostById(postId, userId);
    }

    /**
     * <h3>게시글 검색</h3>
     * <p>
     * 검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션한다.
     * </p>
     *
     * @param type  검색 유형
     * @param query 검색어
     * @param page  페이지 번호
     * @param size  페이지 사이즈
     * @return 검색된 게시글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    public Page<SimplePostDTO> searchPost(String type, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.searchPosts(query, type, pageable);
    }

    /**
     * <h3>게시글 작성</h3>
     * <p>
     * 새로운 게시글을 작성하고 저장한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 작성된 게시글 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public PostDTO writePost(CustomUserDetails userDetails, PostReqDTO postReqDTO) {
        Users user = (userDetails != null) ? userRepository.getReferenceById(userDetails.getUserId()) : null;
        int password = userDetails == null ? postReqDTO.getPassword() : null;

        Post post = postRepository.save(Post.createPost(user, postReqDTO));
        return new PostDTO(post.getId(),
                post.getUser().getId(),
                post.getUser().getUserName(),
                post.getTitle(),
                post.getContent(),
                0,
                0,
                post.isNotice(),
                post.getPopularFlag(),
                post.getCreatedAt(),
                false,
                password);
    }

    /**
     * <h3>게시글 수정</h3>
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있다.
     * </p>
     *
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     수정할 게시글 정보 DTO
     * @return 수정된 게시글 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public PostDTO updatePost(CustomUserDetails userDetails, PostDTO postDTO) {
        Users user = (userDetails != null) ? userRepository.getReferenceById(userDetails.getUserId()) : null;
        Post post = postRepository.findById(postDTO.getPostId()).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!Objects.equals(postDTO.getUserId(), (user != null ? user.getId() : null))) {
            throw new CustomException(ErrorCode.POST_UPDATE_FORBIDDEN);
        }

        if (user == null) {
            if(postDTO.getPassword() != post.getPassword()) {
                throw new CustomException(ErrorCode.POST_UPDATE_FORBIDDEN);
            }
        }

        post.updatePost(postDTO);
        return new PostDTO(post.getId(),
                post.getUser().getId(),
                post.getUser().getUserName(),
                post.getTitle(),
                post.getContent(),
                commentRepository.countByPostId(post.getId()),
                postLikeRepository.countByPostId(post.getId()),
                post.isNotice(),
                post.getPopularFlag(),
                post.getCreatedAt(),
                false,
                post.getPassword());
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>
     * 게시글 작성자만 게시글을 삭제할 수 있다.
     * </p>
     *
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void deletePost(Long postId, CustomUserDetails userDetails) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        if (!post.getUser().getId().equals(userDetails.getClientDTO().getUserId())) {
            throw new IllegalArgumentException("게시글 작성자가 아닙니다.");
        }

        postLikeRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
    }

    // 게시글 추천, 추천 취소
    public void likePost(Long postId, CustomUserDetails userDetails) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        Users user = userRepository.findById(userDetails.getClientDTO().getUserId()).orElseThrow(
                () -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userDetails.getClientDTO().getUserId()));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId,
                userDetails.getClientDTO().getUserId());

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
        } else {
            PostLike postLike = PostLike.builder().post(post).user(user).build();

            postLikeRepository.save(postLike);
        }
    }

    private List<SimplePostDTO> convertToSimplePostDTOList(List<Post> posts) {
        return posts.stream()
                .map(post -> boardUtil.postToSimpleDTO(post,
                        commentRepository.countByPostId(post.getId()),
                        postLikeRepository.countByPostId(post.getId())))
                .collect(Collectors.toList());
    }


    // 실시간 인기글 등록
    // 1일 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개
    // 30분 스케줄러
    @Transactional
    @Scheduled(fixedRate = 60000 * 30)
    public void updateRealtimePopularPosts() {
        postRepository.updateRealtimePopularPosts();

    }

    // 주간 인기글 등록
    // 7일 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개
    // 1일 스케줄러 - 이벤트 기반 비동기 처리
    @Transactional
    @Scheduled(fixedRate = 60000 * 1440)
    public void updateWeeklyPopularPosts() throws IOException {
        List<Post> existedPosts = postRepository.findByIsWeeklyPopularTrue();
        List<Post> updatedPosts = postRepository.updateWeeklyPopularPosts();

        // ID 기준으로 비교
        Set<Long> existedIds = existedPosts.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());

        // 새롭게 추가된 인기글 ID만 추출 (updated에는 있는데 existed에는 없는 것)
        Set<Long> newlyFeaturedIds = updatedPosts.stream()
                .map(Post::getId).collect(Collectors.toSet());
        newlyFeaturedIds.removeAll(existedIds);

        // 해당 ID에 해당하는 Post 객체 필터링
        List<Post> newlyFeaturedPosts = updatedPosts.stream()
                .filter(post -> newlyFeaturedIds.contains(post.getId()))
                .toList();

        for (Post post : newlyFeaturedPosts) {
            // 이벤트 발행 🚀 (알림은 이벤트 리스너에서 비동기로 처리)
            eventPublisher.publishEvent(new PostFeaturedEvent(
                    post.getUser().getId(),
                    "🎉 회원님의 글이 주간 인기글로 선정되었습니다!",
                    post.getId(),
                    post.getUser(),
                    "회원님의 글이 주간 인기글로 선정되었습니다!",
                    "지금 확인해보세요!"));
        }
    }

    // 명예의 전당 등록
    // 추천 수가 20개 이상인 글
    // 1일 스케줄러 - 이벤트 기반 비동기 처리
    @Transactional
    @Scheduled(fixedRate = 60000 * 1440)
    public void updateHallOfFamePosts() throws IOException {
        List<Post> existedPosts = postRepository.findByIsHallOfFameTrue();
        List<Post> updatedPosts = postRepository.updateHallOfFamePosts();

        // ID 기준으로 비교
        Set<Long> existedIds = existedPosts.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());

        // 새롭게 추가된 인기글 ID만 추출 (updated에는 있는데 existed에는 없는 것)
        Set<Long> newlyFeaturedIds = updatedPosts.stream()
                .map(Post::getId).collect(Collectors.toSet());
        newlyFeaturedIds.removeAll(existedIds);

        // 해당 ID에 해당하는 Post 객체 필터링
        List<Post> newlyFeaturedPosts = updatedPosts.stream()
                .filter(post -> newlyFeaturedIds.contains(post.getId()))
                .toList();

        for (Post post : newlyFeaturedPosts) {
            // 이벤트 발행 🚀 (알림은 이벤트 리스너에서 비동기로 처리)
            eventPublisher.publishEvent(new PostFeaturedEvent(
                    post.getUser().getId(),
                    "🎉 회원님의 글이 명예의 전당에 등록 되었습니다!",
                    post.getId(),
                    post.getUser(),
                    "회원님의 글이 명예의 전당에 등록 되었습니다!",
                    "지금 확인해보세요!"));
        }
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
     * @since 1.0.0
     */
    @Transactional
    public void incrementViewCount(Long postId, HttpServletRequest request, HttpServletResponse response) {
        // 1. 게시글 존재 확인
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId);
        }

        // 2. 쿠키 확인
        Cookie[] cookies = request.getCookies();

        // 3. 쿠키가 없거나 해당 게시글을 아직 조회하지 않은 경우에만 조회수 증가
        if (!hasViewedPost(cookies, postId)) {
            postRepository.incrementViews(postId);
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
     * @since 1.0.0
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
                        // Base64 디코딩 후 파싱
                        String jsonValue = new String(Base64.getDecoder().decode(existingCookie.get().getValue()));
                        viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<List<Long>>() {
                        });
                    } catch (Exception e) {
                        // 기존 쿠키 값이 유효하지 않은 경우 빈 리스트로 시작
                        viewedPostIds = new ArrayList<>();
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
        } catch (Exception ignored) {
        }
    }

    /**
     * <h3>사용자가 게시글을 본 적이 있는지 확인</h3>
     * <p>
     * 쿠키를 통해 사용자가 해당 게시글을 본 적이 있는지 확인한다.
     * </p>
     *
     * @param cookies 현재 요청의 쿠키 배열
     * @param postId  게시글 ID
     * @return true: 본 적 있음, false: 본 적 없음
     * @author Jaeik
     * @since 1.0.0
     */
    private boolean hasViewedPost(Cookie[] cookies, Long postId) {
        if (cookies == null)
            return false;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return Arrays.stream(cookies).filter(cookie -> "post_views".equals(cookie.getName())).anyMatch(cookie -> {
                try {
                    // Base64 디코딩 후 파싱
                    String jsonValue = new String(Base64.getDecoder().decode(cookie.getValue()));
                    List<Long> viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<List<Long>>() {
                    });
                    return viewedPostIds.contains(postId);
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * <h3>실시간 인기글 목록 조회</h3>
     * <p>
     * 실시간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @return 실시간 인기글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    public List<SimplePostDTO> getRealtimePopularPosts() {
        List<Post> realtimePopularPosts = postRepository.findByIsRealtimePopularTrue();
        return convertToSimplePostDTOList(realtimePopularPosts);
    }

    /**
     * <h3>주간 인기글 목록 조회</h3>
     * <p>
     * 주간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @return 주간 인기글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    public List<SimplePostDTO> getWeeklyPopularPosts() {
        List<Post> weeklyPopularPosts = postRepository.findByIsWeeklyPopularTrue();
        return convertToSimplePostDTOList(weeklyPopularPosts);
    }

    /**
     * <h3>명예의 전당 게시글 목록 조회</h3>
     * <p>
     * 명예의 전당에 선정된 게시글 목록을 조회한다.
     * </p>
     *
     * @return 명예의 전당 게시글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    public List<SimplePostDTO> getHallOfFamePosts() {
        List<Post> hallOfFamePosts = postRepository.findByIsHallOfFameTrue();
        return convertToSimplePostDTOList(hallOfFamePosts);
    }
}
