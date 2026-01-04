package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.global.out.GlobalPostQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.controller.PostCommandController;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.out.PostRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisDetailPostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier1PostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
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
public class PostCommandService {
    private final PostRepository postRepository;
    private final GlobalPostQueryAdapter globalPostQueryAdapter;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;
    private final RedisDetailPostStoreAdapter redisDetailPostStoreAdapter;
    private final RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;
    private final RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;
    private final CommentCommandService commentCommandService;
    private final RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;

    /**
     * <h3>게시글 작성</h3>
     * <p>새로운 게시글을 생성하고 저장합니다.</p>
     * <p>익명/회원 구분 처리, Post 팩토리 메서드로 엔티티 생성</p>
     * <p>{@link PostCommandController}에서 게시글 작성 API 처리 시 호출됩니다.</p>
     *
     * @param memberId   작성자 사용자 ID (null이면 익명 게시글)
     * @param title    게시글 제목
     * @param content  게시글 내용
     * @param password 게시글 비밀번호 (익명 게시글인 경우)
     * @return 저장된 게시글 ID
     */
    @Transactional
    public Long writePost(Long memberId, String title, String content, Integer password) {
        Member member = (memberId != null) ? globalMemberQueryAdapter.getReferenceById(memberId) : null;
        Post newPost = Post.createPost(member, title, content, password);
        Post savedPost = postRepository.save(newPost);
        return savedPost.getId();
    }


    /**
     * <h3>게시글 수정 (라이트 어라운드 패턴)</h3>
     * <p>기존 게시글의 제목과 내용을 수정하고 모든 관련 캐시를 무효화합니다.</p>
     * <p>작성자 권한 검증 → 게시글 업데이트 → 캐시 무효화 (다음 조회 시 캐시 어사이드로 재생성)</p>
     * <p>{@link PostCommandController}에서 게시글 수정 API 처리 시 호출됩니다.</p>
     *
     * @param memberId  현재 로그인 사용자 ID
     * @param postId  수정할 게시글 ID
     * @param title   새로운 제목
     * @param content 새로운 내용
     */
    @Transactional
    public void updatePost(Long memberId, Long postId, String title, String content, Integer password) {
        Post post = globalPostQueryAdapter.findById(postId);

        if (!post.isAuthor(memberId, password)) {
            throw new CustomException(ErrorCode.POST_FORBIDDEN);
        }

        post.updatePost(title, content);

        // 모든 관련 캐시 무효화
        try {
            redisDetailPostStoreAdapter.deleteSinglePostCache(postId);
            redisTier1PostStoreAdapter.removePostFromListCache(postId);
        } catch (Exception e) {
            log.warn("게시글 {} 캐시 무효화 실패: {}", postId, e.getMessage());
        }

        log.info("게시글 수정 완료: postId={}, memberId={}, title={}", postId, memberId, title);
    }

    /**
     * <h3>게시글 삭제 (라이트 어라운드 패턴)</h3>
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
        Post post = globalPostQueryAdapter.findById(postId);

        if (!post.isAuthor(memberId, password)) {
            throw new CustomException(ErrorCode.POST_FORBIDDEN);
        }

        String postTitle = post.getTitle();

        // CASCADE로 Comment와 PostLike 자동 삭제
        postRepository.delete(post);

        // 모든 관련 캐시 무효화
        try {
            redisDetailPostStoreAdapter.deleteSinglePostCache(postId);
            redisRealTimePostStoreAdapter.removePostIdFromRealtimeScore(postId);
            redisTier1PostStoreAdapter.removePostFromListCache(postId);
            redisTier2PostStoreAdapter.removePostIdFromStorage(postId);
        } catch (Exception e) {
            log.warn("게시글 {} 캐시 무효화 실패: {}", postId, e.getMessage());
        }

        log.info("게시글 삭제 완료: postId={}, memberId={}, title={}", postId, memberId, postTitle);
    }

    /**
     * <h3>회원 작성 게시글 일괄 삭제 (라이트 어라운드 패턴)</h3>
     * <p>회원 탈퇴 시 사용자가 작성한 모든 게시글을 삭제하고 캐시를 무효화합니다.</p>
     * <p>FK 제약 조건 위반 방지를 위해 각 게시글의 댓글을 먼저 삭제합니다.</p>
     * <p>삭제 순서: 댓글 삭제 → 캐시 무효화 → 게시글 삭제</p>
     *
     * @param memberId 게시글을 삭제할 사용자 ID
     */
    @Transactional
    public void deleteAllPostsByMemberId(Long memberId) {
        List<Long> postIds = postRepository.findIdsWithCacheFlagByMemberId(memberId);;
        for (Long postId : postIds) {
            // FK 제약 조건 위반 방지: 게시글의 모든 댓글 먼저 삭제 (CommentClosure 포함)
            commentCommandService.deleteCommentsByPost(postId);
            // 캐시 무효화
            try {
                redisDetailPostStoreAdapter.deleteSinglePostCache(postId);
            } catch (Exception e) {
                log.warn("게시글 {} 캐시 무효화 실패: {}", postId, e.getMessage());
            }
        }
        // 게시글 일괄 삭제
        postRepository.deleteAllByMemberId(memberId);
    }
}
