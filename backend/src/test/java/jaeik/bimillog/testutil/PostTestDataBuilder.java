package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.user.entity.User;

import java.time.Instant;

/**
 * Post 도메인 테스트 데이터 빌더
 * <p>
 * Post 관련 테스트 데이터 생성 유틸리티
 */
public class PostTestDataBuilder {

    /**
     * Post 엔티티 생성
     */
    public static Post createPost(User author, String title, String content) {
        return Post.createPost(author, title, content, 1234);
    }

    /**
     * 익명 비밀번호가 있는 Post 엔티티 생성
     */
    public static Post createPost(User author, String title, String content, int password) {
        return Post.createPost(author, title, content, password);
    }

    /**
     * ID가 설정된 Post 엔티티 생성
     */
    public static Post withId(Long id, Post post) {
        TestFixtures.setFieldValue(post, "id", id);
        return post;
    }

    /**
     * PostSearchResult 생성
     */
    public static PostSearchResult createPostSearchResult(Long id, String title) {
        return PostSearchResult.builder()
                .id(id)
                .title(title)
                .content("미리보기 내용")
                .viewCount(0)
                .likeCount(0)
                .userId(1L)
                .userName("작성자")
                .commentCount(0)
                .createdAt(Instant.now())
                .isNotice(false)
                .build();
    }

    /**
     * PostDetail 생성
     */
    public static PostDetail createPostDetail(Long id, String title, String content) {
        return postDetailBuilder()
                .id(id)
                .title(title)
                .content(content)
                .build();
    }

    /**
     * Mock PostDetail 생성 (테스트용 기본값)
     */
    public static PostDetail createMockPostDetail(Long postId, Long userId) {
        return postDetailBuilder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .userId(userId != null ? userId : 1L)
                .isLiked(userId != null)
                .build();
    }

    /**
     * PostDetail 전용 빌더
     */
    public static class PostDetailBuilder {
        private Long id = 1L;
        private String title = "테스트 게시글";
        private String content = "테스트 내용";
        private Integer viewCount = 10;
        private Integer likeCount = 5;
        private PostCacheFlag postCacheFlag = PostCacheFlag.REALTIME;
        private Instant createdAt = Instant.now();
        private Long userId = 1L;
        private String userName = "testUser";
        private Integer commentCount = 3;
        private boolean isNotice = false;
        private boolean isLiked = false;

        public PostDetailBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PostDetailBuilder title(String title) {
            this.title = title;
            return this;
        }

        public PostDetailBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostDetailBuilder viewCount(Integer viewCount) {
            this.viewCount = viewCount;
            return this;
        }

        public PostDetailBuilder likeCount(Integer likeCount) {
            this.likeCount = likeCount;
            return this;
        }

        public PostDetailBuilder postCacheFlag(PostCacheFlag postCacheFlag) {
            this.postCacheFlag = postCacheFlag;
            return this;
        }

        public PostDetailBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PostDetailBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public PostDetailBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public PostDetailBuilder commentCount(Integer commentCount) {
            this.commentCount = commentCount;
            return this;
        }

        public PostDetailBuilder isNotice(boolean isNotice) {
            this.isNotice = isNotice;
            return this;
        }

        public PostDetailBuilder isLiked(boolean isLiked) {
            this.isLiked = isLiked;
            return this;
        }

        public PostDetail build() {
            return PostDetail.builder()
                    .id(id)
                    .title(title)
                    .content(content)
                    .viewCount(viewCount)
                    .likeCount(likeCount)
                    .postCacheFlag(postCacheFlag)
                    .createdAt(createdAt)
                    .userId(userId)
                    .userName(userName)
                    .commentCount(commentCount)
                    .isNotice(isNotice)
                    .isLiked(isLiked)
                    .build();
        }
    }

    public static PostDetailBuilder postDetailBuilder() {
        return new PostDetailBuilder();
    }

    public static PostDetail defaultPostDetail() {
        return postDetailBuilder().build();
    }
}
