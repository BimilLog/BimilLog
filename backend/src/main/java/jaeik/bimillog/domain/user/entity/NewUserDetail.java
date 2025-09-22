package jaeik.bimillog.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>신규 사용자 상세 정보</h2>
 * <p>소셜 로그인 시 신규 회원의 임시 정보를 담는 엔티티입니다.</p>
 * <p>회원가입 전까지 사용되는 임시 식별자(UUID)만을 포함합니다.</p>
 *
 * <h3>사용 시나리오:</h3>
 * <ol>
 *   <li>최초 소셜 로그인 시 생성</li>
 *   <li>Redis에 임시 데이터와 함께 저장</li>
 *   <li>회원가입 페이지에서 UUID로 임시 데이터 조회</li>
 *   <li>회원가입 완료 후 삭제</li>
 * </ol>
 *
 * @author Jaeik
 * @version 2.0.0
 * @see UserDetail
 * @see ExistingUserDetail
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewUserDetail implements UserDetail{

    /**
     * 임시 사용자 식별자
     * <p>Redis에 저장된 임시 데이터 조회 키로 사용됩니다.</p>
     */
    private String uuid;

    /**
     * <h3>신규 사용자 상세 정보 생성</h3>
     * <p>UUID를 받아 신규 사용자 상세 정보 객체를 생성합니다.</p>
     *
     * @param uuid 임시 사용자 식별자
     * @return NewUserDetail 객체
     */
    public static NewUserDetail of(String uuid) {
        return NewUserDetail.builder()
                .uuid(uuid)
                .build();
    }
}
