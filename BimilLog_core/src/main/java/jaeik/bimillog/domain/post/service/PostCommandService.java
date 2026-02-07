package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.async.PostReadModelSync;
import jaeik.bimillog.domain.post.controller.PostCommandController;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostReadModelQueryRepository;
import jaeik.bimillog.domain.post.repository.PostReadModelRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>게시글 명령 서비스</h2>
 * <p>게시글 명령 유스케이스의 구현체입니다.</p>
 * <p>게시글 작성, 수정, 삭제 비즈니스 로직 처리</p>
 * <p>익명/회원 게시글 권한 검증</p>
 * <p>캐시 무효화 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Log
public class PostCommandService {
    private final PostRepository postRepository;
    private final PostToMemberAdapter postToMemberAdapter;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final CommentCommandService commentCommandService;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisFirstPagePostAdapter redisFirstPagePostAdapter;
    private final PostReadModelQueryRepository postReadModelQueryRepository;
    private final PostReadModelRepository postReadModelRepository;
    private final PostReadModelSync postReadModelSync;

    /**
     * 첫 페이지 캐시 크기 (20개)
     */
    public static final int FIRST_PAGE_SIZE = 20;

    /**
     * <h3>게시글 작성</h3>
     * <p>새로운 게시글을 생성하고 저장합니다.</p>
     * <p>익명/회원 구분 처리, Post 팩토리 메서드로 엔티티 생성</p>
     *
     * @param memberId   작성자 사용자 ID (null이면 익명 게시글)
     * @param title    게시글 제목
     * @param content  게시글 내용
     * @param password 게시글 비밀번호 (익명 게시글인 경우)
     * @return 저장된 게시글 ID
     */
    @Transactional
    public Long writePost(Long memberId, String title, String content, Integer password) {
        // 입력 검증
        if (memberId == null && password == null || memberId != null && password != null) {
            throw new CustomException(ErrorCode.POST_BLANK_PASSWORD);
        }

        // 기본값은 비회원
        Member member = null;
        String memberName = "익명";

        // 회원일 경우
        if (memberId != null) {
            member = postToMemberAdapter.getMember(memberId);
            memberName = member.getMemberName();
        }

        // 영속화
        Post post = Post.createPost(member, title, content, password);
        post = postRepository.save(post);

        // 비동기로 조회용테이블 갱신
        postReadModelSync.handlePostCreated(post.getId(), post.getTitle(), memberId, memberName, post.getCreatedAt());

        // 첫 페이지 캐시 추가
        try {
            PostSimpleDetail newPostDetail = PostSimpleDetail.createNew(
                    post.getId(), post.getTitle(), post.getCreatedAt(), memberId, memberName);
            redisFirstPagePostAdapter.addNewPost(newPostDetail);
        } catch (Exception e) {
            log.warn("첫 페이지 캐시 추가 실패: postId={}", post.getId());
        }

        return post.getId();
    }


    /**
     * <h3>게시글 수정 (Cache Invalidation 패턴)</h3>
     * <p>기존 게시글의 제목과 내용을 수정하고 캐시를 무효화합니다.</p>
     * <p>작성자 권한 검증 → 게시글 업데이트 → 캐시 무효화</p>
     * <p>{@link PostCommandController}에서 게시글 수정 API 처리 시 호출됩니다.</p>
     *
     * @param memberId  현재 로그인 사용자 ID
     * @param postId  수정할 게시글 ID
     * @param title   새로운 제목
     * @param content 새로운 내용
     */
    @Transactional
    public void updatePost(Long memberId, Long postId, String title, String content, Integer password) {
        // 비회원은 비밀번호를 입력해야만 한다.
        if (memberId == null && password == null) {
            throw new CustomException(ErrorCode.POST_BLANK_PASSWORD);
        }

        // 글 조회
        Post post = postRepository.findById(postId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 비밀번호 검증 회원은 비밀번호가 null이기 때문에 통과한다.
        if (!post.isAuthor(memberId, password)) {
            throw new CustomException(ErrorCode.POST_FORBIDDEN);
        }

        // 글 수정
        post.updatePost(title, content);

        // 비동기로 조회용테이블 갱신
        postReadModelSync.handlePostUpdated(postId, title);

        // 캐시 무효화 - 인기글인 경우 해당 Hash 필드 삭제
        try {
            redisSimplePostAdapter.removePostFromCache(postId);
        } catch (Exception e) {
            log.warn("게시글 {} 캐시 무효화 실패: {}", postId, e.getMessage());
        }

        // 첫 페이지 캐시 수정 (PostReadModel에서 조회 후 제목 반영)
        try {
            postReadModelRepository.findById(postId).ifPresent(readModel -> {
                PostSimpleDetail updatedDetail = PostSimpleDetail.builder()
                        .id(postId)
                        .title(title)  // 새로운 제목 반영
                        .viewCount(readModel.getViewCount())
                        .likeCount(readModel.getLikeCount())
                        .createdAt(readModel.getCreatedAt())
                        .memberId(readModel.getMemberId())
                        .memberName(readModel.getMemberName())
                        .commentCount(readModel.getCommentCount())
                        .build();
                redisFirstPagePostAdapter.updatePost(postId, updatedDetail);
            });
        } catch (Exception e) {
            log.warn("첫 페이지 캐시 수정 실패: postId={}", postId);
        }
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글을 데이터베이스에서 완전히 삭제하고 캐시를 무효화합니다.</p>
     * <p>작성자 권한 검증 → 게시글 삭제 → 캐시 무효화</p>
     * <p>CASCADE로 Comment와 PostLike가 자동 삭제됩니다.</p>
     * <p>{@link PostCommandController}에서 게시글 삭제 API 처리 시 호출됩니다.</p>
     *
     * @param memberId 현재 로그인 사용자 ID
     * @param postId 삭제할 게시글 ID
     */
    @Transactional
    public void deletePost(Long memberId, Long postId, Integer password) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.isAuthor(memberId, password)) {
            throw new CustomException(ErrorCode.POST_FORBIDDEN);
        }

        // CASCADE로 Comment와 PostLike 자동 삭제
        postRepository.delete(post);

        // 모든 관련 캐시 무효화
        try {
            redisRealTimePostAdapter.removePostIdFromRealtimeScore(postId);
            redisSimplePostAdapter.removePostFromCache(postId);
        } catch (Exception e) {
            log.warn("게시글 {} 캐시 무효화 실패: {}", postId, e.getMessage());
        }

        // 첫 페이지 캐시 삭제 (삭제 후 21번째 게시글로 빈 자리 채움)
        try {
            redisFirstPagePostAdapter.deletePost(postId, () -> {
                // 삭제 후 21번째 게시글 조회 (빈 자리 채움)
                List<PostSimpleDetail> posts = postReadModelQueryRepository.findBoardPostsByCursor(null, FIRST_PAGE_SIZE + 2);
                // 21번째 게시글이 있으면 반환 (인덱스 20)
                return posts.size() > FIRST_PAGE_SIZE ? posts.get(FIRST_PAGE_SIZE) : null;
            });
        } catch (Exception e) {
            log.warn("첫 페이지 캐시 삭제 실패: postId={}", postId);
        }
    }

    /**
     * <h3>회원 작성 게시글 일괄 삭제 (라이트 어라운드 패턴)</h3>
     * <p>회원 탈퇴 시 사용자가 작성한 모든 게시글을 삭제하고 캐시를 무효화합니다.</p>
     * <p>FK 제약 조건 위반 방지를 위해 각 게시글의 댓글을 먼저 삭제합니다.</p>
     * <p>삭제 순서: 댓글 삭제 → 게시글 삭제</p>
     *
     * @param memberId 게시글을 삭제할 사용자 ID
     */
    @Transactional
    public void deleteAllPostsByMemberId(Long memberId) {
        List<Long> postIds = postRepository.findIdsWithCacheFlagByMemberId(memberId);
        for (Long postId : postIds) {
            // FK 제약 조건 위반 방지: 게시글의 모든 댓글 먼저 삭제 (CommentClosure 포함)
            commentCommandService.deleteCommentsByPost(postId);
        }
        // 게시글 일괄 삭제
        postRepository.deleteAllByMemberId(memberId);
    }
}
