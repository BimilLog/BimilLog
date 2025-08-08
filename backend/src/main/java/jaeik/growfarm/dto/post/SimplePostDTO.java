package jaeik.growfarm.dto.post;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.Tuple;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.entity.user.Users;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h3>게시글 목록용 DTO</h3>
 * <p>
 * 게시글 목록 보기용 간단한 데이터 전송 객체
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class SimplePostDTO {

    private Long postId;

    private Long userId;

    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    @Size(max = 30, message = "글 내용은 최대 30자 까지 입력 가능합니다.")
    private String title;

    private int commentCount;

    private int likes;

    private int views;

    private Instant createdAt;

    private boolean is_notice;

    private PopularFlag popularFlag;

    // 인기글 선정 시 알림 이벤트 발행용 (JSON 직렬화에서 제외)
    @JsonIgnore
    private Users user;

    /**
     * <h3>QueryDSL Tuple로부터 DTO 생성</h3>
     * <p>
     * QueryDSL의 Tuple을 사용하여 SimplePostDTO를 생성합니다.
     * </p>
     *
     * @param tuple QueryDSL Tuple 객체
     * @param post  QPost 객체
     * @param user  QUsers 객체
     * @return 변환된 SimplePostDTO 객체
     * @author Jaeik
     * @since 1.1.0
     */
    public static SimplePostDTO fromTuple(Tuple tuple, QPost post, QUsers user) {
        return SimplePostDTO.builder()
                .postId(tuple.get(post.id))
                .userId(tuple.get(post.user.id))
                .userName(tuple.get(user.userName) != null ? tuple.get(user.userName) : "익명")
                .title(tuple.get(post.title))
                .views(tuple.get(post.views) != null ? tuple.get(post.views) : 0)
                .createdAt(tuple.get(post.createdAt))
                .is_notice(Boolean.TRUE.equals(tuple.get(post.isNotice)))
                .popularFlag(tuple.get(post.popularFlag))
                .build();
    }

    /**
     * <h3>Native Query 결과로부터 DTO 생성</h3>
     * <p>
     * Native Query 결과를 SimplePostDTO로 변환합니다.
     * </p>
     *
     * @param row Native Query 결과 행
     * @return 변환된 SimplePostDTO 객체
     * @author Jaeik
     * @since 1.1.0
     */
    public static SimplePostDTO fromNativeQuery(Object[] row) {
        return SimplePostDTO.builder()
                .postId(convertToLong(row[0]))
                .title((String) row[1])
                .views(row[2] != null ? (Integer) row[2] : 0)
                .is_notice(Boolean.TRUE.equals(row[3]))
                .popularFlag(row[4] != null ? PopularFlag.valueOf((String) row[4]) : null)
                .createdAt(((java.sql.Timestamp) row[5]).toInstant())
                .userId(row[6] != null ? convertToLong(row[6]) : null)
                .userName(row[7] != null ? (String) row[7] : "익명")
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
        return switch (obj) {
            case null -> null;
            case Long l -> l;
            case Integer i -> i.longValue();
            case String s -> Long.parseLong(s);
            case java.math.BigInteger bi -> bi.longValue();
            default -> throw new IllegalArgumentException("Unsupported type for Long conversion: " + obj.getClass());
        };
    }

    /**
     * <h3>댓글 및 좋아요 수 설정</h3>
     * <p>
     * 댓글 수와 좋아요 수를 설정합니다.
     * </p>
     *
     * @param commentCount 댓글 수
     * @param likeCount    좋아요 수
     * @author Jaeik
     * @since 1.1.0
     */
    public void withCounts(int commentCount, int likeCount) {
        this.commentCount = commentCount;
        this.likes = likeCount;
    }
}
