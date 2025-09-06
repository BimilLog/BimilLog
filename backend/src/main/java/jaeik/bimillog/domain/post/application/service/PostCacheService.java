package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheSyncPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>게시글 캐시 서비스</h2>
 * <p>PostCacheUseCase의 구현체로, 게시글 캐시 관리 비즈니스 로직을 처리합니다.</p>
 * <p>공지사항 설정/해제 시 캐시 동기화를 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostCacheService implements PostCacheUseCase {

    private final PostCacheCommandPort postCacheCommandPort;
    private final PostCacheSyncPort postCacheSyncPort;

    /**
     * {@inheritDoc}
     * 
     * <p>DB에서 게시글 상세 정보를 조회한 후 공지사항 캐시에 추가합니다.</p>
     * <p>캐시 추가 실패 시 예외가 발생하지만, 이벤트 리스너에서 처리됩니다.</p>
     */
    @Override
    public void addNoticeToCache(Long postId) {
        log.info("공지사항 캐시 추가 시작: postId={}", postId);
        
        // 게시글 상세 정보를 DB에서 조회
        PostDetail postDetail = postCacheSyncPort.findPostDetail(postId);
        if (postDetail != null) {
            // 단건을 리스트로 감싸서 캐시에 추가
            postCacheCommandPort.cachePostsWithDetails(PostCacheFlag.NOTICE, List.of(postDetail));
            log.info("공지사항 캐시 추가 완료: postId={}", postId);
        } else {
            log.warn("공지사항 캐시 추가 실패 - 게시글을 찾을 수 없음: postId={}", postId);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>공지사항 캐시에서만 특정 게시글을 제거합니다.</p>
     * <p>성능 최적화를 위해 공지사항 캐시에서만 삭제합니다.</p>
     */
    @Override
    public void removeNoticeFromCache(Long postId) {
        log.info("공지사항 캐시 제거 시작: postId={}", postId);
        
        // 공지 캐시에서만 삭제 (성능 최적화)
        postCacheCommandPort.deleteCache(null, postId, PostCacheFlag.NOTICE);
        
        log.info("공지사항 캐시 제거 완료: postId={}", postId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>게시글의 공지 상태에 따라 캐시를 동기화합니다.</p>
     * <p>공지 설정 시 캐시에 추가, 공지 해제 시 캐시에서 제거하여 상태를 일치시킵니다.</p>
     */
    @Override
    public void syncNoticeCache(Long postId, boolean isNotice) {
        if (isNotice) {
            log.info("공지사항 캐시 추가 동기화 시작: postId={}", postId);
            addNoticeToCache(postId);
        } else {
            log.info("공지사항 캐시 제거 동기화 시작: postId={}", postId);
            removeNoticeFromCache(postId);
        }
    }
}