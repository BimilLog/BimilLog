package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>PostPopularService</h2>
 * <p>주간/레전드/공지 인기글 및 첫 페이지 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>모든 캐시가 JSON LIST 단일 구조로 통일되어 getAll(key) → 바로 반환합니다.</p>
 * <p>Redis 장애 시 DB 폴백합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class PostPopularService {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final PostUtil postUtil;

    /**
     * 주간, 레전드, 공지 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPosts(Pageable pageable, String redisKey) {
        try {
            List<PostSimpleDetail> posts = redisPostJsonListAdapter.getAll(redisKey);
            if (!posts.isEmpty()) {
                return postUtil.paginate(posts, pageable);
            }
            return Page.empty();
        } catch (Exception e) {
            log.error("[REDIS_FALLBACK] {} Redis 장애: {}", redisKey, e.getMessage());
            return postQueryRepository.findPostsFallback(pageable, redisKey);
        }
    }
}
