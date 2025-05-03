package jaeik.growfarm.service;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.BlackList;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.admin.BlackListRepository;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.farm.CropRepository;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 관리자 전용 서비스
 * 신고 목록 조회
 * 신고 상세 조회
 * 유저 차단 및 블랙 리스트 등록
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final KakaoService kakaoService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CropRepository cropRepository;
    private final TokenRepository tokenRepository;
    private final BlackListRepository blackListRepository;
    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final SettingRepository settingRepository;


    /*
     * 신고 목록 조회
     * param int page: 페이지 번호
     * param int size: 페이지 사이즈
     * param ReportType reportType: 신고 타입 (null이면 전체 조회)
     * return: Page<ReportDTO>
     *
     * 신고 목록을 조회하는 메서드
     * 신고 타입에 따라 필터링하여 페이지네이션된 결과를 반환한다.
     * 신고 목록은 최신 순으로 정렬된다.
     * 수정일 : 2025-05-23
     */
    public Page<ReportDTO> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (reportType != null) {
            Page<Report> reports = reportRepository.findByReportType(reportType, pageable);
            return reports.map(report -> ReportDTO.builder()
                    .reportId(report.getId())
                    .reportType(report.getReportType())
                    .userId(report.getUsers().getId())
                    .content(report.getContent())
                    .targetId(report.getTargetId())
                    .build());
        }

        Page<Report> reports = reportRepository.findAll(pageable);

        return reports.map(report -> ReportDTO.builder()
                .reportId(report.getId())
                .reportType(report.getReportType())
                .userId(report.getUsers().getId())
                .targetId(report.getTargetId())
                .content(report.getContent())
                .build());
    }

    /*
     * 신고 상세 조회
     * param Long reportId: 신고 ID
     * return: ReportDTO
     *
     * 신고 상세 정보를 조회하는 메서드
     * 신고 ID에 해당하는 신고 정보를 반환한다.
     * 수정일 : 2025-05-23
     */
    public ReportDTO getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다: " + reportId));

        return ReportDTO.builder()
                .reportId(report.getId())
                .reportType(report.getReportType())
                .userId(report.getUsers().getId())
                .targetId(report.getTargetId())
                .content(report.getContent())
                .build();
    }

    /*
     * 유저 차단 및 블랙 리스트 등록
     * param Long userId: 유저 ID
     * return: ResponseEntity<String>
     *
     * 유저를 차단하고 블랙 리스트에 등록하는 메서드
     * 수정일 : 2025-05-23
     */
    @Transactional
    public void banUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userId));

        Setting setting = user.getSetting();


        // 사용자의 농작물 삭제
        cropRepository.deleteCropsByUserId(userId);

        // 신고 내역 삭제
        reportRepository.deleteReportByUserId(userId);

        // 1. 다른 사용자가 이 사용자의 게시글에 누른 좋아요 삭제
        postRepository.deletePostLikesByPostUserIds(userId);

        // 2. 다른 사용자가 이 사용자의 댓글에 누른 좋아요 삭제
        commentRepository.deleteCommentLikesByCommentUserIds(userId);

        // 3. 이 사용자가 다른 게시글에 누른 좋아요 삭제
        postRepository.deletePostLikesByUserId(userId);

        // 4. 이 사용자가 다른 댓글에 누른 좋아요 삭제
        commentRepository.deleteCommentLikesByUserId(userId);

        // 5. 이 사용자의 게시글에 달린 댓글 삭제
        commentRepository.deleteCommentsByPostUserIds(userId);

        // 6. 이 사용자가 작성한 댓글 삭제
        commentRepository.deleteCommentsByUserId(userId);

        // 7. 이 사용자의 게시글 삭제
        postRepository.deletePostsByUserId(userId);

        // 8. 이 사용자의 SSE 구독 삭제
        emitterRepository.deleteAllEmitterByUserId(userId);

        // 9. 이 사용자의 알림 삭제
        notificationRepository.deleteNotificationsByUserId(userId);

        // 10. 이 사용자의 FCM 토큰 삭제
        fcmTokenRepository.deleteFcmTokenByUserId(userId);

        kakaoService.unlinkByAdmin(user.getKakaoId());

        // 블랙 리스트 등록
        BlackList blackList = BlackList.builder()
                .kakaoId(user.getKakaoId())
                .build();

        blackListRepository.save(blackList);

        if (user.getToken() == null) {
            userRepository.deleteById(userId);
            settingRepository.delete(setting);
            SecurityContextHolder.clearContext();

        } else {
            userRepository.deleteById(userId);
            tokenRepository.delete(user.getToken());
            settingRepository.delete(setting);
            SecurityContextHolder.clearContext();

        }
    }
}
