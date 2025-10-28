package jaeik.bimillog.domain.admin.entity;

/**
 * <h2>신고 유형</h2>
 * <p>신고 유형을 정의하는 열거형입니다.</p>
 * <p>POST, COMMENT: 사용자 제재 가능</p>
 * <p>ERROR, IMPROVEMENT: 시스템 개선용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public enum ReportType {
    POST, COMMENT, ERROR, IMPROVEMENT
}

