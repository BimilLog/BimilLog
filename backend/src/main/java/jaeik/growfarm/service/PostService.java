package jaeik.growfarm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.PostReqDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.board.Post;
import jaeik.growfarm.entity.board.PostLike;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.BoardUtil;
import jaeik.growfarm.util.NotificationUtil;
import jaeik.growfarm.util.UserUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final CommentService commentService;
    private final BoardUtil boardUtil;
    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private final UserUtil userUtil;
    private final ReportRepository reportRepository;
    private final NotificationService notificationService;
    private final NotificationUtil notificationUtil;

    // 게시판 진입
    public Page<SimplePostDTO> getBoard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.map(post -> boardUtil.postToSimpleDTO(post, getCommentCount(post.getId()), getLikeCount(post.getId())));
    }

    // 인기글 목록 가져오기
    public List<SimplePostDTO> getFeaturedPosts() {
        List<Post> featuredPosts = postRepository.findByIsFeaturedIsTrue();
        return featuredPosts.stream().map(post -> boardUtil.postToSimpleDTO(post, getCommentCount(post.getId()), getLikeCount(post.getId()))).collect(Collectors.toList());
    }

    // 게시글 검색
    public Page<SimplePostDTO> searchPost(String type, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts;
        switch (type) {
            case "제목" -> posts = postRepository.findByTitleContaining(query, pageable);
            case "제목내용" -> posts = postRepository.findByTitleContainingOrContentContaining(query, query, pageable);
            case "작성자" -> posts = postRepository.findByUser_farmNameContaining(query, pageable);
            default -> throw new IllegalArgumentException("잘못된 검색 타입입니다: " + type);
        }
        return posts.map(post -> boardUtil.postToSimpleDTO(post, getCommentCount(post.getId()), getLikeCount(post.getId())));
    }

    // 게시글 쓰기
    public PostDTO writePost(PostReqDTO postReqDTO) {
        Post post = postRepository.save(boardUtil.postReqDTOToPost(postReqDTO));
        return boardUtil.postToDTO(post, getLikeCount(post.getId()), null, false);
    }

    // 게시글 진입
    public PostDTO getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        boolean isLiked = userId != null && isPostLiked(postId, userId);


        return boardUtil.postToDTO(post, getLikeCount(postId), getCommentList(postId, userId), isLiked);
    }

    // 게시글 수정
    @Transactional
    public PostDTO updatePost(Long postId, CustomUserDetails userDetails, PostDTO postDTO) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        Long userId = post.getUser().getId();

        if (!userId.equals(userDetails.getUserDTO().getUserId())) {
            throw new IllegalArgumentException("게시글 작성자가 아닙니다.");
        }

        post.updatePost(postDTO);
        return boardUtil.postToDTO(post, getLikeCount(postId), getCommentList(postId, userId), isPostLiked(postId, userDetails.getUserDTO().getUserId()));
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId, CustomUserDetails userDetails) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        if (!post.getUser().getId().equals(userDetails.getUserDTO().getUserId())) {
            throw new IllegalArgumentException("게시글 작성자가 아닙니다.");
        }

        postLikeRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
    }

    // 게시글 추천, 추천 취소
    public void likePost(Long postId, CustomUserDetails userDetails) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        Users user = userRepository.findById(userDetails.getUserDTO().getUserId()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userDetails.getUserDTO().getUserId()));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userDetails.getUserDTO().getUserId());

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
        } else {
            PostLike postLike = PostLike.builder().post(post).user(user).build();

            postLikeRepository.save(postLike);
        }
    }

    // 유저의 해당 글 추천 여부 확인
    public boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    // 해당 글의 댓글 수 조회
    public int getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    // 해당 글의 추천 수 조회
    public int getLikeCount(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    // 해당 글의 댓글 조회
    List<CommentDTO> getCommentList(Long postId, Long userId) {

        return commentRepository.findByCommentList(postId).stream().map(comment -> {
            boolean userLike = userId != null && commentService.isCommentLiked(comment.getId(), userId);
            return boardUtil.commentToDTO(comment, commentService.getCommentLikeCount(comment.getId()), userLike);
        }).toList();
    }

    // 인기글 등록
    // 1개월 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개
    @Transactional
    @Scheduled(fixedRate = 60000 * 60)
    public void isPostFeatured() {
        postRepository.resetFeaturedPosts();
        List<Post> featuredPosts = postRepository.findFeaturedPosts();

        if (featuredPosts.isEmpty()) {
            return;
        }

        List<Long> featuredPostIds = featuredPosts.stream().map(Post::getId).toList();
        postRepository.updateFeaturedStatus(featuredPostIds);

        for (Post post : featuredPosts) {
            Long postUserId = post.getUser().getId();
            Long postId = post.getId();

            notificationService.send(postUserId, notificationUtil.createEventDTO(NotificationType.POST_FEATURED, "🎉 글이 인기글로 선정되었습니다!", "http://localhost:3000/board/" + postId));
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
                Optional<Cookie> existingCookie = Arrays.stream(cookies).filter(cookie -> "post_views".equals(cookie.getName())).findFirst();

                if (existingCookie.isPresent()) {
                    try {
                        // Base64 디코딩 후 파싱
                        String jsonValue = new String(Base64.getDecoder().decode(existingCookie.get().getValue()));
                        viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<List<Long>>() {
                        });
                        log.info("기존 쿠키에서 읽은 조회 이력: {}", viewedPostIds);
                    } catch (Exception e) {
                        // 기존 쿠키 값이 유효하지 않은 경우 빈 리스트로 시작
                        log.error("쿠키 값 파싱 실패: {}", e.getMessage(), e);
                        viewedPostIds = new ArrayList<>();
                    }
                }
            }

            // 이미 본 게시글이면 추가하지 않음
            if (!viewedPostIds.contains(postId)) {
                viewedPostIds.add(postId);
                log.info("조회 이력에 추가: {}", postId);

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
                log.info("쿠키 업데이트 완료: {}", viewedPostIds);
            } else {
                log.info("이미 조회한 게시글: {}", postId);
            }
        } catch (Exception e) {
            // 쿠키 설정 실패 시 로그만 남기고 계속 진행
            log.error("Failed to update view cookie: {}", e.getMessage(), e);
        }
    }

    private boolean hasViewedPost(Cookie[] cookies, Long postId) {
        if (cookies == null) return false;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return Arrays.stream(cookies).filter(cookie -> "post_views".equals(cookie.getName())).anyMatch(cookie -> {
                try {
                    // Base64 디코딩 후 파싱
                    String jsonValue = new String(Base64.getDecoder().decode(cookie.getValue()));
                    List<Long> viewedPostIds = objectMapper.readValue(jsonValue, new TypeReference<List<Long>>() {
                    });
                    boolean contains = viewedPostIds.contains(postId);
                    log.info("쿠키 확인: postId={}, 조회이력={}, 포함여부={}", postId, viewedPostIds, contains);
                    return contains;
                } catch (Exception e) {
                    log.error("쿠키 파싱 실패", e);
                    return false;
                }
            });
        } catch (Exception e) {
            log.error("Failed to check viewed posts", e);
            return false;
        }
    }

    public void reportPost(Long postId, CustomUserDetails userDetails, String content) {
        Report report = Report.builder().users(userUtil.DTOToUser(userDetails.getUserDTO())).reportType(ReportType.POST).targetId(postId).content(content).build();

        reportRepository.save(report);
    }
}
