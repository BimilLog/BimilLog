package jaeik.bimillog.domain.post.application.port.in;

/**
 * <h2>게시글 캐시 유스케이스</h2>
 * <p>Post 도메인의 캐시 데이터 동기화 작업을 담당하는 유스케이스입니다.</p>
 * <p>공지사항 상태 변경에 따른 캐시 동기화</p>
 * <p>공지사항 리스트 캐시 추가/제거</p>
 * <p>Redis와 DB 데이터의 일관성 유지</p>
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