package jaeik.bimillog.domain.admin.entity;

/**
 * <h2>ReportType</h2>
 * <p>Admin 도메인에서 사용하는 신고 유형을 정의하는 Value Object입니다.</p>
 * <p>각 신고 유형은 서로 다른 처리 로직을 가지며, POST와 COMMENT는 사용자 제재가 가능하지만
 * ERROR와 IMPROVEMENT는 시스템 개선 목적으로만 사용됩니다.</p>
 * 
 * <ul>
 *     <li><strong>POST</strong>: 부적절한 게시글 신고 (사용자 제재 가능)</li>
 *     <li><strong>COMMENT</strong>: 부적절한 댓글 신고 (사용자 제재 가능)</li>
 *     <li><strong>ERROR</strong>: 시스템 오류 신고 (개발팀 확인용)</li>
 *     <li><strong>IMPROVEMENT</strong>: 기능 개선 제안 (제품 개선용)</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public enum ReportType {
    POST, COMMENT, ERROR, IMPROVEMENT
}

