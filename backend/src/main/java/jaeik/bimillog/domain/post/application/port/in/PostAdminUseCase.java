package jaeik.bimillog.domain.post.application.port.in;

import jaeik.bimillog.domain.post.exception.PostCustomException;

/**
 * <h2>PostAdminUseCase</h2>
 * <p>
 * Post 도메인의 관리자 전용 비즈니스 로직을 처리하는 인바운드 포트입니다.
 * 헥사고날 아키텍처에서 관리자 권한이 필요한 게시글 관리 기능의 진입점 역할을 합니다.
 * </p>
 * <p>
 * 이 유스케이스는 게시글의 공지사항 관리 기능을 제공합니다:
 * - 공지사항 토글: 일반 게시글을 공지로 승격하거나 공지를 일반으로 전환
 * - 공지 상태 조회: 게시글의 현재 공지 여부 확인
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 콘텐츠 관리 - 중요한 게시글을 공지로 우선 노출
 * 2. 커뮤니티 운영 - 관리자의 효율적인 게시글 분류 관리
 * 3. 사용자 경험 - 공지사항과 일반 게시글의 명확한 구분 제공
 * 4. 캐시 무결성 - 공지 상태 변경 시 관련 캐시 동기화 지원
 * </p>
 * <p>
 * AdminCommandController에서 공지 관리 API 제공 시 호출됩니다.
 * PostCacheService에서 공지사항 캐시 동기화 시 상태 확인을 위해 호출됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostAdminUseCase {

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
     * <p>게시글의 공지사항 상태를 현재 상태의 반대로 변경합니다.</p>
     * <p>일반 게시글이면 공지로 설정하고, 공지 게시글이면 일반으로 해제합니다.</p>
     * <p>AdminCommandController에서 관리자의 공지 관리 요청 시 호출됩니다.</p>
     * <p>공지 상태 변경 후 PostCacheService에서 캐시 동기화를 위해 상태 확인이 이어집니다.</p>
     *
     * @param postId 공지 상태를 토글할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    void togglePostNotice(Long postId);

    /**
     * <h3>게시글 공지사항 상태 조회</h3>
     * <p>특정 게시글의 현재 공지사항 설정 여부를 확인합니다.</p>
     * <p>게시글 엔티티의 isNotice 플래그를 확인하여 boolean 값으로 반환합니다.</p>
     * <p>PostCacheService에서 공지 토글 후 변경된 상태를 확인할 때 호출됩니다.</p>
     * <p>AdminCommandController에서 공지 토글 응답에 변경된 상태를 포함할 때 호출됩니다.</p>
     *
     * @param postId 공지 상태를 확인할 게시글 ID
     * @return boolean 공지사항 설정 여부 (true: 공지사항, false: 일반 게시글)
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isPostNotice(Long postId);
}