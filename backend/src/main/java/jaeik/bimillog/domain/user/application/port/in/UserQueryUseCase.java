package jaeik.bimillog.domain.user.application.port.in;

import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.post.application.service.PostCommandService;
import jaeik.bimillog.domain.post.application.service.PostInteractionService;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.user.web.UserQueryController;
import jaeik.bimillog.infrastructure.adapter.out.api.social.SocialAdapter;
import jaeik.bimillog.infrastructure.adapter.out.user.SaveUserAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;

import java.util.Optional;

/**
 * <h2>사용자 조회 유스케이스</h2>
 * <p>사용자 정보 조회를 위한 입력 포트입니다.</p>
 * <p>사용자 엔티티 조회, 설정 조회, 닉네임 검증</p>
 * <p>소셜 로그인 사용자 조회, 토큰 기반 인증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserQueryUseCase {

    /**
     * <h3>소셜 정보로 사용자 조회</h3>
     * <p>제공자(Provider)와 소셜 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>{@link SocialAdapter}, {@link SaveUserAdapter}에서 소셜 로그인 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @param socialId 사용자의 소셜 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId);

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findById(Long id);

    /**
     * <h3>닉네임 중복 확인</h3>
     * <p>해당 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     * <p>{@link UserQueryController}, {@link PaperQueryService}에서 닉네임 중복 확인 시 호출됩니다.</p>
     *
     * @param userName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByUserName(String userName);

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>닉네임을 사용하여 사용자를 조회합니다.</p>
     *
     * @param userName 사용자 닉네임
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findByUserName(String userName);

    /**
     * <h3>ID로 사용자 프록시 조회</h3>
     * <p>실제 쿼리 없이 ID를 가진 사용자의 프록시(참조) 객체를 반환합니다.</p>
     * <p>JPA 연관 관계 설정 시 사용됩니다.</p>
     * <p>{@link PostCommandService}, {@link PostInteractionService}에서 게시글 연관 엔티티 설정 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return User 프록시 객체
     * @author Jaeik
     * @since 2.0.0
     */
    User getReferenceById(Long userId);

    /**
     * <h3>설정 ID로 설정 조회</h3>
     * <p>JWT 토큰의 settingId를 활용하여 설정 정보를 조회합니다.</p>
     * <p>{@link UserQueryController}에서 사용자 설정 조회 API 시 호출됩니다.</p>
     *
     * @param settingId 설정 ID
     * @return 설정 엔티티
     * @throws CustomException 설정을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    Setting findBySettingId(Long settingId);
}
