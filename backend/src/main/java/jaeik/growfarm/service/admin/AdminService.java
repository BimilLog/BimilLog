package jaeik.growfarm.service.admin;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.BlackList;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.kakao.KakaoService;
import jaeik.growfarm.service.redis.RedisPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>관리자 서비스 클래스</h2>
 * <p>
 * 관리자 관련 비즈니스 로직을 처리하는 서비스 클래스
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final KakaoService kakaoService;
    private final AdminUpdateService adminUpdateService;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final RedisPostService redisPostService;

    /**
     * <h3>신고 목록 조회</h3>
     *
     * <p>
     * 신고 타입에 따라 필터링하여 페이지네이션된 신고 목록을 반환한다.
     * </p>
     *
     * @param page       페이지 번호
     * @param size       페이지 사이즈
     * @param reportType 신고 타입 (null이면 전체 조회)
     * @return 신고 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    public Page<ReportDTO> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reportRepository.findReportsWithPaging(reportType, pageable);
    }

    /**
     * <h3>신고 상세 조회</h3>
     *
     * <p>
     * 신고 ID에 해당하는 신고 상세 정보를 조회한다.
     * </p>
     *
     * @param reportId 신고 ID
     * @return 신고 상세 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    public ReportDTO getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REPORT));

        return ReportDTO.createReportDTO(report);
    }

    /**
     * <h3>사용자 차단 및 블랙리스트 등록 검증</h3>
     *
     * <p>
     * 댓글ID 또는 게시글ID를 통해 사용자를 조회한다.
     * </p>
     *
     * @param reportDTO 신고 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    private Long banUserValidation(ReportDTO reportDTO) {

        if (reportDTO.getTargetId() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }

        if (reportDTO.getReportType() == ReportType.COMMENT) {
            return commentRepository.findUserIdByCommentId(reportDTO.getTargetId());
        }

        if (reportDTO.getReportType() == ReportType.POST) {
            return postRepository.findUserIdByPostId(reportDTO.getTargetId());
        }

        throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
    }

    /**
     * <h3>사용자 차단 및 블랙리스트 등록</h3>
     *
     * <p>
     * 사용자를 차단하고 블랙리스트에 등록하며 카카오 연결을 해제한다.
     * </p>
     *
     * @param reportDTO 차단할 사용자 정
     * @author Jaeik
     * @since 1.0.18
     */
    @Transactional
    public void banUser(ReportDTO reportDTO) {
        Long userId = banUserValidation(reportDTO);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        try {
            Long kakaoId = user.getKakaoId();

            BlackList blackList = BlackList.createBlackList(kakaoId);
            adminUpdateService.banUserProcess(userId, blackList);
            kakaoService.unlinkByAdmin(kakaoId);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.BAN_USER_ERROR, e);
        }
    }

    /**
     * <h3>공지사항 설정</h3>
     *
     * <p>
     * 게시글을 공지사항으로 설정하고 캐시를 업데이트한다.
     * </p>
     *
     * @param postId 공지사항으로 설정할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void setPostAsNotice(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        boolean wasNotice = post.isNotice();
        post.setAsNotice();
        
        if (!wasNotice) {
            redisPostService.deleteNoticePostsCache();
        }
    }

    /**
     * <h3>공지사항 해제</h3>
     *
     * <p>
     * 게시글의 공지사항을 해제하고 캐시를 업데이트한다.
     * </p>
     *
     * @param postId 공지사항을 해제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void unsetPostAsNotice(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        boolean wasNotice = post.isNotice();
        post.unsetAsNotice();
        
        if (wasNotice) {
            redisPostService.deleteNoticePostsCache();
        }
    }

}
