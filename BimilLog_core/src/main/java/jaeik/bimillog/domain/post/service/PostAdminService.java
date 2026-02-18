package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>게시글 관리자 서비스</h2>
 * <p>게시글 도메인의 관리자 전용 기능을 처리하는 서비스입니다.</p>
 * <p>공지사항 토글: Post.isNotice 플래그로 직접 관리</p>
 * <p>공지 변경 시 JSON LIST에 추가/제거로 캐시를 반영합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostAdminService {
    private final PostRepository postRepository;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final RedisPostCounterAdapter redisPostCounterAdapter;

    private static final int NOTICE_MAX_SIZE = 100;

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
     * <p>Post.isNotice 플래그로 공지사항 여부를 직접 관리합니다.</p>
     * <p>공지 설정: JSON LIST에 LPUSH (최신이 맨 앞)</p>
     * <p>공지 해제: JSON LIST에서 LREM</p>
     *
     * @param postId 공지 토글할 게시글 ID
     */
    @Transactional
    public void togglePostNotice(Long postId, boolean isNotice) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.updateNotice(!isNotice);

        if (isNotice) {
            releaseNotice(postId);
            return;
        }
        registerNotice(post);
    }

    /**
     * <h3>공지 해제</h3>
     * JSON LIST에서 제거 + 공지 SET에서 제거
     */
    private void releaseNotice(Long postId) {
        try {
            redisPostJsonListAdapter.removePost(RedisKey.POST_NOTICE_JSON_KEY, postId);
            redisPostCounterAdapter.removeFromCategorySet(RedisKey.CACHED_NOTICE_IDS_KEY, postId);
        } catch (Exception e) {
            log.error("공지 해제 중 오류 발생: postId={}", postId, e);
        }
    }

    /**
     * <h3>공지 설정</h3>
     * JSON LIST에 LPUSH + 공지 SET에 추가 + 카운터 초기화
     */
    private void registerNotice(Post post) {
        PostSimpleDetail from = PostSimpleDetail.from(post);
        try {
            redisPostJsonListAdapter.addNewPost(RedisKey.POST_NOTICE_JSON_KEY, PostCacheEntry.from(from), NOTICE_MAX_SIZE);
            redisPostCounterAdapter.addToCategorySet(RedisKey.CACHED_NOTICE_IDS_KEY, post.getId());
            redisPostCounterAdapter.batchSetCounters(List.of(from));
        } catch (Exception e) {
            log.error("공지 설정 중 오류 발생: postId={}", post.getId(), e);
        }
    }
}
