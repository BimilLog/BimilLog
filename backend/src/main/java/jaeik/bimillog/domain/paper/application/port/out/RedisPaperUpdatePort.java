package jaeik.bimillog.domain.paper.application.port.out;

/**
 * <h2>롤링페이퍼 캐시 갱신 포트</h2>
 * <p>Paper 도메인의 Redis 캐시 데이터 갱신 작업을 담당하는 포트입니다.</p>
 * <p>실시간 인기 롤링페이퍼 점수 증가 및 감쇠 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisPaperUpdatePort {

    /**
     * <h3>실시간 인기 롤링페이퍼 점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 롤링페이퍼의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 메시지 작성 또는 방문 이벤트 발생 시 호출됩니다.</p>
     *
     * @param memberId 점수를 증가시킬 회원 ID (롤링페이퍼 소유자)
     * @param score    증가시킬 점수
     * @author Jaeik
     * @since 2.0.0
     */
    void incrementRealtimePopularPaperScore(Long memberId, double score);

    /**
     * <h3>실시간 인기 롤링페이퍼 전체 점수 지수감쇠 적용</h3>
     * <p>Redis Sorted Set의 모든 롤링페이퍼 점수에 0.95를 곱하고, 임계값(1점) 이하의 롤링페이퍼를 제거합니다.</p>
     * <p>PaperScheduledService 스케줄러에서 주기적으로 호출됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    void applyRealtimePopularPaperScoreDecay();

}
