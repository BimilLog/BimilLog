package jaeik.bimillog.domain.user.event;

import jaeik.bimillog.domain.admin.entity.ReportType;

/**
 * <h2>신고 제출 이벤트</h2>
 * <p>사용자가 신고나 건의사항을 제출했을 때 발행되는 도메인 이벤트</p>
 * <p>관리자 도메인에서 이 이벤트를 리스닝하여 신고를 처리합니다.</p>
 *
 * @see jaeik.bimillog.infrastructure.adapter.admin.in.listener.ReportSaveListener 신고 접수 처리
 * @author Jaeik
 * @version 2.0.0
 */
public record ReportSubmittedEvent(
        Long reporterId,
        String reporterName,
        ReportType reportType,
        Long targetId,
        String content
) {

    /**
     * <h3>신고 제출 이벤트 생성</h3>
     * <p>신고자 정보와 신고 내용을 포함한 이벤트를 생성합니다.</p>
     *
     * @param reporterId   신고자 ID
     * @param reporterName 신고자 이름
     * @param reportType   신고 유형
     * @param targetId     신고 대상 ID
     * @param content      신고 내용
     * @return ReportSubmittedEvent 생성된 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    public static ReportSubmittedEvent of(Long reporterId, String reporterName, ReportType reportType, Long targetId, String content) {
        return new ReportSubmittedEvent(reporterId, reporterName, reportType, targetId, content);
    }
}