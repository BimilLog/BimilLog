package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.out.SocialTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialTokenService {
    private final SocialTokenRepository socialTokenRepository;

    public Optional<SocialToken> getSocialToken(Long memberId) {
        return socialTokenRepository.findByMemberId(memberId);
    }
}
