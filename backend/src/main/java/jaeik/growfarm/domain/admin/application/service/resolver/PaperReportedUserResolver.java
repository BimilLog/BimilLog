package jaeik.growfarm.domain.admin.application.service.resolver;

import jaeik.growfarm.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.growfarm.domain.paper.domain.Message;
import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaperReportedUserResolver implements ReportedUserResolver {

    private final PaperQueryUseCase paperQueryUseCase;

    @Override
    public User resolve(Long targetId) {
        Message message = paperQueryUseCase.findMessageById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ROLLING_PAPER));
        return message.getUser();
    }

    @Override
    public ReportType supports() {
        return ReportType.PAPER;
    }
}
