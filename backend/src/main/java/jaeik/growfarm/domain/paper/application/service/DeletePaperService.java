package jaeik.growfarm.domain.paper.application.service;

import jaeik.growfarm.domain.paper.application.port.in.DeletePaperUseCase;
import jaeik.growfarm.domain.paper.application.port.out.DeletePaperPort;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>롤링페이퍼 삭제 서비스</h2>
 * <p>
 * Use Case Implementation: 롤링페이퍼 메시지 삭제 관련 비즈니스 로직 구현
 * 기존 PaperDeleteServiceImpl의 모든 로직을 완전히 보존하여 이전
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeletePaperService implements DeletePaperUseCase {

    private final DeletePaperPort deletePaperPort;

    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperDeleteServiceImpl.deleteMessageInMyPaper() 메서드의 로직을 완전히 보존:</p>
     * <ul>
     *   <li>메시지 소유권 검증 (userId 일치 확인)</li>
     *   <li>동일한 예외 처리 (MESSAGE_DELETE_FORBIDDEN)</li>
     *   <li>메시지 ID로 삭제 수행</li>
     * </ul>
     */
    @Override
    public void deleteMessageInMyPaper(CustomUserDetails userDetails, MessageDTO messageDTO) {
        if (!messageDTO.getUserId().equals(userDetails.getUserId())) {
            throw new CustomException(ErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        deletePaperPort.deleteById(messageDTO.getId());
    }
}