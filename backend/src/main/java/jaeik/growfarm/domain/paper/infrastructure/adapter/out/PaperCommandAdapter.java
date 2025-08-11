package jaeik.growfarm.domain.paper.infrastructure.adapter.out;

import jaeik.growfarm.domain.paper.application.port.out.SavePaperPort;
import jaeik.growfarm.entity.message.Message;
import jaeik.growfarm.repository.paper.PaperCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>롤링페이퍼 명령 어댑터</h2>
 * <p>
 * Secondary Adapter: 롤링페이퍼 데이터 저장/삭제를 위한 JPA 구현
 * 기존 PaperCommandRepository의 모든 기능을 위임하여 완전히 보존
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Repository
@RequiredArgsConstructor
public class PaperCommandAdapter implements SavePaperPort {

    private final PaperCommandRepository paperCommandRepository;

    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperCommandRepository.save() 메서드를 완전히 위임:</p>
     * <ul>
     *   <li>JPA의 save() 기능 그대로 사용</li>
     *   <li>트랜잭션 처리는 서비스 계층에서 관리</li>
     *   <li>엔티티의 모든 제약조건 및 암호화 로직 보존</li>
     * </ul>
     */
    @Override
    public Message save(Message message) {
        return paperCommandRepository.save(message);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperCommandRepository.deleteById() 메서드를 완전히 위임:</p>
     * <ul>
     *   <li>JPA의 deleteById() 기능 그대로 사용</li>
     *   <li>CASCADE 삭제 등 모든 제약조건 보존</li>
     * </ul>
     */
    @Override
    public void deleteById(Long messageId) {
        paperCommandRepository.deleteById(messageId);
    }
}