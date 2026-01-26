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

import java.util.List;

/**
 * <h2>게시글 관리자 서비스</h2>
 * <p>게시글 도메인의 관리자 전용 기능을 처리하는 서비스입니다.</p>
 * <p>공지사항 토글: 일반 게시글을 공지로 승격하거나 공지를 일반으로 전환</p>
 * <p>공지 변경 시 NOTICE 전체 캐시를 재구성합니다 (영구 TTL).</p>
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
     * <p>변경 후 NOTICE 전체 캐시를 재구성합니다 (영구 TTL).</p>
     *
     * @param postId 공지 토글할 게시글 ID
     */
    @Transactional
    public void togglePostNotice(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (post.isNotice()) {
            // 공지 해제: DB 상태 변경 + featured_post 삭제
            post.unsetAsNotice();
            featuredPostRepository.deleteByPostIdAndType(postId, PostCacheFlag.NOTICE);
            log.info("공지 해제 완료: postId={}", postId);
        } else {
            // 공지 설정: DB 상태 변경 + featured_post 저장
            post.setAsNotice();
            FeaturedPost featuredPost = FeaturedPost.createNotice(post);
            featuredPostRepository.save(featuredPost);
            log.info("공지 설정 완료: postId={}", postId);
        }

        // NOTICE 전체 캐시 재구성 (영구 TTL)
        refreshNoticeCache();
    }

    /**
     * <h3>NOTICE 전체 캐시 재구성</h3>
     * <p>featured_post에서 NOTICE 타입의 모든 postId를 조회하여 캐시를 전체 교체합니다.</p>
     * <p>공지가 없으면 Hash 키를 삭제합니다.</p>
     * <p>TTL은 null (영구 저장)입니다.</p>
     */
    private void refreshNoticeCache() {
        try {
            List<Long> postIds = featuredPostRepository.findPostIdsByType(PostCacheFlag.NOTICE);

            if (postIds.isEmpty()) {
                redisSimplePostAdapter.deleteHash(PostCacheFlag.NOTICE);
                log.info("[NOTICE_CACHE] 공지 없음 - Hash 삭제 완료");
                return;
            }

            List<PostSimpleDetail> posts = postQueryRepository.findPostSimpleDetailsByIds(postIds);
            redisSimplePostAdapter.cachePostsWithTtl(PostCacheFlag.NOTICE, posts, null);
            log.info("[NOTICE_CACHE] 전체 캐시 재구성 완료: {}건 (영구 TTL)", posts.size());
        } catch (Exception e) {
            log.warn("[NOTICE_CACHE] 캐시 재구성 실패 (다음 조회 시 DB 폴백): {}", e.getMessage());
        }
    }
}
