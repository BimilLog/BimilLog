package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.paper.web.PaperQueryController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>롤링페이퍼 조회 서비스</h2>
 * <p>롤링페이퍼 도메인의 조회 작업을 담당하는 서비스입니다.</p>
 * <p>내 롤링페이퍼 조회, 타인 롤링페이퍼 방문</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class PaperQueryService implements PaperQueryUseCase {

    private final PaperQueryPort paperQueryPort;


    /**
     * <h3>내 롤링페이퍼 조회</h3>
     * <p>사용자 ID를 통해 자신의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     * <p>메시지 내용과 작성자명을 포함한 완전한 정보를 제공합니다.</p>
     * <p>{@link PaperQueryController}에서 내 롤링페이퍼 조회 요청 시 호출됩니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return 사용자의 롤링페이퍼 메시지 상세 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<MessageDetail> getMyPaper(Long memberId) {
        List<Message> messages = paperQueryPort.findMessagesByUserId(memberId);
        return messages.stream()
                .map(MessageDetail::from)
                .toList();
    }


    /**
     * <h3>다른 사용자 롤링페이퍼 방문 조회</h3>
     * <p>다른 사용자의 롤링페이퍼를 방문하여 메시지 목록을 조회합니다.</p>
     * <p>내용과 작성자명은 제외하고 그리드 위치와 장식 정보만 제공합니다.</p>
     * <p>메시지가 없는 경우 빈 리스트를 반환하여 롤링페이퍼 페이지를 표시할 수 있도록 합니다.</p>
     * <p>{@link PaperQueryController}에서 타인의 롤링페이퍼 방문 요청 시 호출됩니다.</p>
     *
     * @param memberName 방문할 사용자명
     * @return 방문용 메시지 상세 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<VisitMessageDetail> visitPaper(String memberName) {
        if (memberName == null || memberName.trim().isEmpty()) {
            throw new PaperCustomException(PaperErrorCode.INVALID_INPUT_VALUE);
        }

        List<Message> messages = paperQueryPort.findMessagesByMemberName(memberName);
        return messages.stream()
                .map(VisitMessageDetail::from)
                .toList();
    }
}