package jaeik.growfarm.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.board.Post;
import jaeik.growfarm.entity.board.QPost;
import jaeik.growfarm.entity.board.QPostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/*
 * 커스텀 게시글 Repository 구현체
 * 게시글 관련 데이터베이스 작업을 수행하는 Repository 구현체
 * 수정일 : 2025-05-03
 */
@Repository
public class PostCustomRepositoryImpl implements PostCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public PostCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    // 1일 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개를 실시간 인기글로 등록
    @Override
    public List<Post> updateRealtimePopularPosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        jpaQueryFactory.update(post)
                .set(post.isRealtimePopular, false)
                .execute();

        List<Long> popularPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(post.createdAt.after(LocalDateTime.now().minusDays(1)))
                .groupBy(post.id)
                .orderBy(postLike.count().desc())
                .limit(5)
                .fetch();

        if (!popularPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.isRealtimePopular, true)
                    .where(post.id.in(popularPostIds))
                    .execute();
        }

        if (popularPostIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(post)
                .where(post.id.in(popularPostIds))
                .fetch();
    }

    // 해당 유저가 추천 누른 글 목록 반환
    @Override
    public Page<Post> findByLikedPosts(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 사용자가 좋아요한 게시물 ID 목록 조회 (페이징 적용)
        List<Post> posts = jpaQueryFactory
                .selectFrom(post)
                .join(postLike)
                .on(post.id.eq(postLike.post.id))
                .where(postLike.user.id.eq(userId))
                .orderBy(post.createdAt.desc()) // 최신순으로 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 쿼리 실행
        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .join(postLike)
                .on(post.id.eq(postLike.post.id))
                .where(postLike.user.id.eq(userId))
                .fetchOne();

        // null 안전성을 고려한 코드로 수정
        return new PageImpl<>(posts, pageable, total == null ? 0L : total);
    }
}
