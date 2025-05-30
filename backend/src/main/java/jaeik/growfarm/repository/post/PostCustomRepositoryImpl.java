package jaeik.growfarm.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.board.Post;
import jaeik.growfarm.entity.board.QPost;
import jaeik.growfarm.entity.board.QPostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    public void updateRealtimePopularPosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 1. 모든 게시글의 실시간 인기글 컬럼 초기화
        jpaQueryFactory.update(post)
                .set(post.isRealtimePopular, false)
                .execute();

        // 2. 실시간 인기글 조건에 맞는 상위 5개 조회
        List<Long> popularPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(post.createdAt.after(Instant.now().minus(1, ChronoUnit.DAYS)))
                .groupBy(post.id)
                .orderBy(postLike.count().desc())
                .limit(5)
                .fetch();

        // 3. 해당 게시글들의 실시간 인기글 컬럼 true로 설정
        if (!popularPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.isRealtimePopular, true)
                    .where(post.id.in(popularPostIds))
                    .execute();
        }
    }


    // 7일 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개를 주간 인기글로 등록
    @Override
    public List<Post> updateWeeklyPopularPosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 기존 주간 인기글 초기화
        jpaQueryFactory.update(post)
                .set(post.isWeeklyPopular, false)
                .execute();

        // 7일 이내의 글 중 추천 수 많은 상위 5개 선정
        List<Long> popularPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(post.createdAt.after(Instant.now().minus(7, ChronoUnit.DAYS)))
                .groupBy(post.id)
                .orderBy(postLike.count().desc())
                .limit(5)
                .fetch();

        if (!popularPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.isWeeklyPopular, true)
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


    // 추천 수가 20개 이상인 글을 명예의 전당에 등록
    @Override
    public List<Post> updateHallOfFamePosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 추천 수 20개 이상인 게시글 ID 추출
        List<Long> hofPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .groupBy(post.id)
                .having(postLike.count().goe(20))
                .fetch();

        // 먼저 기존 컬럼 초기화
        jpaQueryFactory.update(post)
                .set(post.isHallOfFame, false)
                .execute();

        if (!hofPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.isHallOfFame, true)
                    .where(post.id.in(hofPostIds))
                    .execute();
        }

        if (hofPostIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(post)
                .where(post.id.in(hofPostIds))
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
