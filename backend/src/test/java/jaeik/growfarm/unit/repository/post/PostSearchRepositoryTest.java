package jaeik.growfarm.unit.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.search.PostCustomFullTextRepository;
import jaeik.growfarm.repository.post.search.PostSearchRepositoryImpl;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>PostSearchRepositoryImpl 단위 테스트</h2>
 * <p>
 * 게시글 검색 레포지터리의 단위 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostSearchRepository 단위 테스트")
class PostSearchRepositoryTest {

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostCustomFullTextRepository postCustomFullTextRepository;

    private PostSearchRepositoryImpl postSearchRepository;

    @BeforeEach
    void setUp() {
        postSearchRepository = new PostSearchRepositoryImpl(
                jpaQueryFactory, 
                commentRepository, 
                postCustomFullTextRepository
        );
    }

    @Test
    @DisplayName("제목 검색 - 성공 (FULLTEXT)")
    void searchPosts_Title_FullText_Success() {
        // Given
        String keyword = "검색어테스트";
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Object[]> mockResults = Arrays.asList(
                createMockPostRow(1L, "테스트 검색어테스트 제목", 10, false, null, Instant.now(), 1L, "testUser"),
                createMockPostRow(2L, "검색어테스트가 포함된 제목", 20, false, null, Instant.now(), 2L, "testUser2")
        );
        
        when(postCustomFullTextRepository.findByTitleFullText(anyString(), anyInt(), anyInt()))
                .thenReturn(mockResults);
        when(postCustomFullTextRepository.countByTitleFullText(anyString()))
                .thenReturn(2L);
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 5, 2L, 3));

        // When
        Page<SimplePostDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getTotalElements());
        verify(postCustomFullTextRepository).findByTitleFullText(eq("검색어테스트"), eq(10), eq(0));
        verify(postCustomFullTextRepository).countByTitleFullText(eq("검색어테스트"));
    }

    @Test
    @DisplayName("제목 검색 - 성공 (LIKE 검색, 짧은 키워드)")
    void searchPosts_Title_Like_Success() {
        // Given
        String keyword = "ab"; // 짧은 키워드로 LIKE 검색 유도
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Object[]> mockResults = Arrays.asList(
                createMockPostRow(1L, "ab 포함 제목", 10, false, null, Instant.now(), 1L, "testUser")
        );
        
        when(postCustomFullTextRepository.findByTitleLike(anyString(), anyInt(), anyInt()))
                .thenReturn(mockResults);
        when(postCustomFullTextRepository.countByTitleLike(anyString()))
                .thenReturn(1L);
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 2));

        // When
        Page<SimplePostDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(postCustomFullTextRepository).findByTitleLike(eq("ab"), eq(10), eq(0));
        verify(postCustomFullTextRepository).countByTitleLike(eq("ab"));
    }

    @Test
    @DisplayName("제목+내용 검색 - 성공")
    void searchPosts_TitleContent_Success() {
        // Given
        String keyword = "검색테스트내용";
        String searchType = "TITLE_CONTENT";
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Object[]> mockResults = Arrays.asList(
                createMockPostRow(1L, "제목", 15, false, null, Instant.now(), 1L, "author1")
        );
        
        when(postCustomFullTextRepository.findByTitleContentFullText(anyString(), anyInt(), anyInt()))
                .thenReturn(mockResults);
        when(postCustomFullTextRepository.countByTitleContentFullText(anyString()))
                .thenReturn(1L);
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 7));

        // When
        Page<SimplePostDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(postCustomFullTextRepository).findByTitleContentFullText(eq("검색테스트내용"), eq(10), eq(0));
    }

    @Test
    @DisplayName("작성자 검색 - 성공 (긴 키워드)")
    void searchPosts_Author_LongKeyword_Success() {
        // Given
        String keyword = "testuser"; // 4글자 이상으로 StartsWith 검색 유도
        String searchType = "AUTHOR";
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Object[]> mockResults = Arrays.asList(
                createMockPostRow(1L, "작성자 검색 테스트", 5, false, null, Instant.now(), 1L, "testuser123")
        );
        
        when(postCustomFullTextRepository.findByAuthorStartsWith(anyString(), anyInt(), anyInt()))
                .thenReturn(mockResults);
        when(postCustomFullTextRepository.countByAuthorStartsWith(anyString()))
                .thenReturn(1L);
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 3));

        // When
        Page<SimplePostDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(postCustomFullTextRepository).findByAuthorStartsWith(eq("testuser"), eq(10), eq(0));
    }

    @Test
    @DisplayName("작성자 검색 - 성공 (짧은 키워드)")
    void searchPosts_Author_ShortKeyword_Success() {
        // Given
        String keyword = "유저"; // 4글자 미만으로 Contains 검색 유도
        String searchType = "AUTHOR";
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Object[]> mockResults = Arrays.asList(
                createMockPostRow(1L, "짧은 키워드 검색", 8, false, null, Instant.now(), 1L, "테스트유저")
        );
        
        when(postCustomFullTextRepository.findByAuthorContains(anyString(), anyInt(), anyInt()))
                .thenReturn(mockResults);
        when(postCustomFullTextRepository.countByAuthorContains(anyString()))
                .thenReturn(1L);
        when(commentRepository.findCommentCountsByPostIds(anyList()))
                .thenReturn(Map.of(1L, 4));

        // When
        Page<SimplePostDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(postCustomFullTextRepository).findByAuthorContains(eq("유저"), eq(10), eq(0));
    }

    @Test
    @DisplayName("검색 - 잘못된 검색 타입")
    void searchPosts_InvalidSearchType() {
        // Given
        String keyword = "테스트";
        String searchType = "INVALID_TYPE";
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> 
            postSearchRepository.searchPosts(keyword, searchType, pageable)
        );
        
        assertEquals(ErrorCode.INCORRECT_SEARCH_FORMAT, exception.getErrorCode());
    }

    @Test
    @DisplayName("검색 - 빈 키워드")
    void searchPosts_EmptyKeyword() {
        // Given
        String keyword = "";
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(postCustomFullTextRepository);
    }

    @Test
    @DisplayName("검색 - null 키워드")
    void searchPosts_NullKeyword() {
        // Given
        String keyword = null;
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(postCustomFullTextRepository);
    }

    @Test
    @DisplayName("검색 - 결과 없음")
    void searchPosts_NoResults() {
        // Given
        String keyword = "존재하지않는검색어";
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);
        
        when(postCustomFullTextRepository.findByTitleFullText(anyString(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(postCustomFullTextRepository.countByTitleFullText(anyString()))
                .thenReturn(0L);

        // When
        Page<SimplePostDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(postCustomFullTextRepository).findByTitleFullText(anyString(), anyInt(), anyInt());
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("검색 - 예외 발생시 처리")
    void searchPosts_Exception() {
        // Given
        String keyword = "테스트";
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);
        
        when(postCustomFullTextRepository.findByTitleFullText(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> 
            postSearchRepository.searchPosts(keyword, searchType, pageable)
        );
        
        assertEquals(ErrorCode.INCORRECT_SEARCH_FORMAT, exception.getErrorCode());
        assertTrue(exception.getCause() instanceof RuntimeException);
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