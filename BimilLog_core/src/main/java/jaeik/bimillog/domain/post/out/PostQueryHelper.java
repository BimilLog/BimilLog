package jaeik.bimillog.domain.post.out;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.QPost;
import jaeik.bimillog.domain.post.entity.QPostSimpleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <h2>게시글 조회 헬퍼</h2>
 * <p>PostQueryAdapter의 쿼리 빌딩 및 매핑 로직을 담당하는 헬퍼 클래스입니다.</p>
 * <p>QueryDSL 쿼리 빌딩, 배치 조회, FULLTEXT 검색 결과 매핑을 처리합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostQueryHelper {
    private final JPAQueryFactory jpaQueryFactory;
    private final PostLikeQueryRepository postLikeQueryRepository;

    private static final QPost post = QPost.post;
    private static final QMember member = QMember.member;

    /**
     * <h3>게시글 목록 조회</h3>
     * <p>배치 조회로 댓글 수와 추천 수 조회</p>
     *
     * @param contentQueryCustomizer Content 쿼리 커스터마이징 로직 (JOIN, WHERE 등)
     * @param countQueryCustomizer   Count 쿼리 커스터마이징 로직 (JOIN, WHERE 등)
     * @param pageable               페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> findPostsWithQuery(Consumer<JPAQuery<?>> contentQueryCustomizer,
                                                     Consumer<JPAQuery<?>> countQueryCustomizer,
                                                     Pageable pageable) {
        JPAQuery<PostSimpleDetail> contentQuery = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        Expressions.constant(0),
                        post.createdAt,
                        member.id,
                        Expressions.stringTemplate("COALESCE({0}, {1})", member.memberName, "익명"),
                        Expressions.constant(0)))
                .from(post)
                .leftJoin(post.member, member);

        // 커스터마이징 적용 (JOIN, WHERE 등)
        contentQueryCustomizer.accept(contentQuery);

        // 페이징 및 정렬
        List<PostSimpleDetail> content = contentQuery
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 배치 조회로 추천 수 설정
        batchLikeCount(content);

        // Count 쿼리 빌딩
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .leftJoin(post.member, member);


        // 커스터마이징 적용 (JOIN, WHERE 등)
        countQueryCustomizer.accept(countQuery);

        Long total = countQuery.fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>게시글 목록에 추천 수 주입</h3>
     * <p>게시글 목록의 좋아요 수를 배치로 조회하여 주입.</p>
     *
     * @param posts 좋아요 수를 채울 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public void batchLikeCount(List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream()
                .map(PostSimpleDetail::getId)
                .toList();

        Map<Long, Integer> likeCounts = postLikeQueryRepository.findLikeCountsByPostIds(postIds);

        posts.forEach(post -> {
            post.setLikeCount(likeCounts.getOrDefault(post.getId(), 0));
        });
    }

    /**
     * <h3>FULLTEXT 검색 결과 매핑</h3>
     * <p>FULLTEXT 검색으로 조회한 Object[] 배열 목록을 PostSimpleDetail 목록으로 변환합니다.</p>
     *
     * @param rows FULLTEXT 검색 결과 행 목록
     * @return 변환된 게시글 간략 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> mapFullTextRows(List<Object[]> rows) {
        return rows.stream()
                .map(this::mapFullTextRow)
                .collect(Collectors.toList());
    }

    /**
     * <h3>FULLTEXT 검색 단일 행 매핑</h3>
     * <p>FULLTEXT 검색으로 조회한 Object[] 배열을 PostSimpleDetail 객체로 변환합니다.</p>
     * <p>좋아요 수와 댓글 수는 0으로 초기화되며, 이후 배치 조회로 채워집니다.</p>
     *
     * @param row FULLTEXT 검색 결과 행 (id, title, views, ?, createdAt, memberId, memberName)
     * @return 변환된 게시글 간략 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private PostSimpleDetail mapFullTextRow(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String title = row[1] != null ? row[1].toString() : null;
        Integer views = row[2] != null ? ((Number) row[2]).intValue() : 0;
        Instant createdAt = toInstant(row[4]);
        Long memberId = row[5] != null ? ((Number) row[5]).longValue() : null;
        String memberName = row[6] != null ? row[6].toString() : null;

        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(views)
                .likeCount(0)
                .createdAt(createdAt)
                .memberId(memberId)
                .memberName(memberName)
                .commentCount(0)
                .build();
    }

    /**
     * <h3>Object를 Instant로 변환</h3>
     * <p>다양한 날짜/시간 타입을 Instant로 변환합니다.</p>
     * <p>지원 타입: Instant, Timestamp, LocalDateTime</p>
     *
     * @param value 변환할 날짜/시간 객체
     * @return Instant 객체 (변환 실패 시 null)
     * @author Jaeik
     * @since 2.0.0
     */
    private Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        return null;
    }
}
