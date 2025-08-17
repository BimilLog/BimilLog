package jaeik.growfarm.domain.post.entity;

import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.common.entity.BaseEntity;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.PostReqDTO;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>게시글 엔티티</h2>
 * <p>
 * 게시판에 작성된 게시글 정보를 저장하는 엔티티
 * </p>
 * <p>
 * 제목, 내용, 작성자, 조회수, 공지 여부 등을 포함
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@Table(indexes = {
        @Index(name = "idx_post_notice_created", columnList = "isNotice, created_at DESC"),
        @Index(name = "idx_post_created_at_popular", columnList = "created_at, popularFlag"),
        @Index(name = "idx_post_created", columnList = "created_at"),
        @Index(name = "idx_post_popular_flag", columnList = "popularFlag"),
})
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Column(nullable = false, length = 30) // 제목 30자 허용
    private String title;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false) // 내용 1000자 허용
    private String content;

    @NotNull
    @Column(nullable = false)
    private int views;

    @NotNull
    @Column(nullable = false)
    private boolean isNotice;

    @Enumerated(EnumType.STRING)
    private PostCacheFlag postCacheFlag;

    private Integer password;

    /**
     * <h3>게시글 생성</h3>
     *
     * <p>
     * 게시글을 생성하는 정적 팩토리 메서드이다.
     * </p>
     *
     * @param user       작성자 정보
     * @param postReqDTO 게시글 작성 요청 DTO
     * @return 생성된 Post 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Post createPost(User user, PostReqDTO postReqDTO) {
        if (postReqDTO == null) {
            throw new IllegalArgumentException("PostReqDTO cannot be null");
        }
        
        return Post.builder()
                .user(user)
                .title(postReqDTO.getTitle())
                .content(postReqDTO.getContent())
                .views(0)
                .isNotice(false)
                .password(postReqDTO.getPassword())
                .postCacheFlag(null)
                .build();
    }

    /**
     * <h3>게시글 정보 업데이트</h3>
     *
     * <p>
     * 게시글의 제목과 내용을 업데이트한다.
     * </p>
     *
     * @param postReqDTO 업데이트할 게시글 정보
     * @author Jaeik
     * @since 2.0.0
     */
    public void updatePost(PostReqDTO postReqDTO) {
        if (postReqDTO == null) {
            throw new IllegalArgumentException("PostReqDTO cannot be null");
        }
        
        this.title = postReqDTO.getTitle();
        this.content = postReqDTO.getContent();
    }

    /**
     * <h3>공지사항 설정</h3>
     *
     * <p>
     * 게시글을 공지사항으로 설정한다.
     * </p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void setAsNotice() {
        this.isNotice = true;
    }

    /**
     * <h3>공지사항 해제</h3>
     *
     * <p>
     * 게시글의 공지사항을 해제한다.
     * </p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void unsetAsNotice() {
        this.isNotice = false;
    }
    
    /**
     * <h3>게시글 캐시 플래그 설정</h3>
     * <p>게시글의 캐시 플래그를 설정합니다.</p>
     *
     * @param postCacheFlag 설정할 캐시 플래그
     * @author Jaeik
     * @since 2.0.0
     */
    public void setPostCacheFlag(PostCacheFlag postCacheFlag) {
        this.postCacheFlag = postCacheFlag;
    }

    /**
     * <h3>작성자 권한 확인</h3>
     * <p>
     *     현재 사용자가 게시글의 작성자인지 확인합니다.
     * </p>
     *
     * @param userId 확인할 사용자 ID
     * @return 작성자인 경우 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isAuthor(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    /**
     * <h3>조회수 증가</h3>
     * <p>
     *     게시글의 조회수를 1 증가시킨다.
     * </p>
     */
    public void incrementView() {
        this.views++;
    }
}
