package jaeik.growfarm.unit.service.post;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.repository.post.search.PostSearchRepository;
import jaeik.growfarm.service.post.search.PostSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>PostSearchService 단위 테스트</h2>
 * <p>
 * 게시글 검색 관련 서비스의 메서드들을 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
public class PostSearchServiceTest {

    @Mock
    private PostSearchRepository postSearchRepository;

    @InjectMocks
    private PostSearchServiceImpl postSearchService;

    private SimplePostResDTO simplePostResDTO;

    @BeforeEach
    void setUp() {
        simplePostResDTO = SimplePostResDTO.builder()
                .postId(1L)
                .userId(1L)
                .userName("testUser")
                .title("Test Post Title")
                .commentCount(5)
                .likes(10)
                .views(100)
                .createdAt(Instant.now())
                .is_notice(false)
                .build();
    }

    @Test
    @DisplayName("게시글 검색 테스트 - 제목 검색")
    void testSearchPostByTitle() {
        // Given
        Page<SimplePostResDTO> mockPage = new PageImpl<>(List.of(simplePostResDTO));
        when(postSearchRepository.searchPosts("test", "title", any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        Page<SimplePostResDTO> result = postSearchService.searchPost("title", "test", 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Post Title", result.getContent().get(0).getTitle());
        verify(postSearchRepository, times(1))
                .searchPosts("test", "title", any(Pageable.class));
    }

    @Test
    @DisplayName("게시글 검색 테스트 - 제목+내용 검색")
    void testSearchPostByTitleContent() {
        // Given
        Page<SimplePostResDTO> mockPage = new PageImpl<>(List.of(simplePostResDTO));
        when(postSearchRepository.searchPosts("test", "title_content", any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        Page<SimplePostResDTO> result = postSearchService.searchPost("title_content", "test", 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Post Title", result.getContent().get(0).getTitle());
        verify(postSearchRepository, times(1))
                .searchPosts("test", "title_content", any(Pageable.class));
    }

    @Test
    @DisplayName("게시글 검색 테스트 - 작성자 검색")
    void testSearchPostByAuthor() {
        // Given
        Page<SimplePostResDTO> mockPage = new PageImpl<>(List.of(simplePostResDTO));
        when(postSearchRepository.searchPosts("testUser", "author", any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        Page<SimplePostResDTO> result = postSearchService.searchPost("author", "testUser", 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("testUser", result.getContent().get(0).getUserName());
        verify(postSearchRepository, times(1))
                .searchPosts("testUser", "author", any(Pageable.class));
    }

    @Test
    @DisplayName("게시글 검색 테스트 - 빈 검색 결과")
    void testSearchPostEmpty() {
        // Given
        Page<SimplePostResDTO> emptyPage = new PageImpl<>(List.of());
        when(postSearchRepository.searchPosts("nonexistent", "title", any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        Page<SimplePostResDTO> result = postSearchService.searchPost("title", "nonexistent", 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(postSearchRepository, times(1))
                .searchPosts("nonexistent", "title", any(Pageable.class));
    }

    @Test
    @DisplayName("게시글 검색 테스트 - 페이징")
    void testSearchPostWithPaging() {
        // Given
        List<SimplePostResDTO> posts = List.of(simplePostResDTO);
        Page<SimplePostResDTO> mockPage = new PageImpl<>(posts,
                org.springframework.data.domain.PageRequest.of(1, 5), 20);
        when(postSearchRepository.searchPosts("test", "title", any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        Page<SimplePostResDTO> result = postSearchService.searchPost("title", "test", 1, 5);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(20, result.getTotalElements());
        assertEquals(4, result.getTotalPages());
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        verify(postSearchRepository, times(1))
                .searchPosts("test", "title", any(Pageable.class));
    }

    @Test
    @DisplayName("게시글 검색 테스트 - 검색어 공백 처리")
    void testSearchPostWithWhitespace() {
        // Given
        String searchQuery = "  test  ";
        Page<SimplePostResDTO> mockPage = new PageImpl<>(List.of(simplePostResDTO));
        when(postSearchRepository.searchPosts("test", "title", any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        Page<SimplePostResDTO> result = postSearchService.searchPost("title", searchQuery, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        // 검색어가 trim되어 전달되는지 확인
        verify(postSearchRepository, times(1))
                .searchPosts("test", "title", any(Pageable.class));
    }
}