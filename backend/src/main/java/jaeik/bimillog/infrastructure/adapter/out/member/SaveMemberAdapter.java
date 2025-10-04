package jaeik.bimillog.infrastructure.adapter.out.member;

import jaeik.bimillog.domain.member.application.port.out.SaveMemberPort;
import jaeik.bimillog.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 저장 어댑터</h2>
 * <p>SignUpService가 생성한 신규 {@link Member} 엔티티를 영속화하는 단일 책임을 담당합니다.</p>
 * <p>JPA 저장소를 감싸는 아웃바운드 어댑터로, 추가 비즈니스 로직 없이 저장만 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SaveMemberAdapter implements SaveMemberPort {

    private final MemberRepository userRepository;

    /**
     * <h3>신규 사용자 영속화</h3>
     * <p>{@link Member} 엔티티를 저장소에 위임합니다.</p>
     *
     * @param member 저장할 회원 엔티티
     * @return 저장된 회원 엔티티(식별자가 부여됨)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public Member saveNewMember(Member member) {
        return userRepository.save(member);
    }
}
