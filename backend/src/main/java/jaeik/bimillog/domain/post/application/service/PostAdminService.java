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
 * <h2>PostAdminService</h2>
 * <p>
 * 게시글 관리자 관련 UseCase 인터페이스의 구체적 구현체로서 관리 권한 비즈니스 로직을 오케스트레이션합니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 게시글 도메인의 관리자 기능 처리를 담당하며, 공지사항 설정/해제와 같은
 * 특별한 관리 권한이 필요한 비즈니스 규칙을 관리합니다.
 * </p>
 * <p>
 * 트랜잭션 경계를 설정하여 공지사항 상태 변경의 원자성을 보장하고, 순수한 도메인 로직에 집중하여
 * 캐시 동기화와 같은 인프라스트럭처 관심사는 Controller 레이어에 위임합니다.
 * </p>
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
     * <h3>게시글 공지사항 토글 비즈니스 로직 실행</h3>
     * <p>PostAdminUseCase 인터페이스의 공지사항 토글 기능을 구현하며, 관리자 권한 게시글 상태 변경 규칙을 적용합니다.</p>
     * <p>현재 공지사항이면 일반 게시글로 해제하고, 일반 게시글이면 공지사항으로 설정하는 토글 로직을 수행합니다.</p>
     * <p>Post 엔티티의 공지 관련 메서드를 사용하여 도메인 규칙에 따른 상태 변경을 처리합니다.</p>
     * <p>PostAdminController에서 관리자 공지 토글 요청 시 호출됩니다.</p>
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