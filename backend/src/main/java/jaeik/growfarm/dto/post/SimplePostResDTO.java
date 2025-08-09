package jaeik.growfarm.dto.post;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jaeik.growfarm.entity.post.PostCacheFlag;
import jaeik.growfarm.entity.user.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * <h2>게시글 목록용 DTO</h2>
 * <p>
 * 게시글 목록 보기용 간단한 데이터 전송 객체
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SimplePostResDTO extends BasePostDisplayDTO {

    @JsonIgnore
    private Users user;

    /**
     * <h3>Native Query 결과로부터 DTO 생성</h3>
     * <p>
     * Native Query 결과를 SimplePostDTO로 변환합니다.
     * </p>
     *
     * @param row Native Query 결과 행
     * @return 변환된 SimplePostDTO 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static SimplePostResDTO fromNativeQuery(Map<String, Object> result) {
        return SimplePostResDTO.builder()
                .postId(convertToLong(result.get("postId")))
                .title((String) result.get("title"))
                .views(result.get("views") != null ? (Integer) result.get("views") : 0)
                .isNotice(Boolean.TRUE.equals(result.get("isNotice")))
                .postCacheFlag(result.get("popularFlag") != null ? PostCacheFlag.valueOf((String) result.get("popularFlag")) : null)
                .createdAt(((java.sql.Timestamp) result.get("createdAt")).toInstant())
                .userId(result.get("userId") != null ? convertToLong(result.get("userId")) : null)
                .userName(result.get("userName") != null ? (String) result.get("userName") : "익명")
                .commentCount(((Number) result.get("commentCount")).intValue())
                .likes(((Number) result.get("likeCount")).intValue())
                .build();
    }

    /**
     * <h3>Object를 Long으로 안전하게 변환</h3>
     *
     * @param obj BigInteger, Long, Integer, String 등
     * @return 변환된 Long 값, 변환 불가 타입이면 예외
     * @author Jaeik
     * @since 1.1.0
     */
    private static Long convertToLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return null;
    }
}
