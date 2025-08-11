package jaeik.growfarm.domain.paper.application.service;

import jaeik.growfarm.domain.paper.application.port.in.ReadPaperUseCase;
import jaeik.growfarm.domain.paper.application.port.out.LoadPaperPort;
import jaeik.growfarm.domain.paper.application.port.out.LoadUserPort;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import jaeik.growfarm.domain.paper.domain.Message;

/**
 * <h2>롤링페이퍼 조회 서비스</h2>
 * <p>
 * Use Case Implementation: 롤링페이퍼 조회 관련 비즈니스 로직 구현
 * 기존 PaperReadServiceImpl의 모든 로직을 완전히 보존하여 이전
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReadPaperService implements ReadPaperUseCase {

    private final LoadPaperPort loadPaperPort;
    private final LoadUserPort loadUserPort;

    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperReadServiceImpl.myPaper() 메서드의 로직을 완전히 보존</p>
     */
    @Override
    public List<MessageDTO> getMyPaper(CustomUserDetails userDetails) {
        return loadPaperPort.findMessageDTOsByUserId(userDetails.getUserId());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperReadServiceImpl.visitPaper() 메서드의 로직을 완전히 보존:</p>
     * <ul>
     *   <li>사용자 존재 여부 검증</li>
     *   <li>동일한 예외 처리 (USERNAME_NOT_FOUND)</li>
     *   <li>동일한 반환 타입과 데이터</li>
     * </ul>
     */
    @Override
    public List<VisitMessageDTO> visitPaper(String userName) {
        boolean exists = loadUserPort.existsByUserName(userName);
        if (!exists) {
            throw new CustomException(ErrorCode.USERNAME_NOT_FOUND);
        }
        return loadPaperPort.findVisitMessageDTOsByUserName(userName);
    }

    @Override
    public Optional<Message> findMessageById(Long messageId) {
        return loadPaperPort.findMessageById(messageId);
    }
}