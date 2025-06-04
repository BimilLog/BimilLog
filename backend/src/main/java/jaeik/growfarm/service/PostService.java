package jaeik.growfarm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.PostReqDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.event.PostFeaturedEvent;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.notification.FcmService;
import jaeik.growfarm.service.notification.SseService;
import jaeik.growfarm.service.notification.NotificationService;
import jaeik.growfarm.util.BoardUtil;
import jaeik.growfarm.util.NotificationUtil;
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

/*
 * PostService 클래스
 * 게시판 관련 서비스 클래스
 * 수정일 : 2025-05-03
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final BoardUtil boardUtil;
    private final ReportRepository reportRepository;
    private final NotificationService notificationService;
    private final NotificationUtil notificationUtil;
    private final FcmTokenRepository fcmTokenRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final SseService sseService;
    private final FcmService fcmService;

    // 이벤트 발행을 위한 ApplicationEventPublisher 🚀
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>게시판 조회</h3>
     *
     * <p>
     * 최신순으로 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param page 페이지 번호
     * @param size 페이지 사이즈
     * @return 게시글 목록 페이지
     */
    public Page<SimplePostDTO> getBoard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findAll(pageable);
        return convertToSimplePostDTOPage(posts);
    }

    /**
     * <h3>실시간 인기글 목록 조회</h3>
     *
     * <p>
     * 실시간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 실시간 인기글 목록
     */
    public List<SimplePostDTO> getRealtimePopularPosts() {
        List<Post> realtimePopularPosts = postRepository.findByIsRealtimePopularTrue();
        return convertToSimplePostDTOList(realtimePopularPosts);
    }

    /**
     * <h3>주간 인기글 목록 조회</h3>
     *
     * <p>
     * 주간 인기글로 선정된 게시글 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 주간 인기글 목록
     */
    public List<SimplePostDTO> getWeeklyPopularPosts() {
        List<Post> weeklyPopularPosts = postRepository.findByIsWeeklyPopularTrue();
        return convertToSimplePostDTOList(weeklyPopularPosts);
    }

    /**
     * <h3>명예의 전당 게시글 목록 조회</h3>
     *
     * <p>
     * 명예의 전당에 선정된 게시글 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 명예의 전당 게시글 목록
     */
    public List<SimplePostDTO> getHallOfFamePosts() {
        List<Post> hallOfFamePosts = postRepository.findByIsHallOfFameTrue();
        return convertToSimplePostDTOList(hallOfFamePosts);
    }

    /**
     * <h3>게시글 검색</h3>
     *
     * <p>
     * 검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param type  검색 유형
     * @param query 검색어
     * @param page  페이지 번호
     * @param size  페이지 사이즈
     * @return 검색된 게시글 목록 페이지
     */
    public Page<SimplePostDTO> searchPost(String type, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts;
        switch (type) {
            case "제목" -> posts = postRepository.findByTitleContaining(query, pageable);
            case "제목내용" -> posts = postRepository.findByTitleContainingOrContentContaining(query, query, pageable);
            case "작성자" -> posts = postRepository.findByUser_farmNameContaining(query, pageable);
            default -> throw new IllegalArgumentException("잘못된 검색 타입입니다: " + type);
        }
        return convertToSimplePostDTOPage(posts);
    }

    /**
     * <h3>게시글 작성</h3>
     *
     * <p>
     * 새로운 게시글을 작성하고 저장한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 작성된 게시글 DTO
     */
    public PostDTO writePost(CustomUserDetails userDetails, PostReqDTO postReqDTO) {

        if (userDetails == null) {
            throw new IllegalArgumentException("로그인 후 작성해주세요.");
        }

        Users user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Post post = postRepository.save(boardUtil.postReqDTOToPost(user, postReqDTO));
        return boardUtil.postToDTO(post, postLikeRepository.countByPostId(post.getId()), null, false);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     *
     * <p>
     * 게시글 ID를 통해 게시글 상세 정보를 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId 게시글 ID
     * @param userId 사용자 ID (좋아요 여부 확인용)
     * @return 게시글 상세 DTO
     */
    public PostDTO getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        boolean isLiked = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);
        ;

        return boardUtil.postToDTO(post, postLikeRepository.countByPostId(post.getId()), getCommentList(postId, userId),
                isLiked);
    }

    /**
     * <h3>게시글 수정</h3>
     *
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     수정할 게시글 정보 DTO
     * @return 수정된 게시글 DTO
     */
    @Transactional
    public PostDTO updatePost(Long postId, CustomUserDetails userDetails, PostDTO postDTO) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        Long userId = post.getUser().getId();

        if (!userId.equals(userDetails.getClientDTO().getUserId())) {
            throw new IllegalArgumentException("게시글 작성자가 아닙니다.");
        }

        post.updatePost(postDTO);
        return boardUtil.postToDTO(post, postLikeRepository.countByPostId(post.getId()), getCommentList(postId, userId),
                postLikeRepository.existsByPostIdAndUserId(postId, userDetails.getUserId()));
    }

    /**
     * <h3>게시글 삭제</h3>
     *
     * <p>
     * 게시글 작성자만 게시글을 삭제할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
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

    private Page<SimplePostDTO> convertToSimplePostDTOPage(Page<Post> posts) {
        return posts.map(post -> boardUtil.postToSimpleDTO(
                post,
                commentRepository.countByPostId(post.getId()),
                postLikeRepository.countByPostId(post.getId())));
    }

    // 해당 글의 댓글 조회
    private List<CommentDTO> getCommentList(Long postId, Long userId) {

        return commentRepository.findByCommentList(postId).stream().map(comment -> {
            boolean userLike = userId != null
                    && commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), userId);
            return boardUtil.commentToDTO(comment, commentLikeRepository.countByCommentId(comment.getId()), userLike);
        }).toList();
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

    // 조회수 증가
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
}
