package jaeik.growfarm.service.admin.resolver;

import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReportedUserResolver implements ReportedUserResolver {

    private final CommentRepository commentRepository;

    @Override
    public Users resolve(Long targetId) {
        Comment comment = commentRepository.findById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        return comment.getUser();
    }

    @Override
    public ReportType supports() {
        return ReportType.COMMENT;
    }
}
