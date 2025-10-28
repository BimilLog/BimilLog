package jaeik.bimillog.domain.post.application.port.out;

public interface RedisPostUpdatePort {

    /**
     * <h3>실시간 인기글 점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 게시글의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 조회/댓글/추천 이벤트 발생 시 호출됩니다.</p>
     *
     * @param postId 점수를 증가시킬 게시글 ID
     * @param score 증가시킬 점수 (조회: 2점, 댓글: 3점, 추천: 4점)
     * @author Jaeik
     * @since 2.0.0
     */
    void incrementRealtimePopularScore(Long postId, double score);

    /**
     * <h3>실시간 인기글 전체 점수 지수감쇠 적용</h3>
     * <p>Redis Sorted Set의 모든 게시글 점수에 0.9를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>PostScheduledService 스케줄러에서 5분마다 호출됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    void applyRealtimePopularScoreDecay();
}
