package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.out.GlobalPostQueryAdapter;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.redis.post.RedisPostDeleteAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostSaveAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 관리자 서비스</h2>
 * <p>게시글 도메인의 관리자 전용 기능을 처리하는 서비스입니다.</p>
 * <p>공지사항 토글: 일반 게시글을 공지로 승격하거나 공지를 일반으로 전환</p>
 * <p>공지 상태 조회: 게시글의 현재 공지 여부 확인</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostAdminService {

    private final GlobalPostQueryAdapter globalPostQueryAdapter;
    private final RedisPostSaveAdapter redisPostSaveAdapter;
    private final RedisPostDeleteAdapter redisPostDeleteAdapter;

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
     * <p>일반 게시글이면 공지로 설정하고, 공지 게시글이면 일반으로 해제합니다.</p>
     * <p>postIds 영구 저장소에 단일 게시글만 추가/제거합니다.</p>
     *
     * @param postId 공지 토글할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void togglePostNotice(Long postId) {
        Post post = globalPostQueryAdapter.findById(postId);

        if (post.isNotice()) {
            post.unsetAsNotice();
            redisPostDeleteAdapter.removePostIdFromStorage(postId);
            redisPostDeleteAdapter.removePostFromListCache(postId);
            log.info("공지사항 해제: postId={}, title={}", postId, post.getTitle());
        } else {
            post.setAsNotice();
            redisPostSaveAdapter.addPostIdToStorage(PostCacheFlag.NOTICE, postId);
            log.info("공지사항 설정: postId={}, title={}", postId, post.getTitle());
        }
    }
}