package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>PostCacheService</h2>
 * <p>게시글 캐시 관리 관련 UseCase 인터페이스의 구현체로서 캐시 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>공지사항 상태 변경에 따른 Redis 캐시 동기화와 데이터 무결성 보장을 위한 비즈니스 규칙을 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService implements PostCacheUseCase {

    private final RedisPostCommandPort redisPostCommandPort;
    private final PostQueryPort postQueryPort;


    /**
     * <h3>공지사항 캐시 동기화 비즈니스 로직 실행</h3>
     * <p>PostCacheUseCase 인터페이스의 캐시 동기화 기능을 구현하며, 공지사항 상태 변경에 따른 캐시 일관성 유지 규칙을 적용합니다.</p>
     * <p>공지사항 설정 시 Redis 캐시에 게시글을 추가하고, 공지사항 해제 시 캐시에서 제거하여 DB와 캐시 간 동기화를 보장합니다.</p>
     * <p>캐시 무효화와 재생성을 통해 사용자에게 정확한 공지사항 목록을 실시간으로 제공합니다.</p>
     * <p>PostAdminController에서 공지사항 토글 완료 후 호출됩니다.</p>
     *
     * @param postId 동기화할 게시글 ID
     * @param isNotice 공지사항 여부 (true: 캐시 추가, false: 캐시 제거)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void syncNoticeCache(Long postId, boolean isNotice) {
        if (isNotice) {
            log.info("공지사항 캐시 추가 동기화 시작: postId={}", postId);
            addNoticeToCache(postId);
        } else {
            log.info("공지사항 캐시 제거 동기화 시작: postId={}", postId);
            removeNoticeFromCache(postId);
        }
    }

    /**
     * <h3>공지사항 캐시 추가 로직</h3>
     * <p>DB에서 게시글 정보를 조회하여 Redis 공지사항 캐시에 postId만 추가합니다.</p>
     * <p>주간/레전드 인기글과 동일한 캐싱 전략으로 메모리 효율성을 높입니다.</p>
     * <p>상세 정보는 조회 시 캐시 어사이드 패턴으로 처리됩니다.</p>
     * <p>syncNoticeCache 메서드에서 공지사항 설정 시 호출됩니다.</p>
     *
     * @param postId 캐시에 추가할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void addNoticeToCache(Long postId) {
        log.info("공지사항 캐시 추가 시작: postId={}", postId);

        // 게시글 정보를 DB에서 조회 (PostSearchResult로 조회)
        PostDetail postDetail = postQueryPort.findPostDetailWithCounts(postId, null).orElse(null);
        if (postDetail != null) {
            // postId만 캐시에 추가 (주간/레전드와 동일한 방식)
            redisPostCommandPort.cachePostIds(PostCacheFlag.NOTICE, List.of(postDetail.toSearchResult()));
            log.info("공지사항 캐시 추가 완료: postId={}", postId);
        } else {
            log.warn("공지사항 캐시 추가 실패 - 게시글을 찾을 수 없음: postId={}", postId);
        }
    }

    /**
     * <h3>공지사항 캐시 제거 로직</h3>
     * <p>Redis 공지사항 캐시에서 지정된 게시글을 제거합니다.</p>
     * <p>성능 최적화를 위해 공지사항 캐시에서만 선별적으로 삭제하여 불필요한 캐시 작업을 방지합니다.</p>
     * <p>syncNoticeCache 메서드에서 공지사항 해제 시 호출됩니다.</p>
     *
     * @param postId 캐시에서 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void removeNoticeFromCache(Long postId) {
        log.info("공지사항 캐시 제거 시작: postId={}", postId);

        // 공지 캐시에서만 삭제
        redisPostCommandPort.deleteCache(null, postId, PostCacheFlag.NOTICE);

        log.info("공지사항 캐시 제거 완료: postId={}", postId);
    }
}