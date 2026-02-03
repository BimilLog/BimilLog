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
 * <p>공지사항 토글: FeaturedPost(type=NOTICE) 테이블로만 공지사항 관리</p>
 * <p>공지 변경 시 NOTICE Hash 캐시에서 해당 field만 추가/삭제합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
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
     * <p>FeaturedPost(type=NOTICE) 테이블로 공지사항 여부를 관리합니다.</p>
     * <p>변경 후 NOTICE Hash 캐시에서 해당 field만 추가/삭제합니다.</p>
     *
     * @param postId 공지 토글할 게시글 ID
     */
    @Transactional
    public void togglePostNotice(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        try {
            boolean isCurrentlyNotice = featuredPostRepository.existsByPostIdAndType(postId, PostCacheFlag.NOTICE);

            if (isCurrentlyNotice) {
                // 공지 해제: featured_post 삭제 + 캐시 단건 삭제
                featuredPostRepository.deleteByPostIdAndType(postId, PostCacheFlag.NOTICE);
                redisSimplePostAdapter.removePostFromCache(PostCacheFlag.NOTICE, postId);
            } else {
                // 공지 설정: featured_post 저장 + 캐시 단건 추가
                Optional<PostSimpleDetail> detail = postQueryRepository.findPostSimpleDetailById(postId);
                detail.ifPresent(d -> {
                    FeaturedPost featuredPost = FeaturedPost.createFeaturedPost(d, PostCacheFlag.NOTICE);
                    featuredPostRepository.save(featuredPost);
                    redisSimplePostAdapter.putPostToCache(PostCacheFlag.NOTICE, d);
                });
            }
        } catch (Exception e) {
            log.error("공지 설정/해제 중 오류 발생: postId={}", postId, e);
        }
    }
}
