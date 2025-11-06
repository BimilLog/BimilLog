package jaeik.bimillog.domain.comment.in.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>댓글 정보 DTO</h2>
 * <p>
 * 게시글에 작성된 댓글의 정보를 담는 DTO 클래스
 * </p>
 * <p>댓글의 연관관계를 표현할수 있다.</p>
 * <p>댓글의 논리적 삭제를 표현할 수 있다.</p>
 * <p>댓글의 인기플래그를 표현 할 수 있다.</p>
 * <p>글 상세 보기에 사용됨</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentDTO {

    private Long id;

    private Long parentId;

    private Long postId;

    private Long memberId;

    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String memberName;

    @Size(max = 255, message = "댓글은 최대 255자 까지 입력 가능합니다.")
    private String content;

    private boolean popular;

    private boolean deleted;

    private Integer likeCount;

    private Instant createdAt;

    private boolean userLike;

    /**
     * <h3>QueryDSL용 댓글 DTO 생성자 - 보안 강화</h3>
     * <p>QueryDSL 쿼리에서 추천수를 한번에 조회할 때 사용하는 생성자입니다.</p>
     * <p>응답용 DTO이므로 비밀번호 필드는 제외됩니다.</p>
     *
     * @param id        댓글 ID
     * @param postId    게시글 ID
     * @param memberId    사용자 ID
     * @param memberName  사용자명
     * @param content   댓글 내용
     * @param deleted   삭제 여부
     * @param createdAt 생성일시
     * @param parentId  부모 댓글 ID
     * @param likes     추천수
     * @author Jaeik
     * @since 2.0.0
     */
    @QueryProjection
    public CommentDTO(Long id, Long postId, Long memberId, String memberName, String content, boolean deleted, Instant createdAt, Long parentId, Integer likes) {
        this.id = id;
        this.postId = postId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.content = content;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.parentId = parentId;
        this.popular = false;
        this.likeCount = likes != null ? likes : 0;
        this.userLike = false;
    }
}
