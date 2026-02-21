package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListDeleteAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 관리자 서비스</h2>
 * <p>게시글 도메인의 관리자 전용 기능을 처리하는 서비스입니다.</p>
 * <p>공지사항 토글: Post.isNotice 플래그로 직접 관리</p>
 * <p>공지 변경 시 JSON LIST에 추가/제거로 캐시를 반영합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostAdminService {
    private final PostRepository postRepository;
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final RedisPostListDeleteAdapter redisPostListDeleteAdapter;

    private static final int NOTICE_MAX_SIZE = 100;

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
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

    private void releaseNotice(Long postId) {
        try {
            redisPostListDeleteAdapter.removePost(postId);
        } catch (Exception e) {
            log.error("공지 해제 중 오류 발생: postId={}", postId, e);
        }
    }

    private void registerNotice(Post post) {
        try {
            PostSimpleDetail detail = PostSimpleDetail.from(post);
            redisPostListUpdateAdapter.addPostToList(
                    RedisKey.POST_NOTICE_JSON_KEY, detail, NOTICE_MAX_SIZE);
        } catch (Exception e) {
            log.error("공지 설정 중 오류 발생: postId={}", post.getId(), e);
        }
    }
}
