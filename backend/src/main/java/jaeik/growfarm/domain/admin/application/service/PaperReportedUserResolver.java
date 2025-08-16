package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>롤링페이퍼 신고 사용자 해결사</h2>
 * <p>롤링페이퍼 신고 유형에 대해 신고 대상 사용자 정보를 해결하는 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PaperReportedUserResolver implements ReportedUserResolver {

    private final PaperQueryUseCase paperQueryUseCase;

    /**
     * <h3>지원하는 신고 유형 반환</h3>
     * <p>이 해결사가 지원하는 신고 유형(PAPER)을 반환합니다.</p>
     *
     * @return ReportType.PAPER
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public ReportType supports() {
        return ReportType.PAPER;
    }

    /**
     * <h3>메시지 ID로 신고 대상 사용자 해결</h3>
     * <p>주어진 메시지 ID에 해당하는 롤링페이퍼 메시지의 작성자(사용자)를 조회하여 반환합니다.</p>
     *
     * @param targetId 메시지 ID
     * @return User 메시지 작성 사용자 엔티티
     * @throws CustomException 메시지를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User resolve(Long targetId) {
        Message message = paperQueryUseCase.findMessageById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));
        return message.getUser();
    }
}
