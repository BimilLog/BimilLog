package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.port.out.LoadUserPort;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 서비스</h2>
 * <p>
 * 롤링페이퍼 조회 관련 비즈니스 로직 구현
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaperQueryService implements PaperQueryUseCase {

    private final PaperQueryPort paperQueryPort;
    private final LoadUserPort loadUserPort;


    /**
     * <h3>내 롤링페이퍼 조회</h3>
     * <p>사용자 ID를 통해 자신의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자의 롤링페이퍼 메시지 상세 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<MessageDetail> getMyPaper(Long userId) {
        List<Message> messages = paperQueryPort.findMessagesByUserId(userId);
        return messages.stream()
                .map(MessageDetail::from)
                .toList();
    }


    /**
     * <h3>다른 사용자 롤링페이퍼 방문 조회</h3>
     * <p>다른 사용자의 롤링페이퍼를 방문하여 메시지 목록을 조회합니다.</p>
     * <p>민감한 정보(내용, 작성자명)는 제외하고 장식 정보만 제공합니다.</p>
     *
     * @param userName 방문할 사용자명
     * @return 방문용 메시지 상세 정보 목록
     * @throws CustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<VisitMessageDetail> visitPaper(String userName) {
        boolean exists = loadUserPort.existsByUserName(userName);
        if (!exists) {
            throw new PaperCustomException(PaperErrorCode.USERNAME_NOT_FOUND);
        }
        List<Message> messages = paperQueryPort.findMessagesByUserName(userName);
        return messages.stream()
                .map(VisitMessageDetail::from)
                .toList();
    }


    /**
     * <h3>메시지 ID로 메시지 조회</h3>
     * <p>주어진 메시지 ID에 해당하는 메시지를 조회합니다.</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return 메시지 엔티티 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Message> findMessageById(Long messageId) {
        return paperQueryPort.findMessageById(messageId);
    }
}