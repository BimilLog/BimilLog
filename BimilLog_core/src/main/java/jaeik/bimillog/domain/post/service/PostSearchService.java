package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.controller.PostQueryController;
import jaeik.bimillog.domain.post.repository.PostFulltextRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Log
public class PostSearchService {
    private final PostFulltextRepository postFulltextRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostToMemberAdapter postToMemberAdapter;

    /**
     * <h3>게시글 검색 전략 선택</h3>
     * <p>검색 조건에 따라 최적의 검색 전략을 선택하여 게시글을 검색합니다.</p>
     * <p>검색 전략:</p>
     * <ul>
     *     <li>2글자 이상 + WRITER 아님 → ngram 전문검색 (NATURAL LANGUAGE MODE) 시도, 실패 시 부분 검색 폴백</li>
     *     <li>WRITER → 부분 검색 (LIKE %query%) — 라운드 5 멤버 검색 정책과 일관 (4글자 prefix 정책 폐기)</li>
     *     <li>그 외 (1글자) → 부분 검색</li>
     * </ul>
     * <p>{@link PostQueryController}에서 검색 요청 시 호출됩니다.</p>
     *
     * @param type     검색 유형 (TITLE, WRITER, TITLE_CONTENT)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return Page&lt;PostSimpleDetail&gt; 검색된 게시글 목록 페이지
     */
    public Page<PostSimpleDetail> searchPost(PostQueryType type, String query, Pageable pageable, Long memberId) {
        Page<PostSimpleDetail> posts;

        // 전략 1: 2글자 이상 + 작성자 검색 아님 → ngram 전문 검색 시도 (token_size=2 기준)
        // ngram parser 가 토큰화하는 최소 단위가 2글자이므로 2글자부터 FTS 활용 가능.
        if (query.length() >= 2 && type != PostQueryType.WRITER) {
            Page<Object[]> rawResult = findByFullTextSearch(type, query, pageable, memberId);
            List<PostSimpleDetail> content = rawResult.stream()
                    .map(this::mapFullTextRow)
                    .collect(Collectors.toList());

            // FTS 결과가 0건이면 LIKE %% 부분 검색으로 폴백 (드문 케이스: 조사/특수문자 토큰 경계 이슈)
            if (rawResult.isEmpty()) {
                posts = postQueryRepository.selectPostSimpleDetails(type.partialCondition(query), pageable, type.getOrders());
            } else {
                posts = new PageImpl<>(content, rawResult.getPageable(), rawResult.getTotalElements());
            }
        }
        // 전략 2: WRITER 또는 1글자 → 부분 검색 (LIKE %query%)
        // WRITER 4글자 prefix-only 정책 폐기. 라운드 5 멤버 검색 결정과 일관.
        else {
            posts = postQueryRepository.selectPostSimpleDetails(type.partialCondition(query), pageable, type.getOrders());
        }

        if (memberId == null) {
            return posts;
        }

        Set<Long> blacklistSet = new HashSet<>(postToMemberAdapter.getInterActionBlacklist(memberId));
        List<PostSimpleDetail> blackListFilterPosts = posts.getContent().stream()
                .filter(post -> !blacklistSet.contains(post.getMemberId())).collect(Collectors.toList());

        return new PageImpl<>(blackListFilterPosts, posts.getPageable(),
                posts.getTotalElements() - (posts.getContent().size() - blackListFilterPosts.size()));
    }

    /**
     * <h3>FULLTEXT 검색 (NATURAL LANGUAGE MODE)</h3>
     * <p>ngram parser(token_size=2) 토큰 매칭으로 한글 임의 위치 부분어 검색을 지원합니다.</p>
     * <p>BOOLEAN MODE prefix(`query+"*"`) 는 토큰 시퀀스의 시작점만 매치하여 한글 부분어 누락이 발생합니다.</p>
     * <p>NATURAL LANGUAGE MODE 는 ngram 토큰을 그대로 매칭하므로 부분어 검색이 자연스럽습니다.</p>
     */
    private Page<Object[]> findByFullTextSearch(PostQueryType type, String query, Pageable pageable, Long viewerId) {
        try {
            List<Object[]> rows;
            long total;
            if (type == PostQueryType.TITLE) {
                rows = postFulltextRepository.findByTitleFullText(query, pageable, viewerId);
                total = postFulltextRepository.countByTitleFullText(query, viewerId);
            } else if (type == PostQueryType.TITLE_CONTENT) {
                rows = postFulltextRepository.findByTitleContentFullText(query, pageable, viewerId);
                total = postFulltextRepository.countByTitleContentFullText(query, viewerId);
            } else {
                throw new IllegalArgumentException("지원하지 않는 검색 타입: " + type);
            }
            return new PageImpl<>(rows, pageable, total);
        } catch (Exception e) {
            log.error("전문검색 실패 - type: {}, query: {}, error: {}", type, query, e.getMessage());
            return Page.empty(pageable);
        }
    }

    /**
     * <h3>FULLTEXT 검색 단일 행 매핑</h3>
     * <p>FULLTEXT 검색으로 조회한 Object[] 배열을 PostSimpleDetail 객체로 변환합니다.</p>
     * <p>댓글 수는 0으로 초기화되며, 이후 배치 조회로 채워집니다.</p>
     *
     * @param row FULLTEXT 검색 결과 행 (id, title, views, createdAt, memberId, memberName, likeCount)
     * @return 변환된 게시글 간략 정보
     */
    private PostSimpleDetail mapFullTextRow(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String title = row[1] != null ? row[1].toString() : null;
        Integer views = row[2] != null ? ((Number) row[2]).intValue() : 0;
        Instant createdAt = toInstant(row[3]);
        Long memberId = row[4] != null ? ((Number) row[4]).longValue() : null;
        String memberName = row[5] != null ? row[5].toString() : null;
        Integer likeCount = ((Number) row[6]).intValue();
        Integer commentCount = row[7] != null ? ((Number) row[7]).intValue() : 0;
        boolean isWeekly = row[8] != null && ((Boolean) row[8]);
        boolean isLegend = row[9] != null && ((Boolean) row[9]);
        boolean isNotice = row[10] != null && ((Boolean) row[10]);

        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(views)
                .likeCount(likeCount)
                .createdAt(createdAt)
                .memberId(memberId)
                .memberName(memberName)
                .commentCount(commentCount)
                .isWeekly(isWeekly)
                .isLegend(isLegend)
                .isNotice(isNotice)
                .build();
    }

    /**
     * <h3>Object를 Instant로 변환</h3>
     * <p>다양한 날짜/시간 타입을 Instant로 변환합니다.</p>
     * <p>지원 타입: Instant, Timestamp, LocalDateTime</p>
     *
     * @param value 변환할 날짜/시간 객체
     * @return Instant 객체 (변환 실패 시 null)
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
