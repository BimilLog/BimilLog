package jaeik.bimillog.testutil.annotation;

import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

/**
 * <h2>WithMockCustomUser SecurityContext Factory</h2>
 * <p>@WithMockCustomUser 애노테이션을 처리하는 팩토리 클래스</p>
 * <p>테스트용 CustomUserDetails를 생성하여 SecurityContext에 설정</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class WithMockCustomUserSecurityContextFactory 
        implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // ExistingUserDetail 생성
        ExistingUserDetail userDetail = ExistingUserDetail.builder()
                .userId(annotation.userId())
                .settingId(annotation.settingId())
                .socialId(annotation.socialId())
                .socialNickname(annotation.socialNickname())
                .thumbnailImage(annotation.thumbnailImage())
                .userName(annotation.userName())
                .provider(SocialProvider.KAKAO) // 기본값 KAKAO
                .role(annotation.role())
                .tokenId(null)
                .fcmTokenId(null)
                .build();

        // CustomUserDetails 생성
        CustomUserDetails customUserDetails = new CustomUserDetails(userDetail);

        // Authentication 생성
        Authentication auth = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role().name()))
        );

        // SecurityContext에 설정
        context.setAuthentication(auth);
        return context;
    }
}