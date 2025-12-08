package jaeik.bimillog.domain.member.entity;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * <h2>사용자 엔티티</h2>
 * <p>
 * 소셜 로그인(카카오, 네이버 등)을 통한 사용자 정보를 저장하는 엔티티
 * </p>
 * <p>
 * 닉네임, 소셜 정보, 설정 정보 등을 포함
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
@Table(name = "member", indexes = {
        @Index(name = "idx_member_membername", columnList = "memberName"),
        @Index(name = "uk_provider_social_id", columnList = "provider, social_id", unique = true),
})

public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "member_id")
    private Long id;

    // JPA CASCADE (V2.5): Member 저장 시 Setting 자동 저장, 삭제 시 자동 삭제
    // DB CASCADE (V2.5): Member 삭제 시 Setting 자동 삭제
    // 생명주기 일치: Member와 Setting은 항상 함께 생성/삭제
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "setting_id")
    private Setting setting;

    // Null + FK 제거 + Cascade 없음
    // 소셜 토큰이 없는 순간도 있음 + 고아 객체의 생성 가능성 낮음 + FK 필요성 없음
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_token_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private SocialToken socialToken;

    @NotNull
    @Column(name = "social_id", nullable = false)
    private String socialId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    @NotNull
    @Column(name = "member_name", unique = true, nullable = false)
    private String memberName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false) // 권한
    private MemberRole role;

    @Column(name = "social_nickname") // 소셜 프로필 닉네임
    private String socialNickname;

    @Column(name = "thumbnail_image") // 소셜 프로필 이미지
    private String thumbnailImage;

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
    public void updateMemberInfo(String socialNickname, String thumbnailImage) {
        this.socialNickname = socialNickname;
        this.thumbnailImage = thumbnailImage;
    }

    /**
     * <h3>소셜 토큰 갱신</h3>
     * <p>소셜 로그인 시 토큰을 최신 토큰으로 갱신합니다.</p>
     * <p>기존 토큰이 만료되거나 갱신이 필요할 때 호출됩니다.</p>
     *
     * @param socialToken 갱신할 소셜 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateSocialToken(SocialToken socialToken) {
        this.socialToken = socialToken;
    }

    /**
     * <h3>닉네임 변경</h3>
     *
     * <p>
     * 사용자의 닉네임을 변경합니다. JPA 더티체킹을 통해 트랜잭션 커밋 시 UPDATE가 수행됩니다.
     * </p>
     * <p>
     * DB UNIQUE 제약조건에 의해 중복 닉네임 사용 시 DataIntegrityViolationException이 발생합니다.
     * </p>
     *
     * @param newMemberName 새로운 닉네임
     * @author Jaeik
     * @since 2.0.0
     */
    public void changeMemberName(String newMemberName) {
        this.memberName = newMemberName;
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
    public void updateSettings(boolean messageNotification, boolean commentNotification, boolean postFeaturedNotification, boolean friendSendNotification) {
        this.setting.updateSettings(messageNotification, commentNotification, postFeaturedNotification, friendSendNotification);
    }

    /**
     * <h3>사용자 생성</h3>
     *
     * <p>
     * 소셜 사용자 프로필과 사용자 이름을 기반으로 새로운 사용자 엔티티를 생성한다.
     * Setting과 SocialToken은 명시적으로 전달되어야 한다.
     * </p>
     * @param socialId 소셜 id
     * @param provider 소셜 제공자
     * @param nickname 소셜 닉네임
     * @param profileImageUrl 소셜 프로필 사진 url
     * @param memberName 사용자 이름
     * @param setting 사용자 설정
     * @param socialToken 소셜 OAuth 토큰
     * @return 생성된 사용자 엔티티
     */
    public static Member createMember(String socialId, SocialProvider provider, String nickname, String profileImageUrl, String memberName, Setting setting, SocialToken socialToken) {
        return Member.builder()
                .socialId(socialId)
                .provider(provider)
                .socialNickname(nickname)
                .thumbnailImage(profileImageUrl)
                .memberName(memberName)
                .role(MemberRole.USER)
                .setting(setting)
                .socialToken(socialToken)
                .build();
    }
}
