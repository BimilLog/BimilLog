package jaeik.bimillog.domain.admin.entity;

/**
 * <h2>신고 유형 Enum</h2>
 * <p>사용자가 신고할 수 있는 대상의 유형을 정의하는 Enum 클래스</p>
 * <ul>
 *     <li>POST: 게시글 신고</li>
 *     <li>COMMENT: 댓글 신고</li>
 *     <li>SUGGESTION: 건의 사항</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public enum ReportType {
    POST, COMMENT, SUGGESTION
}

