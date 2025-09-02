package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.ReportedUserResolver;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>건의사항 사용자 해결사</h2>
 * <p>건의사항 유형에 대해 신고 대상 사용자 정보를 해결하는 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SuggestionReportedUserResolver implements ReportedUserResolver {

    private final PaperQueryUseCase paperQueryUseCase;

    /**
     * <h3>지원하는 신고 유형 반환</h3>
     * <p>이 해결사가 지원하는 신고 유형(PAPER)을 반환합니다.</p>
     *
     * @return ReportType.SUGGESTION
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public ReportType supports() {
        return ReportType.SUGGESTION;
    }

    @Override
    public User resolve(Long targetId) {
        return null;
    }
}
