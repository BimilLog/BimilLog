package jaeik.bimillog.domain.post.entity;

/**
 * <h2>게시글 캐시 플래그</h2>
 * <p>게시글의 캐시 분류와 인기글 상태를 나타내는 열거형입니다.</p>
 * <p>REALTIME: 실시간 인기 게시글 (최근 24시간 내 인기글)</p>
 * <p>WEEKLY: 주간 인기 게시글 (7일 간 인기글)</p>
 * <p>LEGEND: 전설의 게시글 (높은 인기도를 얻은 명예의 게시글)</p>
 * <p>NOTICE: 공지사항 게시글 (관리자가 지정한 공지)</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public enum PostCacheFlag {
    REALTIME, WEEKLY, LEGEND, NOTICE;

    /**
     * <h3>인기글 캐시 타입 목록 반환</h3>
     * <p>인기글 조회 시 대상이 되는 캐시 타입들을 배열로 반환합니다.</p>
     * <p>PostQueryController에서 인기글 목록 조회 시 필터링 조건에 사용됩니다.</p>
     * <p>PostCacheController에서 인기글 대상 게시글 확인 시 사용됩니다.</p>
     *
     * @return PostCacheFlag[] 인기글 캐시 타입 배열 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @author Jaeik
     * @since 2.0.0
     */
    public static PostCacheFlag[] getPopularPostTypes() {
        return new PostCacheFlag[]{REALTIME, WEEKLY, LEGEND, NOTICE};
    }
}
