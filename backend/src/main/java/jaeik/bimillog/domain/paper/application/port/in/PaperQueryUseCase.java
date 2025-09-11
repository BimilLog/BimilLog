package jaeik.bimillog.domain.paper.application.port.in;

import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.infrastructure.exception.CustomException;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 유스케이스</h2>
 * <p>롤링페이퍼 관련 조회 작업의 추상화된 인터페이스를 정의합니다.</p>
 * <p>내 롤링페이퍼 조회: 소유자가 마이페이지에서 내게 온 메시지들 확인</p>
 * <p>다른 사용자 롤링페이퍼 방문: 메시지 작성자가 친구 롤링페이퍼에 메시지 남기고 싶을 때</p>
 * <p>메시지 ID로 메시지 조회: Comment 도메인에서 댓글이 속한 메시지 정보 확인</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperQueryUseCase {

    /**
     * <h3>내 롤링페이퍼 조회</h3>
     * <p>사용자 ID를 통해 자신의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     * <p>모든 메시지의 내용과 작성자명을 포함한 완전한 정보를 제공합니다.</p>
     * <p>{@link PaperQueryController}에서 내 롤링페이퍼 조회 요청 시 호출됩니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @return 내 롤링페이퍼의 메시지 목록 (완전한 정보 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    List<MessageDetail> getMyPaper(Long userId);

    /**
     * <h3>다른 사용자 롤링페이퍼 방문 조회</h3>
     * <p>다른 사용자의 롤링페이퍼를 방문하여 메시지 목록을 조회합니다.</p>
     * <p>민감한 정보(내용, 작성자명)는 제외하고 장식 정보만 제공합니다.</p>
     * <p>{@link PaperQueryController}에서 타 사용자의 롤링페이퍼 방문 요청 시 호출됩니다.</p>
     *
     * @param userName 방문할 사용자명
     * @return 방문한 롤링페이퍼의 메시지 목록 (익명화된 정보)
     * @throws CustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    List<VisitMessageDetail> visitPaper(String userName);

    /**
     * <h3>메시지 ID로 메시지 조회</h3>
     * <p>주어진 메시지 ID에 해당하는 메시지를 조회합니다.</p>
     * <p>Comment 도메인에서 댓글이 속한 메시지 정보를 확인할 때 호출됩니다.</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return 메시지 엔티티 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Message> findMessageById(Long messageId);
}