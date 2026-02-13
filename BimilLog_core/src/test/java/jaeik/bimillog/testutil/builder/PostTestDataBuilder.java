package jaeik.bimillog.testutil.builder;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.testutil.fixtures.TestFixtures;

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
        String memberName = author != null ? author.getMemberName() : "익명";
        return Post.createPost(author, title, content, 1234, memberName);
    }

    /**
     * 익명 비밀번호가 있는 Post 엔티티 생성
     */
    public static Post createPost(Member author, String title, String content, int password) {
        String memberName = author != null ? author.getMemberName() : "익명";
        return Post.createPost(author, title, content, password, memberName);
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

    /**
     * PostSimpleDetail 생성 (memberId 지정)
     */
    public static PostSimpleDetail createPostSearchResultWithMemberId(Long id, String title, Long memberId) {
        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(0)
                .likeCount(0)
                .memberId(memberId)
                .memberName("작성자" + memberId)
                .commentCount(0)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * PostCacheEntry 생성 (JSON LIST 캐시용)
     */
    public static PostCacheEntry createCacheEntry(Long id, String title) {
        return new PostCacheEntry(id, title, Instant.now(), 1L, "작성자", false, false, false);
    }

}
