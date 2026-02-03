package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.dto.MessageWriteDTO;
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
     */
    @Transactional
    public void writeMessage(Long memberId, MessageWriteDTO messageWriteDTO) {
        Member member = paperToMemberAdapter.getMemberById(messageWriteDTO.getOwnerId());

        // 비회원 확인
        if (memberId != null) {
            eventPublisher.publishEvent(new CheckBlacklistEvent(memberId, member.getId())); // 블랙리스트 확인
        }

        Message message = messageWriteDTO.convertDtoToEntity(member);
        paperRepository.save(message);

        eventPublisher.publishEvent(new RollingPaperEvent(
                member.getId(),
                member.getMemberName()
        ));
    }

    /**
     * <h3>메시지 삭제</h3>
     */
    @Transactional
    public void deleteMessage(Long memberId, Long messageId) {
        Member member = paperRepository.findMemberById(messageId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        if (!member.getId().equals(memberId)) {
            throw new CustomException(ErrorCode.PAPER_MESSAGE_DELETE_FORBIDDEN);
        }

        paperRepository.deleteById(messageId);
        eventPublisher.publishEvent(new MessageDeletedEvent(memberId));
    }

    /**
     * <h3>회원탈퇴시 메시지 전체 삭제</h3>
     */
    @Transactional
    public void deleteAllMessageWhenWithdraw(Long memberId) {
        paperRepository.deleteAllByMember_Id(memberId);
        redisPaperDeleteAdapter.removeMemberIdFromRealtimeScore(memberId);
    }
}