package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.out.SocialTokenQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialTokenService {
    private final SocialTokenQueryRepository socialTokenQueryRepository;

    public Optional<SocialToken> getSocialToken(Long memberId) {
        return socialTokenQueryRepository.findSocialTokenByMemberId(memberId);
    }
}
