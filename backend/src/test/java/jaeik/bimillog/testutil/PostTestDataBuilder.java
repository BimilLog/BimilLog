package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.member.entity.member.Member;

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

}
