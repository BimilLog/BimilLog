package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostAdminUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
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
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostAdminService implements PostAdminUseCase {

    private final PostQueryPort postQueryPort;
    private final PostCommandPort postCommandPort;

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
     * <p>게시글의 공지사항 상태를 현재 상태의 반대로 변경합니다.</p>
     * <p>일반 게시글이면 공지로 설정하고, 공지 게시글이면 일반으로 해제합니다.</p>
     * <p>{@link PostAdminController}에서 관리자 공지 토글 요청 시 호출됩니다.</p>
     *
     * @param postId 공지 토글할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void togglePostNotice(Long postId) {
        Post post = postQueryPort.findById(postId);
        
        if (post.isNotice()) {
            post.unsetAsNotice();
            log.info("공지사항 해제: postId={}, title={}", postId, post.getTitle());
        } else {
            post.setAsNotice();
            log.info("공지사항 설정: postId={}, title={}", postId, post.getTitle());
        }
        
        postCommandPort.save(post);
        log.info("게시글 공지 토글 DB 업데이트 완료: postId={}, isNotice={}", postId, post.isNotice());
    }

    /**
     * <h3>게시글 공지사항 상태 조회 비즈니스 로직 실행</h3>
     * <p>PostAdminUseCase 인터페이스의 공지 상태 조회 기능을 구현하며, 캐시 동기화를 위한 현재 상태 확인 규칙을 적용합니다.</p>
     * <p>게시글의 현재 공지사항 여부를 조회하여 캐시 동기화 로직에서 정확한 상태 판단을 가능하게 합니다.</p>
     * <p>Post 엔티티의 isNotice 메서드를 통해 도메인 규칙에 따른 상태 조회를 수행합니다.</p>
     * <p>PostCacheService에서 캐시 동기화 시 현재 공지 상태 확인을 위해 호출됩니다.</p>
     *
     * @param postId 상태를 확인할 게시글 ID
     * @return boolean 공지사항 여부 (true: 공지사항, false: 일반 게시글)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isPostNotice(Long postId) {
        Post post = postQueryPort.findById(postId);
        
        boolean isNotice = post.isNotice();
        log.debug("게시글 공지 상태 조회: postId={}, isNotice={}", postId, isNotice);
        return isNotice;
    }
}