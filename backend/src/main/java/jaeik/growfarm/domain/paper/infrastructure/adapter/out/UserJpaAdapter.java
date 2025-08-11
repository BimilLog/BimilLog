package jaeik.growfarm.domain.paper.infrastructure.adapter.out;

import jaeik.growfarm.domain.paper.application.port.out.LoadUserPort;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>사용자 JPA 어댑터</h2>
 * <p>
 * Secondary Adapter: Paper 도메인에서 필요한 사용자 관련 데이터 조회를 위한 JPA 구현
 * 기존 UserRepository의 필요한 기능들을 위임하여 완전히 보존
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Repository
@RequiredArgsConstructor
public class UserJpaAdapter implements LoadUserPort {

    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     * 
     * <p>기존 UserRepository.existsByUserName() 메서드를 완전히 위임:</p>
     * <ul>
     *   <li>JPA의 existsBy 쿼리 메서드 그대로 사용</li>
     *   <li>동일한 성능 특성 보존</li>
     * </ul>
     */
    @Override
    public boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>기존 UserRepository.findByUserName() 메서드를 완전히 위임:</p>
     * <ul>
     *   <li>JPA의 findBy 쿼리 메서드 그대로 사용</li>
     *   <li>null 반환 가능성 보존</li>
     *   <li>LAZY 로딩 등 모든 특성 보존</li>
     * </ul>
     */
    @Override
    public Users findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }
}