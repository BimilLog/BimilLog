package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.member.entity.Member;

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
    public static Post createPost(Member author, String title, String content) {
        return Post.createPost(author, title, content, 1234);
    }

    /**
     * 익명 비밀번호가 있는 Post 엔티티 생성
     */
    public static Post createPost(Member author, String title, String content, int password) {
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
     * PostSimpleDetail 생성
     */
    public static PostSimpleDetail createPostSearchResult(Long id, String title) {
        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(0)
                .likeCount(0)
                .memberId(1L)
                .memberName("작성자")
                .commentCount(0)
                .createdAt(Instant.now())
                .build();
    }

}
