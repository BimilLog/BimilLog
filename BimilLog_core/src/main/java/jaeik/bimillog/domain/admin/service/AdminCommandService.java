package jaeik.bimillog.domain.admin.service;

import jaeik.bimillog.domain.admin.controller.AdminCommandController;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
import jaeik.bimillog.domain.admin.out.AdminQueryRepository;
import jaeik.bimillog.domain.admin.out.ReportRepository;
import jaeik.bimillog.domain.auth.service.BlacklistService;
import jaeik.bimillog.domain.admin.out.AdminToCommentAdapter;
import jaeik.bimillog.domain.global.out.GlobalPostQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>관리자 명령 서비스</h2>
 * <p>관리자 도메인의 명령 작업을 담당하는 서비스입니다.</p>
 * <p>신고 접수, 사용자 제재, 강제 탈퇴</p>
 * <p>이벤트 기반 도메인 간 통신으로 MemberBannedEvent, UserForcedWithdrawalEvent 발행</p>
 * <p>Post/Comment 도메인과 협력하여 신고 대상 유효성 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class AdminCommandService {
    private final ApplicationEventPublisher eventPublisher;
    private final ReportRepository reportRepository;
    private final AdminQueryRepository adminQueryRepository;
    private final MemberRepository memberRepository;
    private final GlobalPostQueryAdapter globalPostQueryAdapter;
    private final AdminToCommentAdapter adminToCommentAdapter;
    private final BlacklistService blacklistService;

    /**
     * <h3>신고 및 건의사항 접수</h3>
     * <p>익명/로그인 사용자 구분 처리, POST/COMMENT 대상 유효성 검증</p>
     *
     * @param memberId 신고자 사용자 ID (null이면 익명 신고로 처리)
     * @param reportType 신고 유형 (POST, COMMENT, ERROR, IMPROVEMENT)
     * @param targetId 신고 대상 ID (POST/COMMENT 신고 시 필수, ERROR/IMPROVEMENT 시 null 허용)
     * @param content 신고 내용 및 상세 설명
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void createReport(Long memberId, ReportType reportType, Long targetId, String content) {
        Member reporter = Optional.ofNullable(memberId)
                .flatMap(memberRepository::findById)
                .orElse(null);

        Report report = Report.createReport(reportType, targetId, content, reporter);
        reportRepository.save(report);
    }

    /**
     * <h3>사용자 제재 처리</h3>
     * <p>관리자의 제재 결정을 실행합니다.</p>
     * <p>POST/COMMENT 작성자 조회 후 MemberBannedEvent 발행으로 실제 제재 처리</p>
     * <p>{@link AdminCommandController}에서 관리자 제재 결정 시 호출됩니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT만 허용, ERROR/IMPROVEMENT는 예외 발생)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void banUser(ReportType reportType, Long targetId) {
        Member member = resolveUser(reportType, targetId);
        blacklistService.addToBlacklist(member.getId(), member.getSocialId(), member.getProvider());
        blacklistService.blacklistAllUserTokens(member.getId());
        eventPublisher.publishEvent(new MemberBannedEvent(member.getId(), member.getSocialId(), member.getProvider()));
    }

    /**
     * <h3>사용자 강제 탈퇴 처리</h3>
     * <p>POST/COMMENT 작성자 조회 후 UserForcedWithdrawalEvent 발행으로 탈퇴 및 데이터 정리 처리</p>
     * <p>{@link AdminCommandController}에서 관리자 강제 탈퇴 결정 시 호출됩니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT만 허용, ERROR/IMPROVEMENT는 예외 발생)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void forceWithdrawUser(ReportType reportType, Long targetId) {
        Member member = resolveUser(reportType, targetId);
        blacklistService.addToBlacklist(member.getId(), member.getSocialId(), member.getProvider());
        blacklistService.blacklistAllUserTokens(member.getId());
        eventPublisher.publishEvent(new MemberWithdrawnEvent(member.getId(), member.getSocialId(), member.getProvider()));
    }

    /**
     * <h3>신고자 익명화</h3>
     * <p>회원 탈퇴 시 신고 이력은 유지하면서 reporter 연관만 제거합니다.</p>
     *
     * @param memberId 탈퇴한 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void anonymizeReporterByUserId(Long memberId) {
        List<Report> reports = adminQueryRepository.findAllReportsByUserId(memberId);
        for (Report report : reports) {
            report.anonymizeReporter();
        }
    }

    /**
     * <h3>신고 대상 사용자 조회</h3>
     * <p>제재할 사용자를 식별하고 조회합니다.</p>
     * <p>POST 신고: Post 도메인에서 게시글 작성자 조회, COMMENT 신고: Comment 도메인에서 댓글 작성자 조회</p>
     * <p>익명 게시글/댓글인 경우 member가 null이므로 제재 불가능합니다.</p>
     * <p>banUser, forceWithdrawUser에서 사용자 식별 단계에서 사용됩니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT만 허용)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @return Member 신고 대상 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    private Member resolveUser(ReportType reportType, Long targetId) {
        if (targetId == null) {
            throw new CustomException(ErrorCode.ADMIN_INVALID_REPORT_TARGET);
        }

        Member member = switch (reportType) {
            case POST -> {
                try {
                    yield globalPostQueryAdapter.findById(targetId).getMember();
                } catch (Exception e) {
                    throw new CustomException(ErrorCode.ADMIN_POST_ALREADY_DELETED, e);
                }
            }
            case COMMENT -> {
                try {
                    yield adminToCommentAdapter.findById(targetId).getMember();
                } catch (Exception e) {
                    throw new CustomException(ErrorCode.ADMIN_COMMENT_ALREADY_DELETED, e);
                }
            }
            default -> throw new CustomException(ErrorCode.ADMIN_INVALID_REPORT_TARGET);
        };

        if (member == null) {
            throw new CustomException(ErrorCode.ADMIN_ANONYMOUS_USER_CANNOT_BE_BANNED);
        }

        return member;
    }
}
