package jaeik.growfarm.service;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.BlackList;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.admin.BlackListRepository;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.farm.CropRepository;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    // 신고 목록 반환
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

    // 신고 상세 보기
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

    @Transactional
    public void banUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userId));


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

        kakaoService.unlinkByAdmin(user.getKakaoId());

        // 블랙 리스트 등록
        BlackList blackList = BlackList.builder()
                .kakaoId(user.getKakaoId())
                .build();

        blackListRepository.save(blackList);

        if (user.getToken() == null) {
            userRepository.deleteById(userId);
        } else {
            tokenRepository.delete(user.getToken());
            userRepository.deleteById(userId);
        }
    }
}
