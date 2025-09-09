package jaeik.bimillog.infrastructure.adapter.paper.out.paper;

import jaeik.bimillog.domain.paper.application.port.out.PaperCommandPort;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.infrastructure.adapter.paper.out.jpa.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>롤링페이퍼 Command 어댑터</h2>
 * <p>
 * 헥사고날 아키텍처의 아웃바운드 어댑터로서 롤링페이퍼 메시지의 생성과 삭제를 담당합니다.
 * </p>
 * <p>
 * 도메인 계층의 PaperCommandPort를 구현하여 Message 엔티티의 영속성 작업을 처리하며,
 * MessageRepository를 통해 실제 데이터베이스 작업을 위임합니다.
 * </p>
 * <p>
 * 메시지 작성과 삭제 시 PaperCommandService에서 호출되어 JPA 기반 데이터 영속화를 수행합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PaperCommandAdapter implements PaperCommandPort {

    private final MessageRepository messageRepository;

    /**
     * <h3>롤링페이퍼 메시지 저장</h3>
     * <p>새로운 메시지 엔티티를 데이터베이스에 영속화합니다.</p>
     * <p>AES-256으로 암호화된 메시지 내용과 그리드 레이아웃 정보를 포함하여 저장하며, JPA를 통해 자동 생성된 ID를 반환합니다.</p>
     * <p>롤링페이퍼 메시지 작성 시 PaperCommandService에서 호출되어 Message 엔티티를 데이터베이스에 저장합니다.</p>
     *
     * @param message 저장할 메시지 엔티티 (내용, 위치, 크기, 장식 정보 포함)
     * @return Message 저장된 메시지 엔티티 (생성된 ID 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Message save(Message message) {
        return messageRepository.save(message);
    }

    /**
     * <h3>롤링페이퍼 메시지 삭제</h3>
     * <p>지정된 ID의 메시지를 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>메시지 소유권 검증이 완료된 후 호출되며, 물리적 삭제를 통해 저장 공간을 확보합니다.</p>
     * <p>내 롤링페이퍼 메시지 삭제 시 PaperCommandService에서 호출되어 해당 메시지를 데이터베이스에서 영구 제거합니다.</p>
     *
     * @param messageId 삭제할 메시지의 고유 식별자
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteById(Long messageId) {
        messageRepository.deleteById(messageId);
    }
}