package jaeik.bimillog.domain.paper.application.port.out;

/**
 * <h2>롤링페이퍼 캐시 삭제 포트</h2>
 * <p>Paper 도메인의 Redis 캐시 데이터 삭제 작업을 담당하는 포트입니다.</p>
 * <p>실시간 인기 롤링페이퍼 목록에서 특정 회원의 롤링페이퍼 제거</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisPaperDeletePort {

    /**
     * <h3>실시간 인기 롤링페이퍼 목록에서 회원 제거</h3>
     * <p>Redis Sorted Set에서 특정 회원의 롤링페이퍼를 제거합니다.</p>
     * <p>회원 탈퇴 또는 롤링페이퍼 삭제 시 호출됩니다.</p>
     *
     * @param memberId 제거할 회원 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void removeMemberIdFromRealtimeScore(Long memberId);

}
