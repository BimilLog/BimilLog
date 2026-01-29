package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.FeaturedPost;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>게시글 관리자 서비스</h2>
 * <p>게시글 도메인의 관리자 전용 기능을 처리하는 서비스입니다.</p>
 * <p>공지사항 토글: 일반 게시글을 공지로 승격하거나 공지를 일반으로 전환</p>
 * <p>공지 변경 시 NOTICE Hash 캐시에서 해당 field만 추가/삭제합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostAdminService {
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final FeaturedPostRepository featuredPostRepository;

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
     * <p>일반 게시글이면 공지로 설정하고, 공지 게시글이면 일반으로 해제합니다.</p>
     * <p>변경 후 NOTICE Hash 캐시에서 해당 field만 추가/삭제합니다.</p>
     *
     * @param postId 공지 토글할 게시글 ID
     */
    @Transactional
    public void togglePostNotice(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (post.isNotice()) {
            // 공지 해제: DB 상태 변경 + featured_post 삭제 + 캐시 단건 삭제
            post.unsetAsNotice();
            featuredPostRepository.deleteByPostIdAndType(postId, PostCacheFlag.NOTICE);
            removeNoticeCacheEntry(postId);
            log.info("공지 해제 완료: postId={}", postId);
        } else {
            // 공지 설정: DB 상태 변경 + featured_post 저장 + 캐시 단건 추가
            post.setAsNotice();
            FeaturedPost featuredPost = FeaturedPost.createNotice(post);
            featuredPostRepository.save(featuredPost);
            addNoticeCacheEntry(postId);
            log.info("공지 설정 완료: postId={}", postId);
        }
    }

    /**
     * <h3>NOTICE 캐시 단건 추가 (HSET)</h3>
     * <p>DB에서 해당 게시글의 PostSimpleDetail을 조회하여 Hash field로 추가합니다.</p>
     */
    private void addNoticeCacheEntry(Long postId) {
        try {
            Optional<PostSimpleDetail> post = postQueryRepository.findPostSimpleDetailById(postId);
            post.ifPresent(p -> {
                redisSimplePostAdapter.putPostToCache(PostCacheFlag.NOTICE, p);
                log.info("[NOTICE_CACHE] 단건 추가 완료: postId={}", postId);
            });
        } catch (Exception e) {
            log.warn("[NOTICE_CACHE] 단건 추가 실패 (다음 조회 시 DB 폴백): {}", e.getMessage());
        }
    }

    /**
     * <h3>NOTICE 캐시 단건 삭제 (HDEL)</h3>
     * <p>Hash에서 해당 postId field만 삭제합니다.</p>
     */
    private void removeNoticeCacheEntry(Long postId) {
        try {
            redisSimplePostAdapter.removePostFromCache(PostCacheFlag.NOTICE, postId);
            log.info("[NOTICE_CACHE] 단건 삭제 완료: postId={}", postId);
        } catch (Exception e) {
            log.warn("[NOTICE_CACHE] 단건 삭제 실패 (다음 조회 시 DB 폴백): {}", e.getMessage());
        }
    }
}
