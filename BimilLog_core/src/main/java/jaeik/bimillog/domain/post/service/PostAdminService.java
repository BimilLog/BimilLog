package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>게시글 관리자 서비스</h2>
 * <p>게시글 도메인의 관리자 전용 기능을 처리하는 서비스입니다.</p>
 * <p>공지사항 토글: Post.featuredType 필드로 직접 관리</p>
 * <p>공지 변경 시 글 단위 Hash 생성 + SET 인덱스 추가/제거로 캐시를 반영합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostAdminService {
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final RedisPostHashAdapter redisPostHashAdapter;
    private final RedisPostIndexAdapter redisPostIndexAdapter;

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
     * <p>Post.featuredType 필드로 공지사항 여부를 직접 관리합니다.</p>
     * <p>공지 설정: 글 단위 Hash 생성 + SET 인덱스 SADD</p>
     * <p>공지 해제: SET 인덱스 SREM (글 단위 Hash는 삭제하지 않음 - 다른 목록에서 참조 가능)</p>
     *
     * @param postId 공지 토글할 게시글 ID
     */
    @Transactional
    public void togglePostNotice(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        try {
            boolean isCurrentlyNotice = post.getFeaturedType() == PostCacheFlag.NOTICE;

            if (isCurrentlyNotice) {
                // 공지 해제: featuredType null로 + SET 인덱스에서 제거
                post.updateFeaturedType(null);
                redisPostIndexAdapter.removeFromIndex(RedisKey.POST_NOTICE_IDS_KEY, postId);
            } else {
                // 공지 설정: featuredType NOTICE로 + 글 단위 Hash 생성 + SET 인덱스 추가
                post.updateFeaturedType(PostCacheFlag.NOTICE);
                Optional<PostSimpleDetail> detail = postQueryRepository.findPostSimpleDetailById(postId);
                detail.ifPresent(d -> {
                    redisPostHashAdapter.createPostHash(d);
                    redisPostIndexAdapter.addToIndex(RedisKey.POST_NOTICE_IDS_KEY, postId);
                });
            }
        } catch (Exception e) {
            log.error("공지 설정/해제 중 오류 발생: postId={}", postId, e);
        }
    }
}
