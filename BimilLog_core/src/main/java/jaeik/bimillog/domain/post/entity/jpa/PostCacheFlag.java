package jaeik.bimillog.domain.post.entity.jpa;

/**
 * <h2>게시글 특집 타입</h2>
 * <p>Post.featuredType 컬럼에 저장되는 게시글 분류 열거형입니다.</p>
 * <p>WEEKLY: 주간 인기 게시글 (7일 간 인기글)</p>
 * <p>LEGEND: 전설의 게시글 (높은 인기도를 얻은 명예의 게시글)</p>
 * <p>NOTICE: 공지사항 게시글 (관리자가 지정한 공지)</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public enum PostCacheFlag {
    WEEKLY, LEGEND, NOTICE;
}
