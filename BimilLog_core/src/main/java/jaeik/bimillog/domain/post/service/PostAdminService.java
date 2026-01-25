package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
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
 * <p>공지 상태 조회: 게시글의 현재 공지 여부 확인</p>
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

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
     * <p>일반 게시글이면 공지로 설정하고, 공지 게시글이면 일반으로 해제합니다.</p>
     * <p>Tier1 Hash에 직접 추가/제거합니다 (Tier2 제거됨).</p>
     *
     * @param postId 공지 토글할 게시글 ID
     */
    @Transactional
    public void togglePostNotice(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (post.isNotice()) {
            // 공지 해제: DB 상태 변경 + Tier1 Hash에서 제거
            post.unsetAsNotice();
            redisSimplePostAdapter.removePostFromCache(PostCacheFlag.NOTICE, postId);
            log.info("공지 해제 완료: postId={}", postId);
        } else {
            // 공지 설정: DB 상태 변경 + Tier1 Hash에 추가
            post.setAsNotice();

            // PostSimpleDetail 조회 후 캐시에 추가
            List<PostSimpleDetail> details = postQueryRepository.findPostSimpleDetailsByIds(List.of(postId));
            if (!details.isEmpty()) {
                redisSimplePostAdapter.addPostToCache(PostCacheFlag.NOTICE, postId, details.get(0));
            }
            log.info("공지 설정 완료: postId={}", postId);
        }
    }
}