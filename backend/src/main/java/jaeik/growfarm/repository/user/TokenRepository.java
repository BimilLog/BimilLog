package jaeik.growfarm.repository.user;

import jaeik.growfarm.entity.user.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findById(Long tokenId);

    Token findByKakaoAccessToken(String kakaoAccessToken);

    Token findByJwtRefreshToken(String jwtRefreshToken);
}
