package jaeik.growfarm.service.admin.resolver;

import jaeik.growfarm.domain.comment.application.port.out.LoadCommentPort;
import jaeik.growfarm.domain.comment.domain.Comment;
import jaeik.growfarm.domain.report.domain.ReportType;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReportedUserResolver implements ReportedUserResolver {

    private final LoadCommentPort loadCommentPort;

    @Override
    public User resolve(Long targetId) {
        Comment comment = loadCommentPort.findById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_FAILED));
        return comment.getUser();
    }

    @Override
    public ReportType supports() {
        return ReportType.COMMENT;
    }
}
