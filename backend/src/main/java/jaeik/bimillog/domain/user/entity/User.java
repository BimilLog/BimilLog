package jaeik.bimillog.domain.user.entity;

import jaeik.bimillog.domain.common.entity.BaseEntity;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>사용자 엔티티</h2>
 * <p>
 * 카카오 로그인을 통한 사용자 정보를 저장하는 엔티티
 * </p>
 * <p>
 * 닉네임, 카카오 정보, 설정 정보 등을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "user", indexes = {
        @Index(name = "idx_user_username", columnList = "userName"),
        @Index(name = "uk_provider_social_id", columnList = "provider, social_id", unique = true),
})

public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "user_id")
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "setting_id")
    private Setting setting;

    @NotNull
    @Column(name = "social_id", nullable = false)
    private String socialId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    @NotNull
    @Column(name = "user_name", unique = true, nullable = false)
    private String userName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false) // 권한
    private UserRole role;

    @Column(name = "social_nickname") // 소셜 프로필 닉네임
    private String socialNickname;

    @Column(name = "thumbnail_image") // 카카오 프로필 이미지
    private String thumbnailImage;

    /**
     * <h3>사용자 토큰 목록</h3>
     * <p>다중 로그인 지원을 위한 토큰 목록. 사용자 삭제 시 모든 토큰이 자동 삭제됩니다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     */
    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Token> tokens = new ArrayList<>();

    /**
     * <h3>사용자 정보 업데이트</h3>
     *
     * <p>
     * 카카오 닉네임과 프로필 이미지를 업데이트한다.
     * </p>
     *
     * @param socialNickname  카카오 닉네임
     * @param thumbnailImage 프로필 이미지 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateUserInfo(String socialNickname, String thumbnailImage) {
        this.socialNickname = socialNickname;
        this.thumbnailImage = thumbnailImage;
    }

    /**
     * <h3>닉네임 변경</h3>
     *
     * <p>
     * 비즈니스 규칙에 따른 닉네임 변경 수행. 중복 검사 및 Race Condition 처리 포함.
     * </p>
     *
     * @param newUserName 새로운 닉네임
     * @param userQueryPort 중복 확인을 위한 쿼리 포트
     * @throws CustomException 중복된 닉네임인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    public void changeUserName(String newUserName, UserQueryPort userQueryPort) {
        // 1차 중복 확인 (성능 최적화를 위한 사전 검사)
        if (userQueryPort.existsByUserName(newUserName)) {
            throw new UserCustomException(UserErrorCode.EXISTED_NICKNAME);
        }
        
        this.userName = newUserName;
    }
    
    /**
     * <h3>닉네임 수정 (단순)</h3>
     *
     * <p>
     * 방어적 코드 - 단순 닉네임 업데이트. 내부 사용 전용.
     * </p>
     *
     * @param userName 새로운 닉네임
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateUserName(String userName) {
        this.userName = userName;
    }

    /**
     * <h3>사용자 역할 변경</h3>
     *
     * <p>
     * 사용자의 역할을 변경한다. (예: 제재를 위한 BAN 역할로 변경)
     * </p>
     *
     * @param role 새로운 역할
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateRole(UserRole role) {
        this.role = role;
    }

    /**
     * <h3>사용자 설정 업데이트</h3>
     *
     * <p>
     * 사용자의 알림 설정을 업데이트한다. JPA 변경 감지를 활용하여 자동 저장된다.
     * </p>
     *
     * @param messageNotification 메시지 알림 여부
     * @param commentNotification 댓글 알림 여부
     * @param postFeaturedNotification 게시글 추천 알림 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateSettings(boolean messageNotification, boolean commentNotification, boolean postFeaturedNotification) {
        this.setting.updateSettings(messageNotification, commentNotification, postFeaturedNotification);
    }

    /**
     * <h3>사용자 생성</h3>
     *
     * <p>
     * 소셜 사용자 프로필과 사용자 이름을 기반으로 새로운 사용자 엔티티를 생성한다.
     * Setting은 명시적으로 전달되어야 한다.
     * </p>
     * @param socialId 소셜 id
     * @param provider 소셜 제공자
     * @param nickname 소셜 닉네임
     * @param profileImageUrl 소셜 프로필 사진 url
     * @param userName 사용자 이름
     * @param setting 사용자 설정
     * @return 생성된 사용자 엔티티
     */
    public static User createUser(String socialId, SocialProvider provider, String nickname, String profileImageUrl, String userName, Setting setting) {
        return User.builder()
                .socialId(socialId)
                .provider(provider)
                .socialNickname(nickname)
                .thumbnailImage(profileImageUrl)
                .userName(userName)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }
}
