package jaeik.growfarm.domain.post.entity;

/**
 * <h2>게시글 캐시 플래그</h2>
 * <p>
 * 게시글의 캐시 상태를 나타내는 열거형입니다.
 * </p>
 * <ul>
 *     <li>REALTIME: 실시간 게시글</li>
 *     <li>WEEKLY: 주간 인기 게시글</li>
 *     <li>LEGEND: 전설의 게시글</li>
 *     <li>NOTICE: 공지사항 게시글</li>
 * </ul>
 *
 * @version 2.0.0
 * @author Jaeik
 */
public enum PostCacheFlag {
    REALTIME, WEEKLY, LEGEND, NOTICE
}
