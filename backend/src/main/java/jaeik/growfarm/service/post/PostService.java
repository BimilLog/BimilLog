package jaeik.growfarm.service.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
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
import jaeik.growfarm.service.redis.RedisPostService;
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

import java.util.*;

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
    private final ApplicationEventPublisher eventPublisher;
    private final PostUpdateService postUpdateService;
    private final RedisPostService redisPostService;

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
     * <p>
     * 비회원은 패스워드를 저장한다. user는 null이다.
     * </p>
     * <p>
     * 회원은 패스워드가 null이다.
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
        Post post = postRepository.save(Post.createPost(user, postReqDTO));
        return PostDTO.newPost(post);
    }

    /**
     * <h3>게시글 수정</h3>
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있습니다.
     * </p>
     *
     * @param userDetails 현재 로그인 한 사용자 정보
     * @param postDTO     수정할 게시글 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    public void updatePost(CustomUserDetails userDetails, PostDTO postDTO) {
        Post post = ValidatePost(userDetails, postDTO);
        postUpdateService.postUpdate(postDTO, post);
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>
     * 게시글 작성자만 게시글을 삭제할 수 있습니다다.
     * </p>
     * <p>
     * 인기글인 경우 Redis 캐시에서도 즉시 삭제됩니다.
     * </p>
     * <p>
     * 댓글은 CASCADE 설정으로 자동 삭제됩니다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     게시글 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    public void deletePost(CustomUserDetails userDetails, PostDTO postDTO) {
        Post post = ValidatePost(userDetails, postDTO);
        postUpdateService.postDelete(post.getId());
    }

    /**
     * <h3>게시글 추천</h3>
     *
     * <p>
     * 게시글을 추천하거나 추천 취소한다.
     * </p>
     * <p>
     * 이미 추천한 경우 추천을 취소하고, 추천하지 않은 경우 추천을 추가한다.
     * </p>
     *
     * @param postDTO     추천할 게시글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void likePost(PostDTO postDTO, CustomUserDetails userDetails) {
        Long postId = postDTO.getPostId();
        Long userId = userDetails.getUserId();

        Post post = postRepository.getReferenceById(postId);
        Users user = userRepository.getReferenceById(userId);

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
        postUpdateService.savePostLike(existingLike, post, user);
    }

    /**
     * <h3>게시글 유효성 검사</h3>
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있다.
     * </p>
     * <p>
     * 비회원은 패스워드를 입력해야 한다.
     * </p>
     * <p>
     * 회원은 패스워드를 입력하지 않아도 된다. userId로 검사한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     게시글 정보 DTO
     * @return 유효한 게시글 엔티티
     * @throws CustomException 게시글이 존재하지 않거나 작성자가 일치하지 않는 경우
     * @author Jaeik
     * @since 1.0.0
     */
    private Post ValidatePost(CustomUserDetails userDetails, PostDTO postDTO) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        Post post = postRepository.findById(postDTO.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!Objects.equals(postDTO.getUserId(), (userId))) {
            throw new CustomException(ErrorCode.POST_UPDATE_FORBIDDEN);
        }

        if (userId == null) {
            if (!Objects.equals(postDTO.getPassword(), post.getPassword())) {
                throw new CustomException(ErrorCode.POST_PASSWORD_NOT_MATCH);
            }
        }
        return post;
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
    public void incrementViewCount(Long postId, HttpServletRequest request, HttpServletResponse response) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        Cookie[] cookies = request.getCookies();

        if (!hasViewedPost(cookies, postId)) {
            postUpdateService.updateViewCount(postId);
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
     * @since 1.0.0
     */
    private boolean hasViewedPost(Cookie[] cookies, Long postId) {
        if (cookies == null)
            return false;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return Arrays.stream(cookies).filter(cookie -> "post_views".equals(cookie.getName())).anyMatch(cookie -> {
                try {
                    String jsonValue = new String(Base64.getDecoder().decode(cookie.getValue()));
                    List<Long> viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<>() {
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
     * <h3>실시간 인기글 선정</h3>
     * <p>
     * 1일 이내의 글 중 추천 수가 가장 높은 상위 5개를 실시간 인기글로 등록한다.
     * </p>
     * <p>
     * redis에 캐시한다.
     * </p>
     * <p>
     * 30분마다 시행한다.
     * </p>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    @Scheduled(fixedRate = 60000 * 30)
    public void updateRealtimePopularPosts() {
        List<SimplePostDTO> realtimePosts = postRepository.updateRealtimePopularPosts();
        redisPostService.cachePopularPosts(RedisPostService.PopularPostType.REALTIME, realtimePosts);
    }

    /**
     * <h3>주간 인기글 선정</h3>
     * <p>
     * 7일 이내의 글 중 추천 수가 가장 높은 상위 5개를 주간 인기글로 등록한다.
     * </p>
     * <p>
     * redis에 캐시한다.
     * </p>
     * <p>
     * 1일마다 시행한다.
     * </p>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440)
    public void updateWeeklyPopularPosts() {
        List<SimplePostDTO> weeklyPosts = postRepository.updateWeeklyPopularPosts();
        redisPostService.cachePopularPosts(RedisPostService.PopularPostType.WEEKLY, weeklyPosts);

        for (SimplePostDTO simplePostDTO : weeklyPosts) {
            if (simplePostDTO.getUser() != null) {
                eventPublisher.publishEvent(new PostFeaturedEvent(
                        simplePostDTO.getUserId(),
                        "🎉 회원님의 글이 주간 인기글로 선정되었습니다!",
                        simplePostDTO.getPostId(),
                        "회원님의 글이 주간 인기글로 선정되었습니다!",
                        "지금 확인해보세요!"));
            }
        }
    }

    /**
     * <h3>레전드 게시글 선정</h3>
     * <p>
     * 추천 수가 20개 이상인 글을 선정한다.
     * </p>
     *
     * <p>
     * redis에 캐시한다.
     * </p>
     * <p>
     * 1일마다 시행한다.
     * </p>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440)
    public void updateLegendPopularPosts() {
        List<SimplePostDTO> legendPosts = postRepository.updateLegendPosts();
        redisPostService.cachePopularPosts(RedisPostService.PopularPostType.LEGEND, legendPosts);

        for (SimplePostDTO simplePostDTO : legendPosts) {
            eventPublisher.publishEvent(new PostFeaturedEvent(
                    simplePostDTO.getUserId(),
                    "🎉 회원님의 글이 레전드글로 선정되었습니다!",
                    simplePostDTO.getPostId(),
                    "회원님의 글이 레전드글로 선정되었습니다!",
                    "지금 확인해보세요!"));
        }
    }
}
