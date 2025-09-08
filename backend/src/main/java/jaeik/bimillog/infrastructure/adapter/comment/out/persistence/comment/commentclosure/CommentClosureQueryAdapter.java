package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure;

import jaeik.bimillog.domain.comment.application.port.out.CommentClosureQueryPort;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 클로저 조회 어댑터</h2>
 * <p>댓글 클로저 엔티티 조회를 위한 아웃바운드 어댑터 구현체</p>
 * <p>CQRS 패턴에 따른 조회 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentClosureQueryAdapter implements CommentClosureQueryPort {

    private final CommentClosureRepository commentClosureRepository;

    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티 목록을 조회합니다.</p>
     *
     * @param descendantId 자손 댓글 ID
     * @return Optional<List<CommentClosure>> 조회된 댓글 클로저 엔티티 목록. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<List<CommentClosure>> findByDescendantId(Long descendantId) {
        return commentClosureRepository.findByDescendantId(descendantId);
    }

}
