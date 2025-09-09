package jaeik.bimillog.domain.admin.application.port.in;

import jaeik.bimillog.domain.admin.entity.ReportType;

/**
 * <h2>AdminCommandUseCase</h2>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 명령 처리를 위한 Primary Port 인터페이스입니다.
 * </p>
 * <p>
 * CQRS 패턴에 따른 명령(Command) 측면의 관리자 기능을 정의하며, 신고 접수, 사용자 제재, 강제 탈퇴 등
 * 시스템 상태를 변경하는 핵심 관리자 업무를 추상화합니다.
 * </p>
 * <p>
 * AdminCommandController에서 외부 요청을 받아 이 인터페이스를 호출하고,
 * AdminCommandService에서 구체적인 비즈니스 로직을 구현합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminCommandUseCase {
    /**
     * <h3>신고 및 건의사항 접수</h3>
     * <p>사용자나 관리자로부터 접수된 신고 및 건의사항을 시스템에 등록합니다.</p>
     * <p>프론트엔드의 신고 폼, 관리자 대시보드, 또는 고객센터를 통해 제출된 요청을 처리합니다.</p>
     * <p>AdminCommandController에서 HTTP 요청을 받아 이 메서드를 호출합니다.</p>
     *
     * @param userId 신고자 사용자 ID (익명 신고인 경우 null)
     * @param reportType 신고 유형 (POST, COMMENT, ERROR, IMPROVEMENT)
     * @param targetId 신고 대상 ID (POST/COMMENT 신고 시 필수, 기타는 null)
     * @param content 신고 내용 및 상세 설명
     * @author Jaeik
     * @since 2.0.0
     */
    void createReport(Long userId, ReportType reportType, Long targetId, String content);

    /**
     * <h3>사용자 제재 처리</h3>
     * <p>관리자의 제재 결정에 따라 특정 사용자에게 제재를 가합니다.</p>
     * <p>관리자 대시보드에서 신고를 검토한 후 제재가 필요하다고 판단될 때 AdminCommandController에서 호출합니다.</p>
     * <p>사용자의 활동을 일시적으로 제한하는 조치로, 강제 탈퇴보다는 가벼운 제재 수단입니다.</p>
     *
     * @param reportType 신고 유형 (POST 또는 COMMENT, 기타 유형은 불가)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @author Jaeik
     * @since 2.0.0
     */
    void banUser(ReportType reportType, Long targetId);

    /**
     * <h3>사용자 강제 탈퇴 처리</h3>
     * <p>관리자의 최종 판단에 따라 사용자를 시스템에서 영구적으로 제거합니다.</p>
     * <p>관리자 대시보드에서 심각한 위반으로 판단하여 영구 제재가 필요할 때 AdminCommandController에서 호출합니다.</p>
     * <p>단순 제재와 달리 사용자의 모든 데이터를 정리하고 재가입을 차단하는 강력한 최종 조치입니다.</p>
     *
     * @param reportType 신고 유형 (POST 또는 COMMENT, 기타 유형은 불가)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @author Jaeik
     * @since 2.0.0
     */
    void forceWithdrawUser(ReportType reportType, Long targetId);
}
