package jaeik.growfarm.service.paper.read;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.paper.PaperReadRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>PaperReadService 구현 클래스</h2>
 * <p>롤링페이퍼 조회 관련 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaperReadServiceImpl implements PaperReadService {

    private final PaperReadRepository paperReadRepository;
    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MessageDTO> myPaper(CustomUserDetails userDetails) {
        return paperReadRepository.findMessageDTOsByUserId(userDetails.getUserId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VisitMessageDTO> visitPaper(String userName) {
        boolean exists = userRepository.existsByUserName(userName);
        if (!exists) {
            throw new CustomException(ErrorCode.USERNAME_NOT_FOUND);
        }
        return paperReadRepository.findVisitMessageDTOsByUserName(userName);
    }
}
