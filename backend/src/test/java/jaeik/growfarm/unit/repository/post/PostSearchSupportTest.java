package jaeik.growfarm.unit.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.search.PostSearchSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * <h2>PostSearchSupport 단위 테스트</h2>
 * <p>
 * 게시글 검색 지원 클래스의 단위 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostSearchSupport 단위 테스트")
class PostSearchSupportTest {

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private CommentRepository commentRepository;

    private TestablePostSearchSupport postSearchSupport;

    // PostSearchSupport는 abstract 클래스이므로 테스트를 위한 concrete 클래스 필요
    private static class TestablePostSearchSupport extends PostSearchSupport {
        protected TestablePostSearchSupport(JPAQueryFactory jpaQueryFactory, CommentRepository commentRepository) {
            super(jpaQueryFactory, commentRepository);
        }

        // protected 메서드들을 public으로 노출하여 테스트 가능하게 함
        public boolean testHasValidSearchCondition(String keyword, String searchType) {
            return hasValidSearchCondition(keyword, searchType);
        }

        public String testSafeNormalizeKeyword(String keyword) {
            return safeNormalizeKeyword(keyword);
        }

        public boolean testIsFullTextLikelyIneffective(String keyword) {
            return isFullTextLikelyIneffective(keyword);
        }

        public Page<SimplePostDTO> testProcessNativeQueryResults(List<Object[]> results, Pageable pageable, long totalCount) {
            return processNativeQueryResults(results, pageable, totalCount);
        }
    }

    @BeforeEach
    void setUp() {
        postSearchSupport = new TestablePostSearchSupport(jpaQueryFactory, commentRepository);
    }

    @Test
    @DisplayName("검색 조건 유효성 검증 - 유효한 조건")
    void hasValidSearchCondition_ValidConditions() {
        // Given & When & Then
        assertTrue(postSearchSupport.testHasValidSearchCondition("검색어", "TITLE"));
        assertTrue(postSearchSupport.testHasValidSearchCondition("test", "TITLE_CONTENT"));
        assertTrue(postSearchSupport.testHasValidSearchCondition("user", "AUTHOR"));
        assertTrue(postSearchSupport.testHasValidSearchCondition("  검색어  ", "TITLE")); // 공백 포함
    }

    @Test
    @DisplayName("검색 조건 유효성 검증 - 무효한 조건")
    void hasValidSearchCondition_InvalidConditions() {
        // Given & When & Then
        assertFalse(postSearchSupport.testHasValidSearchCondition(null, "TITLE"));
        assertFalse(postSearchSupport.testHasValidSearchCondition("", "TITLE"));
        assertFalse(postSearchSupport.testHasValidSearchCondition("   ", "TITLE"));
        assertFalse(postSearchSupport.testHasValidSearchCondition("검색어", null));
        assertFalse(postSearchSupport.testHasValidSearchCondition(null, null));
    }

    @Test
    @DisplayName("검색어 정규화 - 정상 케이스")
    void safeNormalizeKeyword_NormalCases() {
        // Given & When & Then
        assertEquals("검색어", postSearchSupport.testSafeNormalizeKeyword("검색어"));
        assertEquals("test keyword", postSearchSupport.testSafeNormalizeKeyword("test keyword"));
        assertEquals("검색어", postSearchSupport.testSafeNormalizeKeyword("  검색어  ")); // 공백 제거
        assertEquals("한글 English 123", postSearchSupport.testSafeNormalizeKeyword("한글 English 123"));
    }

    @Test
    @DisplayName("검색어 정규화 - 특수문자 제거")
    void safeNormalizeKeyword_SpecialCharacters() {
        // Given & When & Then
        assertEquals("검색어", postSearchSupport.testSafeNormalizeKeyword("검색어!@#"));
        assertEquals("test keyword", postSearchSupport.testSafeNormalizeKeyword("test@#$%keyword"));
        assertEquals("한글 English 123", postSearchSupport.testSafeNormalizeKeyword("한글!@#English$%^123"));
        assertEquals("", postSearchSupport.testSafeNormalizeKeyword("!@#$%^&*()"));
        assertEquals("a b c", postSearchSupport.testSafeNormalizeKeyword("a!@#b$%^c"));
    }

    @Test
    @DisplayName("검색어 정규화 - 공백 정리")
    void safeNormalizeKeyword_WhitespaceNormalization() {
        // Given & When & Then
        assertEquals("a b c", postSearchSupport.testSafeNormalizeKeyword("a   b     c"));
        assertEquals("검색 테스트", postSearchSupport.testSafeNormalizeKeyword("검색\t\n테스트"));
        assertEquals("", postSearchSupport.testSafeNormalizeKeyword("   \t\n   "));
    }

    @Test
    @DisplayName("검색어 정규화 - null 처리")
    void safeNormalizeKeyword_NullHandling() {
        // Given & When & Then
        assertEquals("", postSearchSupport.testSafeNormalizeKeyword(null));
    }

    @Test
    @DisplayName("FullText 검색 비효율 판단 - 효과적인 경우")
    void isFullTextLikelyIneffective_Effective() {
        // Given & When & Then
        assertFalse(postSearchSupport.testIsFullTextLikelyIneffective("검색어테스트"));
        assertFalse(postSearchSupport.testIsFullTextLikelyIneffective("test keyword"));
        assertFalse(postSearchSupport.testIsFullTextLikelyIneffective("abc def"));
        assertFalse(postSearchSupport.testIsFullTextLikelyIneffective("한글검색"));
    }

    @Test
    @DisplayName("FullText 검색 비효율 판단 - 비효율적인 경우")
    void isFullTextLikelyIneffective_Ineffective() {
        // Given & When & Then
        assertTrue(postSearchSupport.testIsFullTextLikelyIneffective("ab")); // 3글자 미만
        assertTrue(postSearchSupport.testIsFullTextLikelyIneffective("a"));
        assertTrue(postSearchSupport.testIsFullTextLikelyIneffective(""));
        assertTrue(postSearchSupport.testIsFullTextLikelyIneffective("a b")); // 모든 토큰이 3글자 미만
        assertTrue(postSearchSupport.testIsFullTextLikelyIneffective("ab cd ef"));
        assertTrue(postSearchSupport.testIsFullTextLikelyIneffective(null));
    }

    @Test
    @DisplayName("FullText 검색 비효율 판단 - 혼합 토큰")
    void isFullTextLikelyIneffective_MixedTokens() {
        // Given & When & Then
        assertFalse(postSearchSupport.testIsFullTextLikelyIneffective("ab test")); // 하나라도 3글자 이상이면 효과적
        assertFalse(postSearchSupport.testIsFullTextLikelyIneffective("a bc test"));
        assertFalse(postSearchSupport.testIsFullTextLikelyIneffective("검색 ab"));
    }

    @Test
    @DisplayName("네이티브 쿼리 결과 처리 - 성공")
    void processNativeQueryResults_Success() {
        // Given
        List<Object[]> results = Arrays.asList(
                createMockPostRow(1L, "제목1", 10, false, null, Instant.now(), 1L, "사용자1"),
                createMockPostRow(2L, "제목2", 20, true, "REALTIME", Instant.now(), 2L, "사용자2")
        );
        Pageable pageable = PageRequest.of(0, 10);
        long totalCount = 2L;

        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 3, 2L, 5));

        // When
        Page<SimplePostDTO> result = postSearchSupport.testProcessNativeQueryResults(results, pageable, totalCount);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getTotalElements());
        
        SimplePostDTO firstPost = result.getContent().get(0);
        assertEquals(1L, firstPost.getPostId());
        assertEquals("제목1", firstPost.getTitle());
        assertEquals(3, firstPost.getCommentCount());
    }

    @Test
    @DisplayName("네이티브 쿼리 결과 처리 - 빈 결과")
    void processNativeQueryResults_EmptyResults() {
        // Given
        List<Object[]> results = Collections.emptyList();
        Pageable pageable = PageRequest.of(0, 10);
        long totalCount = 0L;

        // When
        Page<SimplePostDTO> result = postSearchSupport.testProcessNativeQueryResults(results, pageable, totalCount);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getTotalElements());
    }

    /**
     * Mock 게시글 데이터 생성 헬퍼 메서드
     */
    private Object[] createMockPostRow(Long postId, String title, Integer views, Boolean isNotice, 
                                      String popularFlag, Instant createdAt, Long userId, String userName) {
        return new Object[]{postId, title, views, isNotice, popularFlag, 
                           java.sql.Timestamp.from(createdAt), userId, userName};
    }
}