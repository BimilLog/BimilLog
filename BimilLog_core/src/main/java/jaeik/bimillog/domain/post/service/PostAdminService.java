package jaeik.bimillog.domain.post.service;

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

import java.util.Optional;

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
    private final PostQueryRepository postQueryRepository;
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
    public void togglePostNotice(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        try {
            if (post.isNotice()) {
                // 공지 해제: isNotice false + JSON LIST에서 제거
                post.updateNotice(false);
                redisPostJsonListAdapter.removePost(RedisKey.POST_NOTICE_JSON_KEY, postId);
            } else {
                // 공지 설정: isNotice true + JSON LIST에 LPUSH
                post.updateNotice(true);
                Optional<PostSimpleDetail> detail = postQueryRepository.findPostSimpleDetailById(postId);
                detail.ifPresent(d -> {
                    redisPostJsonListAdapter.addNewPost(RedisKey.POST_NOTICE_JSON_KEY, d, NOTICE_MAX_SIZE);
                    redisPostCounterAdapter.addCachedPostId(postId);
                });
            }
        } catch (Exception e) {
            log.error("공지 설정/해제 중 오류 발생: postId={}", postId, e);
        }
    }
}
