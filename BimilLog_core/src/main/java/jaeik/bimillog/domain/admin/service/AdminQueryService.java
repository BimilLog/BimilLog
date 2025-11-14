
package jaeik.bimillog.domain.admin.service;

import jaeik.bimillog.domain.admin.dto.ReportDTO;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.repository.AdminQueryRepository;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.global.out.GlobalCommentQueryAdapter;
import jaeik.bimillog.domain.global.out.GlobalPostQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>관리자 조회 서비스</h2>
 * <p>관리자 도메인의 조회 작업을 담당하는 서비스입니다.</p>
 * <p>신고 목록 페이지네이션 조회</p>
 * <p>읽기 전용 트랜잭션 사용</p>
 * <p>신고 유형별 필터링, 최신순 정렬</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class AdminQueryService {

    private final AdminQueryRepository adminQueryRepository;
    private final GlobalPostQueryAdapter globalPostQueryAdapter;
    private final GlobalCommentQueryAdapter globalCommentQueryAdapter;

    /**
     * <h3>신고 목록 조회</h3>
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 표시할 신고 수
     * @param reportType 필터링할 신고 유형 (null이면 전체 조회)
     * @return Page<Report> 페이지네이션된 신고 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<ReportDTO> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = adminQueryRepository.findReportsWithPaging(reportType, pageable);

        List<Long> postIds = reports.stream()
                .filter(r -> r.getReportType() == ReportType.POST)
                .map(Report::getTargetId).toList();

        List<Long> commentIds = reports.stream()
                .filter(r -> r.getReportType() == ReportType.COMMENT)
                .map(Report::getTargetId).toList();

        List<Post> posts = globalPostQueryAdapter.findAllByIds(postIds);
        List<Comment> comments = globalCommentQueryAdapter.findAllByIds(commentIds);

        Map<Long, Member> postMaps = posts.stream().collect(Collectors.toMap(
                Post::getId,
                Post::getMember
        ));

        Map<Long, Member> commentMaps = comments.stream().collect(Collectors.toMap(
                Comment::getId,
                Comment::getMember
        ));

        return reports.map(report -> {
            Member targetAuthor = null;
            if (report.getReportType() == ReportType.POST) {
                targetAuthor = postMaps.get(report.getTargetId());
            } else if (report.getReportType() == ReportType.COMMENT) {
                targetAuthor = commentMaps.get(report.getTargetId());
            }
            return ReportDTO.from(report, targetAuthor);
        });
    }
}
