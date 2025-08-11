package jaeik.growfarm.domain.comment.application.port.out;

import java.util.List;

public interface DeleteCommentClosurePort {

    void deleteByDescendantId(Long commentId);

    void deleteByDescendantIds(List<Long> commentIds);
}
