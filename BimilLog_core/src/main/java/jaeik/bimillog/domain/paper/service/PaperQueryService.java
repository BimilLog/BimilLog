package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.global.out.GlobalMemberBlacklistAdapter;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.VisitPaperResult;
import jaeik.bimillog.domain.paper.event.PaperViewedEvent;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.paper.out.PaperQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
public class PaperQueryService {
    private final PaperQueryRepository paperQueryRepository;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;
    private final ApplicationEventPublisher eventPublisher;
    private final GlobalMemberBlacklistAdapter globalMemberBlacklistAdapter;

    /**
     * <h3>내 롤링페이퍼 조회</h3>
     * <p>사용자 ID를 통해 자신의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public List<Message> getMyPaper(Long memberId) {
        return paperQueryRepository.findMessagesByUserId(memberId);
    }

    /**
     * <h3>다른 사용자 롤링페이퍼 방문 조회</h3>
     * <p>메시지가 없는 경우 빈 리스트를 반환</p>
     * <p>롤링페이퍼 조회 성공 시 PaperViewedEvent를 발행하여 실시간 인기 점수를 증가시킵니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public VisitPaperResult visitPaper(Long memberId, String memberName) {
        if (memberName == null || memberName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.PAPER_INVALID_INPUT_VALUE);
        }

        // 사용자 존재 여부 확인 (존재하지 않으면 USERNAME_NOT_FOUND 예외 발생)
        Member member = globalMemberQueryAdapter.findByMemberName(memberName)
                .orElseThrow(() -> new CustomException(ErrorCode.PAPER_USERNAME_NOT_FOUND));

        // 비회원 확인
        if (memberId != null) {
            // 블랙리스트 확인
            globalMemberBlacklistAdapter.checkMemberBlacklist(memberId, member.getId());
        }

        List<Message> messages = paperQueryRepository.findMessagesByMemberName(memberName);

        //
        List<VisitPaperResult.VisitMessage> visitMessages = messages.stream()
                .map(VisitPaperResult.VisitMessage::from)
                .toList();

        // 롤링페이퍼 조회 이벤트 발행 (실시간 인기 점수 증가)
        eventPublisher.publishEvent(new PaperViewedEvent(member.getId()));

        return new VisitPaperResult(visitMessages, member.getId());
    }
}