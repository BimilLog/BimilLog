package jaeik.bimillog.domain.admin.application.port.in;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.infrastructure.adapter.in.admin.listener.ReportSaveListener;
import jaeik.bimillog.infrastructure.adapter.in.admin.web.AdminCommandController;

/**
 * <h2>관리자 명령 유스케이스</h2>
 * <p>관리자 도메인의 명령 작업을 담당하는 유스케이스입니다.</p>
 * <p>신고 접수, 사용자 제재, 강제 탈퇴</p>
 * <p>관리자 권한이 필요한 시스템 상태 변경 작업</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminCommandUseCase {

    /**
     * <h3>신고 및 건의사항 접수</h3>
     * <p>사용자나 관리자로부터 접수된 신고 및 건의사항을 시스템에 등록합니다.</p>
     * <p>POST/COMMENT 신고 시 대상 유효성 검증, ERROR/IMPROVEMENT 신고는 일반적인 건의사항 처리</p>
     * <p>{@link ReportSaveListener}에서 호출합니다.</p>
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
     * <p>POST/COMMENT 작성자 조회 후 UserBannedEvent 발행으로 실제 제재 수행</p>
     * <p>{@link AdminCommandController}에서 관리자 제재 결정 시 호출합니다.</p>
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
     * <p>POST/COMMENT 작성자 조회 후 UserForcedWithdrawalEvent 발행으로 탈퇴 및 데이터 정리 수행</p>
     * <p>{@link AdminCommandController}에서 관리자 강제 탈퇴 결정 시 호출합니다.</p>
     *
     * @param reportType 신고 유형 (POST 또는 COMMENT, 기타 유형은 불가)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @author Jaeik
     * @since 2.0.0
     */
    void forceWithdrawUser(ReportType reportType, Long targetId);
}
