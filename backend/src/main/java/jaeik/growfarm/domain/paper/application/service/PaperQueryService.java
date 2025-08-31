package jaeik.growfarm.domain.paper.application.service;

import jaeik.growfarm.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.growfarm.domain.paper.application.port.out.PaperQueryPort;
import jaeik.growfarm.domain.paper.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.paper.entity.MessageDetail;
import jaeik.growfarm.domain.paper.entity.VisitMessageDetail;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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


    @Override
    public List<MessageDetail> getMyPaper(Long userId) {
        List<Message> messages = paperQueryPort.findMessagesByUserId(userId);
        return messages.stream()
                .map(MessageDetail::from)
                .toList();
    }


    @Override
    public List<VisitMessageDetail> visitPaper(String userName) {
        boolean exists = loadUserPort.existsByUserName(userName);
        if (!exists) {
            throw new CustomException(ErrorCode.USERNAME_NOT_FOUND);
        }
        List<Message> messages = paperQueryPort.findMessagesByUserName(userName);
        return messages.stream()
                .map(VisitMessageDetail::from)
                .toList();
    }


    @Override
    public Optional<Message> findMessageById(Long messageId) {
        return paperQueryPort.findMessageById(messageId);
    }
}