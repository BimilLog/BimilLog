package jaeik.growfarm.domain.admin.entity;

import lombok.Builder;

/**
 * <h3>신고 값 객체</h3>
 * <p>
 * 신고 생성 시 필요한 정보를 담는 도메인 순수 값 객체
 * </p>
 *
 * @param reportType 신고 유형
 * @param targetId   신고 대상 ID
 * @param content    신고 내용
 * @author Jaeik
 * @since 2.0.0
 */
public record ReportVO(
        ReportType reportType,
        Long targetId,
        String content
) {

    @Builder
    public ReportVO {
    }

    /**
     * <h3>신고 값 객체 생성</h3>
     * <p>신고 생성에 필요한 정보로 ReportVO를 생성합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @param content    신고 내용
     * @return ReportVO 객체
     */
    public static ReportVO of(ReportType reportType, Long targetId, String content) {
        return new ReportVO(reportType, targetId, content);
    }
}