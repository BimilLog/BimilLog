package jaeik.growfarm.service.paper.delete;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.paper.PaperCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>PaperDeleteService 구현 클래스</h2>
 * <p>롤링페이퍼 삭제 관련 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PaperDeleteServiceImpl implements PaperDeleteService {

    private final PaperCommandRepository paperCommandRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageInMyPaper(CustomUserDetails userDetails, MessageDTO messageDTO) {
        if (!messageDTO.getUserId().equals(userDetails.getUserId())) {
            throw new CustomException(ErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        paperCommandRepository.deleteById(messageDTO.getId());
    }
}
