package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.comment;

import jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.CommentReadRepository;
import jaeik.growfarm.domain.user.application.port.out.LoadCommentPort;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * <h2>댓글 로드 어댑터</h2>
 * <p>User 도메인에서 Comment 도메인의 데이터에 직접 접근하기 위한 어댑터</p>
 * <p>헥사고날 아키텍처 원칙에 따라 User 도메인의 Out-Port를 구현합니다.</p>
 * <p>Comment 도메인의 QueryDSL 구현체를 직접 사용하여 순환 참조를 방지합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class LoadCommentAdapter implements LoadCommentPort {

    private final CommentReadRepository commentReadRepository;

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>Comment 도메인의 QueryDSL 구현체를 직접 사용하여 효율적인 쿼리를 수행합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 작성한 댓글 목록 페이지
     * @author jaeik
     * @version 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
        return commentReadRepository.findCommentsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>Comment 도메인의 QueryDSL 구현체를 직접 사용하여 효율적인 쿼리를 수행합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 추천한 댓글 목록 페이지
     * @author jaeik
     * @version 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return commentReadRepository.findLikedCommentsByUserId(userId, pageable);
    }
}