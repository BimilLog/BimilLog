package jaeik.growfarm.domain.admin.application.service.resolver;

import jaeik.growfarm.domain.paper.application.port.in.ReadPaperUseCase;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaperReportedUserResolver implements ReportedUserResolver {

    private final ReadPaperUseCase readPaperUseCase;

    @Override
    public ReportType supports() {
        return ReportType.PAPER;
    }

    @Override
    public User resolve(Long targetId) {
        Message message = readPaperUseCase.findMessageById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));
        return message.getUser();
    }
}
