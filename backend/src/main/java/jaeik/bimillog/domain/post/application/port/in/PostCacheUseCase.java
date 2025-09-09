package jaeik.bimillog.domain.post.application.port.in;

/**
 * <h2>PostCacheUseCase</h2>
 * <p>
 * Post 도메인의 캐시 데이터 동기화 비즈니스 로직을 처리하는 인바운드 포트입니다.
 * 헥사고날 아키텍처에서 공지사항과 관련된 캐시 데이터의 일관성을 보장하는 역할을 합니다.
 * </p>
 * <p>
 * 이 유스케이스는 공지사항 상태 변경에 따른 캐시 동기화를 처리합니다:
 * - 공지 설정 시: 공지사항 리스트 캐시에 추가
 * - 공지 해제 시: 공지사항 리스트 캐시에서 제거
 * - 캐시 무결성: Redis에 저장된 공지사항과 DB 데이터의 일관성 유지
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 데이터 일관성 - DB와 Redis 캐시 간의 공지사항 데이터 동기화
 * 2. 성능 최적화 - 공지사항 조회 시 빠른 캐시 데이터 제공
 * 3. 반응성 향상 - 공지 상태 변경이 즉시 사용자에게 반영
 * 4. 사용자 경험 - 공지사항 목록의 일관된 표시
 * </p>
 * <p>
 * PostAdminService에서 공지 토글 완료 후 캐시 동기화를 위해 호출됩니다.
 * AdminCommandController에서 공지 관리 작업 후 캐시 무결성 보장을 위해 호출됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCacheUseCase {

    /**
     * <h3>공지사항 데이터 동기화</h3>
     * <p>게시글의 공지사항 상태 변경에 따라 Redis 캐시와 DB 데이터를 동기화합니다.</p>
     * <p>새로운 공지사항은 캐시에 추가하고, 해제된 공지사항은 캐시에서 제거합니다.</p>
     * <p>PostAdminService에서 공지 토글 완료 후 데이터 일관성 보장을 위해 호출됩니다.</p>
     * <p>공지사항 리스트 조회 시 최신 데이터를 제공하기 위해 사용됩니다.</p>
     *
     * @param postId 동기화대상 게시글 ID
     * @param isNotice 변경된 공지사항 상태 (true: 공지로 설정, false: 일반으로 해제)
     * @author Jaeik
     * @since 2.0.0
     */
    void syncNoticeCache(Long postId, boolean isNotice);
}