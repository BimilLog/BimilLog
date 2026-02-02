package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.global.listener.MemberWithdrawListener;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.controller.PaperCommandController;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.event.MessageDeletedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.paper.repository.PaperRepository;
import jaeik.bimillog.domain.paper.adapter.PaperToMemberAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperDeleteAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>롤링페이퍼 명령 서비스</h2>
 * <p>롤링페이퍼 도메인의 명령 작업을 담당하는 서비스입니다.</p>
 * <p>메시지 작성, 메시지 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class PaperCommandService {
    private final PaperRepository paperRepository;
    private final PaperToMemberAdapter paperToMemberAdapter;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisPaperDeleteAdapter redisPaperDeleteAdapter;

    /**
     * <h3>롤링페이퍼 메시지 작성</h3>
     * <p>특정 사용자의 롤링페이퍼에 메시지를 작성.</p>
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void writeMessage(Long memberId, String memberName, DecoType decoType, String anonymity, String content, int x, int y) {

        if (memberName == null || memberName.trim().isEmpty()) { // 입력 닉네임 검증
            throw new CustomException(ErrorCode.PAPER_INVALID_INPUT_VALUE);
        }

        Member member = paperToMemberAdapter.findByMemberName(memberName) // 입력 닉네임 존재 검증
                .orElseThrow(() -> new CustomException(ErrorCode.PAPER_USERNAME_NOT_FOUND));

        // 비회원 확인
        if (memberId != null) {
            // 블랙리스트 확인
            eventPublisher.publishEvent(new CheckBlacklistEvent(memberId, member.getId()));
        }

        Message message = Message.createMessage(member, decoType, anonymity, content, x, y);
        paperRepository.save(message);

        eventPublisher.publishEvent(new RollingPaperEvent(
                member.getId(),
                member.getMemberName()
        ));
    }

    /**
     * <h3>내 롤링페이퍼 메시지 삭제</h3>
     * <p>사용자의 롤링페이퍼 메시지를 삭제합니다.</p>
     * <p>messageId가 null인 경우: 해당 사용자의 모든 메시지를 삭제합니다 (회원탈퇴 시).</p>
     * <p>messageId가 있는 경우: 특정 메시지를 삭제합니다 (단건 삭제).</p>
     * <p>다른 사용자의 messageId를 전송할 수 있으므로 소유권 검증 필요</p>
     * <p>메시지 삭제 성공 시 MessageDeletedEvent를 발행하여 실시간 인기 점수를 감소.</p>
     * <p>{@link PaperCommandController}에서 메시지 삭제 요청 시 호출되거나,</p>
     * <p>{@link MemberWithdrawListener}에서 회원탈퇴 시 호출됩니다.</p>
     *
     * @param memberId    현재 로그인한 사용자 ID
     * @param messageId 삭제할 메시지 ID (null인 경우 모든 메시지 삭제)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void deleteMessageInMyPaper(Long memberId, Long messageId) {
        if (messageId == null) {
            paperRepository.deleteAllByMember_Id(memberId);
            redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(memberId);
            return;
        }

        // 메시지 삭제의 경우
        Long ownerId = paperRepository.findOwnerIdByMessageId(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAPER_MESSAGE_NOT_FOUND));

        if (!ownerId.equals(memberId)) {
            throw new CustomException(ErrorCode.PAPER_MESSAGE_DELETE_FORBIDDEN);
        }

        // 메시지 삭제
        paperRepository.deleteById(messageId);

        // 메시지 삭제 성공 시 이벤트 발행 (실시간 인기 점수 감소, 단건 삭제만)
        eventPublisher.publishEvent(new MessageDeletedEvent(memberId));
    }
}