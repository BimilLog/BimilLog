package jaeik.bimillog.domain.user.entity;

/**
 * <h2>사용자 상세 정보 인터페이스</h2>
 * <p>소셜 로그인 처리 과정에서 사용자 정보를 나타내는 마커 인터페이스입니다.</p>
 * <p>기존 사용자와 신규 사용자를 구분하여 다형성을 구현합니다.</p>
 *
 * <h3>구현체:</h3>
 * <ul>
 *   <li>{@link ExistingUserDetail}: 기존 회원의 상세 정보 (ID, 토큰, 권한 등 포함)</li>
 *   <li>{@link NewUserDetail}: 신규 회원의 임시 정보 (UUID만 포함)</li>
 * </ul>
 *
 * <p><b>설계 의도:</b> Auth와 User 도메인 간 데이터 전달 시 타입 안정성 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2025-01
 * @see ExistingUserDetail
 * @see NewUserDetail
 */
public interface UserDetail {
}
