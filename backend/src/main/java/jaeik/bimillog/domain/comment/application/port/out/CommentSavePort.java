package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;

import java.util.List;
import java.util.Optional;

/**
 * <h2>CommentSavePort</h2>
 * <p>
 * 댓글 및 댓글 계층 구조 저장을 담당하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 댓글 생성/저장 관련 외부 의존성을 추상화하여 도메인 로직의 순수성을 보장합니다.
 * CQRS 패턴에 따른 저장 전용 포트로 쓰기 작업에 특화되어 있습니다.
 * </p>
 * <p>
 * 이 포트는 댓글 시스템의 핵심 저장 기능을 제공합니다:
 * - 댓글 엔티티 저장: 새로운 댓글의 데이터베이스 저장
 * - 클로저 테이블 관리: 댓글 계층 구조를 위한 클로저 관계 저장
 * - 배치 저장: 다중 클로저 관계의 효율적 저장
 * - 계층 구조 조회: 부모 댓글의 조상 관계 조회
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 댓글 계층 구조 유지 - 대댓글과 원댓글 간의 관계 보존
 * 2. 트리 탐색 최적화 - 클로저 테이블을 통한 효율적 조상/후손 조회
 * 3. 데이터 일관성 보장 - 댓글과 클로저 관계의 원자적 저장
 * </p>
 * <p>
 * CommentService에서 새 댓글 생성 시 댓글과 클로저 관계를 저장하기 위해 사용됩니다.
 * CommentService에서 대댓글 작성 시 부모 댓글의 조상 관계를 조회하기 위해 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentSavePort {

    /**
     * <h3>댓글 엔티티 저장</h3>
     * <p>새로운 댓글 엔티티를 데이터베이스에 저장하고 자동 생성된 ID를 포함한 엔티티를 반환합니다.</p>
     * <p>댓글 생성 시 첫 번째 단계로 실행되며, 이후 클로저 테이블 저장이 이어집니다.</p>
     * <p>CommentService에서 새 댓글 작성 시 호출됩니다.</p>
     *
     * @param comment 저장할 댓글 엔티티
     * @return Comment 저장된 댓글 엔티티 (생성된 ID 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    Comment save(Comment comment);

    /**
     * <h3>단일 댓글 클로저 관계 저장</h3>
     * <p>댓글 간의 조상-후손 관계를 나타내는 클로저 엔티티를 저장합니다.</p>
     * <p>댓글의 계층 구조를 유지하기 위한 클로저 테이블 패턴의 핵심 구현입니다.</p>
     * <p>CommentService에서 개별 클로저 관계 저장이 필요할 때 호출됩니다.</p>
     *
     * @param commentClosure 저장할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void save(CommentClosure commentClosure);

    /**
     * <h3>다중 댓글 클로저 관계 일괄 저장</h3>
     * <p>여러 개의 댓글 클로저 엔티티를 한 번의 배치 작업으로 효율적으로 저장합니다.</p>
     * <p>대댓글 작성 시 부모의 모든 조상과의 관계를 한 번에 생성할 때 사용하여 데이터베이스 호출을 최소화합니다.</p>
     * <p>CommentService에서 대댓글 생성 시 다중 클로저 관계를 일괄 생성할 때 호출됩니다.</p>
     *
     * @param commentClosures 저장할 댓글 클로저 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void saveAll(List<CommentClosure> commentClosures);

    /**
     * <h3>부모 댓글의 조상 클로저 관계 조회</h3>
     * <p>특정 부모 댓글의 모든 조상 댓글과의 클로저 관계를 조회합니다.</p>
     * <p>대댓글 생성 시 새 댓글이 부모의 모든 조상과도 관계를 가져야 하므로 이 정보가 필요합니다.</p>
     * <p>부모 댓글이 존재하지 않거나 null인 경우 빈 Optional을 반환하여 최상위 댓글임을 나타냅니다.</p>
     * <p>CommentService에서 대댓글 생성 시 클로저 관계 구성을 위해 호출됩니다.</p>
     *
     * @param parentId 부모 댓글 ID
     * @return Optional<List<CommentClosure>> 조상 댓글 클로저 목록. 최상위 댓글이면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<List<CommentClosure>> getParentClosures(Long parentId);
}