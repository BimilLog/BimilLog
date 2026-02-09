package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.controller.PostQueryController;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.repository.PostSearchRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Log
public class PostSearchService {
    private final PostSearchRepository postSearchRepository;
    private final PostUtil postUtil;

    /**
     * <h3>게시글 검색 전략 선택</h3>
     * <p>검색 조건에 따라 최적의 검색 전략을 선택하여 게시글을 검색합니다.</p>
     * <p>검색 전략:</p>
     * <ul>
     *     <li>3글자 이상 + WRITER 아님 → 전문검색 시도 (실패 시 부분검색)</li>
     *     <li>WRITER + 4글자 이상 → 접두사 검색 (인덱스 활용)</li>
     *     <li>그 외 → 부분 검색</li>
     * </ul>
     * <p>{@link PostQueryController}에서 검색 요청 시 호출됩니다.</p>
     *
     * @param type     검색 유형 (TITLE, WRITER, TITLE_CONTENT)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return Page&lt;PostSimpleDetail&gt; 검색된 게시글 목록 페이지
     */
    public Page<PostSimpleDetail> searchPost(PostSearchType type, String query, Pageable pageable, Long memberId) {
        Page<PostSimpleDetail> posts;

        // 전략 1: 3글자 이상 + 작성자 검색 아님 → 전문 검색 시도
        if (query.length() >= 3 && type != PostSearchType.WRITER) {
            Page<Object[]> rawResult = postSearchRepository.findByFullTextSearch(type, query, pageable, memberId);
            List<PostSimpleDetail> content = rawResult.stream()
                    .map(this::mapFullTextRow)
                    .collect(Collectors.toList());

            posts = new PageImpl<>(content, rawResult.getPageable(), rawResult.getTotalElements());
        }
        // 전략 2: 작성자 검색 + 4글자 이상 → 접두사 검색 (인덱스 활용)
        else if (type == PostSearchType.WRITER && query.length() >= 4) {
            posts = postSearchRepository.findByPrefixMatch(type, query, pageable, memberId);
        }
        // 전략 3: 그 외 → 부분 검색
        else {
            posts = postSearchRepository.findByPartialMatch(type, query, pageable, memberId);
        }

        List<PostSimpleDetail> blackListFilterPosts = postUtil.removePostsWithBlacklist(memberId, posts.getContent());

        return new PageImpl<>(blackListFilterPosts, posts.getPageable(),
                posts.getTotalElements() - (posts.getContent().size() - blackListFilterPosts.size()));
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
        PostCacheFlag featuredType = row[8] != null ? PostCacheFlag.valueOf(row[8].toString()) : null;

        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(views)
                .likeCount(likeCount)
                .createdAt(createdAt)
                .memberId(memberId)
                .memberName(memberName)
                .commentCount(commentCount)
                .featuredType(featuredType)
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
