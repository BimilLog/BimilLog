package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.domain.paper.application.port.out.PaperCommandPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperToUserPort;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>PaperCommandService</h2>
 * <p>
 * 롤링페이퍼 메시지 작성/삭제의 비즈니스 규칙을 관리하고 실행하는 핵심 서비스입니다.
 * 헥사고날 아키텍처에서 UseCase의 구체적 구현체로서 도메인 로직을 오케스트레이션합니다.
 * </p>
 * <p>
 * 사용자가 웹에서 롤링페이퍼에 익명 메시지를 남기고자 할 때, 또는 자신의 롤링페이퍼에서 
 * 부적절한 메시지를 삭제하고자 할 때 필요한 모든 비즈니스 검증과 처리를 담당합니다.
 * </p>
 * <p>
 * 메시지 작성 시: 대상 사용자 존재 여부 확인 → 메시지 생성 → 실시간 알림 이벤트 발행
 * 메시지 삭제 시: 소유권 검증 → 권한 확인 → 메시지 삭제 수행
 * 이러한 비즈니스 흐름을 실행하기 위해 PaperCommandController로부터 호출됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PaperCommandService implements PaperCommandUseCase {

    private final PaperCommandPort paperCommandPort;
    private final PaperQueryPort paperQueryPort;
    private final PaperToUserPort paperToUserPort;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * <h3>내 롤링페이퍼 메시지 삭제</h3>
     * <p>롤링페이퍼 소유자가 자신의 페이퍼에 작성된 부적절하거나 원하지 않는 메시지를 삭제하는 기능입니다.</p>
     * <p>메시지 소유권을 엄격히 검증하여 오직 롤링페이퍼 소유자만이 해당 페이퍼의 메시지를 삭제할 수 있도록 합니다.</p>
     * <p>
     * 사용자가 마이페이지에서 자신의 롤링페이퍼를 관리하던 중 불쾌하거나 스팸성 메시지를 발견했을 때,
     * 해당 메시지를 제거하여 깨끗한 롤링페이퍼를 유지하고자 하는 상황에서
     * PaperCommandController를 통해 호출됩니다.
     * </p>
     *
     * @param userId 현재 로그인한 롤링페이퍼 소유자 ID
     * @param messageId 삭제할 메시지 ID
     * @throws PaperCustomException 메시지가 존재하지 않거나 삭제 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteMessageInMyPaper(Long userId, Long messageId) {
        if (messageId == null || userId == null) {
            throw new PaperCustomException(PaperErrorCode.INVALID_INPUT_VALUE);
        }
        
        Long ownerId = paperQueryPort.findOwnerIdByMessageId(messageId)
                .orElseThrow(() -> new PaperCustomException(PaperErrorCode.MESSAGE_NOT_FOUND));

        if (!ownerId.equals(userId)) {
            throw new PaperCustomException(PaperErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        paperCommandPort.deleteById(messageId);
    }

    /**
     * <h3>롤링페이퍼 메시지 작성</h3>
     * <p>익명의 방문자나 지인이 특정 사용자의 롤링페이퍼에 따뜻한 메시지나 축하 인사를 남기는 핵심 기능입니다.</p>
     * <p>메시지 작성자의 신원을 보호하면서도 의미 있는 소통을 가능하게 하는 익명 메시징 시스템을 구현합니다.</p>
     * <p>
     * 생일, 기념일, 응원 등의 상황에서 누군가가 롤링페이퍼 링크를 통해 접속한 후,
     * 마음을 담은 메시지를 작성하여 롤링페이퍼를 꾸미고 싶을 때,
     * 메시지 내용과 디자인을 선택한 뒤 작성 완료 버튼을 누르는 순간
     * PaperCommandController를 통해 호출됩니다.
     * 메시지 저장과 함께 롤링페이퍼 소유자에게 실시간 알림을 발송합니다.
     * </p>
     *
     * @param userName 롤링페이퍼 소유자의 사용자명 (메시지를 받을 대상)
     * @param decoType 메시지 장식 스타일 (색상, 테마 등)
     * @param anonymity 익명 작성자 이름 (실제 신원을 숨긴 닉네임)
     * @param content 전달하고 싶은 메시지 내용
     * @param width 그리드 레이아웃에서의 메시지 너비
     * @param height 그리드 레이아웃에서의 메시지 높이
     * @throws PaperCustomException 대상 사용자가 존재하지 않거나 입력값이 유효하지 않은 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void writeMessage(String userName, DecoType decoType, String anonymity, 
                           String content, int width, int height) {
        if (userName == null || decoType == null) {
            throw new PaperCustomException(PaperErrorCode.INVALID_INPUT_VALUE);
        }
        
        User user = paperToUserPort.findByUserName(userName)
                .orElseThrow(() -> new PaperCustomException(PaperErrorCode.USERNAME_NOT_FOUND));

        Message message = Message.createMessage(user, decoType, anonymity, content, width, height);
        paperCommandPort.save(message);

        eventPublisher.publishEvent(new RollingPaperEvent(
                user.getId(),
                userName
        ));
    }
}