package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperToUserPort;
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
 * <p>
 * 사용자들이 따뜻한 메시지를 주고받는 롤링페이퍼 경험을 제공하기 위해,
 * 두 가지 핵심 상황을 지원합니다:
 * </p>
 * <p>
 * 1. 롤링페이퍼 소유자가 마이페이지에서 자신에게 온 모든 메시지를 확인하고 싶을 때
 * 2. 방문자가 특정 사용자의 롤링페이퍼에 메시지를 남기기 위해 기존 메시지 레이아웃을 확인하고 싶을 때
 * </p>
 * <p>PaperQueryController를 통해 이런 상황들에서 호출되어 적절한 메시지 데이터를 제공합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaperQueryService implements PaperQueryUseCase {

    private final PaperQueryPort paperQueryPort;
    private final PaperToUserPort paperToUserPort;


    /**
     * <h3>내 롤링페이퍼 조회</h3>
     * <p>사용자 ID를 통해 자신의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     * <p>
     * 사용자가 생일이나 기념일 이후 마이페이지에 접속하여
     * "내게 어떤 메시지들이 왔을까?" 궁금해하며 롤링페이퍼를 열어보는 상황에서
     * PaperQueryController를 통해 호출됩니다.
     * 모든 메시지 내용과 작성자명을 포함한 완전한 정보를 제공하여
     * 사용자가 받은 따뜻한 메시지들을 모두 확인할 수 있게 합니다.
     * </p>
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
     * <p>
     * 친구나 지인이 "생일축하해줘야지" 또는 "응원 메시지를 남겨야겠어"라며
     * 롤링페이퍼 링크를 클릭해서 접속한 후, 메시지를 작성하기 전에
     * "어디에 메시지를 써야 할까? 기존 메시지들은 어떻게 배치되어 있지?"라고 궁금해하는 상황에서
     * PaperQueryController를 통해 호출됩니다.
     * 프라이버시 보호를 위해 메시지 내용과 작성자는 숨기고, 
     * 그리드 위치와 장식 정보만 제공하여 새 메시지 배치에 도움을 줍니다.
     * </p>
     *
     * @param userName 방문할 사용자명
     * @return 방문용 메시지 상세 정보 목록
     * @throws PaperCustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<VisitMessageDetail> visitPaper(String userName) {
        boolean exists = paperToUserPort.existsByUserName(userName);
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
     * <p>
     * 사용자가 롤링페이퍼의 특정 메시지에 댓글을 작성하려고 할 때,
     * Comment 도메인에서 "이 댓글이 어떤 메시지에 속하는지" 확인하기 위해
     * 메시지의 존재성과 기본 정보를 검증하는 상황에서 호출됩니다.
     * 크로스 도메인 통신을 통해 Paper 도메인의 데이터를 안전하게 제공합니다.
     * </p>
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