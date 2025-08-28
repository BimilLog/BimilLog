package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.cache;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostDetail;
import jaeik.growfarm.domain.post.entity.PostSearchResult;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * <h2>PostCacheQueryAdapter 테스트</h2>
 * <p>Redis 캐시 조회 어댑터의 모든 기능을 테스트합니다.</p>
 * <p>캐시 존재 여부 확인, 게시글 목록 조회, 개별 게시글 조회 등을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class PostCacheQueryAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ListOperations<String, Object> listOperations;

    private PostCacheQueryAdapter postCacheQueryAdapter;

    private List<PostSearchResult> testSimplePostDTOs;
    private PostDetail testFullPostDTO;

    @BeforeEach
    void setUp() {
        // RedisTemplate Mock 설정 - lenient()로 불필요한 스터빙 허용
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        // PostCacheQueryAdapter 인스턴스 생성
        postCacheQueryAdapter = new PostCacheQueryAdapter(redisTemplate);

        // 테스트 데이터 준비
        createTestData();
    }

    private void createTestData() {
        // PostSearchResult 테스트 데이터 (실제 Redis에 저장되는 형태)
        testSimplePostDTOs = Arrays.asList(
            PostSearchResult.builder()
                .id(1L)
                .title("인기 게시글 1")
                .content("인기 게시글 내용 1")
                .viewCount(100)
                .likeCount(50)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .createdAt(Instant.now())
                .userId(1L)
                .userName("testUser")
                .commentCount(10)
                .isNotice(false)
                .build(),
            PostSearchResult.builder()
                .id(2L)
                .title("인기 게시글 2")
                .content("인기 게시글 내용 2")
                .viewCount(80)
                .likeCount(40)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .createdAt(Instant.now())
                .userId(2L)
                .userName("testUser2")
                .commentCount(5)
                .isNotice(false)
                .build()
        );

        // PostDetail 테스트 데이터 (실제 Redis에 저장되는 형태)
        testFullPostDTO = PostDetail.builder()
            .id(1L)
            .title("상세 게시글")
            .content("상세 게시글 내용입니다.")
            .viewCount(200)
            .likeCount(100)
            .postCacheFlag(PostCacheFlag.LEGEND)
            .createdAt(Instant.now())
            .userId(1L)
            .userName("testUser")
            .commentCount(15)
            .isNotice(false)
            .isLiked(true)
            .build();
    }

    @Test
    @DisplayName("정상 케이스 - REALTIME 캐시 존재 확인")
    void shouldReturnTrue_WhenRealtimeCacheExists() {
        // Given: REALTIME 캐시가 존재함
        given(redisTemplate.hasKey("cache:posts:realtime")).willReturn(true);
        
        // When: 캐시 존재 여부 확인
        boolean result = postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.REALTIME);
        
        // Then: true 반환
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey("cache:posts:realtime");
    }

    @Test
    @DisplayName("정상 케이스 - WEEKLY 캐시 미존재 확인")
    void shouldReturnFalse_WhenWeeklyCacheNotExists() {
        // Given: WEEKLY 캐시가 존재하지 않음
        given(redisTemplate.hasKey("cache:posts:weekly")).willReturn(false);
        
        // When: 캐시 존재 여부 확인
        boolean result = postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.WEEKLY);
        
        // Then: false 반환
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey("cache:posts:weekly");
    }

    @Test
    @DisplayName("정상 케이스 - LEGEND 캐시 게시글 목록 조회")
    void shouldReturnCachedPosts_WhenLegendCacheExists() {
        // Given: LEGEND 캐시에 게시글 DTO 목록 존재
        given(valueOperations.get("cache:posts:legend")).willReturn(testSimplePostDTOs);
        
        // When: 캐시된 게시글 목록 조회
        List<PostSearchResult> result = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.LEGEND);
        
        // Then: 캐시된 게시글 목록이 변환되어 반환됨
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("인기 게시글 1");
        assertThat(result.get(1).getTitle()).isEqualTo("인기 게시글 2");
        verify(valueOperations).get("cache:posts:legend");
    }

    @Test
    @DisplayName("정상 케이스 - NOTICE 캐시 미존재 시 빈 목록 반환")
    void shouldReturnEmptyList_WhenNoticeCacheNotExists() {
        // Given: NOTICE 캐시가 존재하지 않음
        given(valueOperations.get("cache:posts:notice")).willReturn(null);
        
        // When: 캐시된 게시글 목록 조회
        List<PostSearchResult> result = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.NOTICE);
        
        // Then: 빈 목록 반환
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(valueOperations).get("cache:posts:notice");
    }

    @Test
    @DisplayName("정상 케이스 - 개별 게시글 캐시 조회")
    void shouldReturnCachedPost_WhenPostCacheExists() {
        // Given: 개별 게시글 캐시에 DTO 존재
        Long postId = 1L;
        String expectedKey = "cache:post:" + postId;
        given(valueOperations.get(expectedKey)).willReturn(testFullPostDTO);
        
        // When: 캐시된 게시글 조회
        PostDetail result = postCacheQueryAdapter.getCachedPost(postId);
        
        // Then: 캐시된 게시글이 변환되어 반환됨
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("상세 게시글");
        assertThat(result.content()).isEqualTo("상세 게시글 내용입니다.");
        assertThat(result.isLiked()).isTrue();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("정상 케이스 - 개별 게시글 캐시 미존재 시 null 반환")
    void shouldReturnNull_WhenPostCacheNotExists() {
        // Given: 개별 게시글 캐시 미존재
        Long postId = 999L;
        String expectedKey = "cache:post:" + postId;
        given(valueOperations.get(expectedKey)).willReturn(null);
        
        // When: 캐시된 게시글 조회
        PostDetail result = postCacheQueryAdapter.getCachedPost(postId);
        
        // Then: null 반환
        assertThat(result).isNull();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("정상 케이스 - 페이지별 캐시 조회")
    void shouldReturnPagedCachedPosts_WhenCacheExistsWithPagination() {
        // Given: 캐시에 5개 게시글 존재 (Value 구조 사용)
        List<PostSearchResult> allPosts = Arrays.asList(
            testSimplePostDTOs.get(0), testSimplePostDTOs.get(1),
            createAdditionalPost(3L, "추가글3"), createAdditionalPost(4L, "추가글4"), createAdditionalPost(5L, "추가글5")
        );
        given(valueOperations.get("cache:posts:realtime")).willReturn(allPosts);
        
        Pageable pageable = PageRequest.of(0, 2);
        
        // When: 페이지별 캐시 조회
        Page<PostSearchResult> result = postCacheQueryAdapter.getCachedPostListPaged(PostCacheFlag.REALTIME, pageable);
        
        // Then: 페이지 데이터 반환 (첫 번째 페이지, 2개 항목)
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5L);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("인기 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("인기 게시글 2");
        
        verify(valueOperations).get("cache:posts:realtime");
    }

    @Test
    @DisplayName("경계값 - 빈 페이지 캐시 조회")
    void shouldReturnEmptyPage_WhenCacheListIsEmpty() {
        // Given: 빈 캐시 (Value 구조에서 null 반환)
        given(valueOperations.get("cache:posts:legend")).willReturn(null);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When: 페이지별 캐시 조회
        Page<PostSearchResult> result = postCacheQueryAdapter.getCachedPostListPaged(PostCacheFlag.LEGEND, pageable);
        
        // Then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
        assertThat(result.getTotalPages()).isEqualTo(0);
        
        verify(valueOperations).get("cache:posts:legend");
    }

    @Test
    @DisplayName("예외 케이스 - Redis 연결 오류 시 hasPopularPostsCache CustomException 발생")
    void shouldThrowCustomException_WhenRedisConnectionFailsInHasCache() {
        // Given: Redis 연결 오류 발생
        given(redisTemplate.hasKey(anyString()))
            .willThrow(new RuntimeException("Redis connection failed"));
        
        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.REALTIME);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_READ_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("예외 케이스 - Redis 읽기 오류 시 getCachedPostList CustomException 발생")
    void shouldThrowCustomException_WhenRedisReadErrorInGetCachedList() {
        // Given: Redis 읽기 오류 발생
        given(valueOperations.get(anyString()))
            .willThrow(new RuntimeException("Redis read failed"));
        
        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheQueryAdapter.getCachedPostList(PostCacheFlag.WEEKLY);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_READ_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("예외 케이스 - Redis 읽기 오류 시 getCachedPost CustomException 발생")
    void shouldThrowCustomException_WhenRedisReadErrorInGetCachedPost() {
        // Given: Redis 읽기 오류 발생
        given(valueOperations.get(anyString()))
            .willThrow(new RuntimeException("Redis read failed"));
        
        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheQueryAdapter.getCachedPost(1L);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_READ_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("예외 케이스 - Redis Value 조회 오류 시 getCachedPostListPaged CustomException 발생")
    void shouldThrowCustomException_WhenRedisValueErrorInGetCachedPaged() {
        // Given: Redis Value 조회 오류 발생
        given(valueOperations.get("cache:posts:weekly"))
            .willThrow(new RuntimeException("Redis value operation failed"));
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheQueryAdapter.getCachedPostListPaged(PostCacheFlag.WEEKLY, pageable);
        })
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> {
            CustomException customEx = (CustomException) ex;
            assertThat(customEx.getStatus()).isEqualTo(ErrorCode.REDIS_READ_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("경계값 - 잘못된 타입의 캐시 데이터가 있을 때 빈 목록 반환")
    void shouldReturnEmptyList_WhenCacheContainsWrongDataType() {
        // Given: 잘못된 타입의 데이터가 캐시에 있음
        given(valueOperations.get("cache:posts:realtime")).willReturn("wrong type data");
        
        // When: 캐시된 게시글 목록 조회
        List<PostSearchResult> result = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.REALTIME);
        
        // Then: 빈 목록 반환 (타입 안전성)
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 잘못된 타입의 개별 게시글 캐시 데이터가 있을 때 null 반환")
    void shouldReturnNull_WhenPostCacheContainsWrongDataType() {
        // Given: 잘못된 타입의 데이터가 캐시에 있음
        Long postId = 1L;
        String expectedKey = "cache:post:" + postId;
        given(valueOperations.get(expectedKey)).willReturn("wrong type data");
        
        // When: 캐시된 게시글 조회
        PostDetail result = postCacheQueryAdapter.getCachedPost(postId);
        
        // Then: null 반환 (타입 안전성)
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("비즈니스 로직 - 모든 캐시 타입별 키 매핑 정확성 검증")
    void shouldUseCorrectKeys_ForAllCacheTypes() {
        // Given: 모든 캐시 타입
        
        // When & Then: 각 캐시 타입에 대해 올바른 키 사용
        
        // REALTIME 키 확인
        given(redisTemplate.hasKey("cache:posts:realtime")).willReturn(true);
        postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(redisTemplate).hasKey("cache:posts:realtime");
        
        // WEEKLY 키 확인
        given(redisTemplate.hasKey("cache:posts:weekly")).willReturn(true);
        postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(redisTemplate).hasKey("cache:posts:weekly");
        
        // LEGEND 키 확인
        given(redisTemplate.hasKey("cache:posts:legend")).willReturn(true);
        postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.LEGEND);
        verify(redisTemplate).hasKey("cache:posts:legend");
        
        // NOTICE 키 확인
        given(redisTemplate.hasKey("cache:posts:notice")).willReturn(true);
        postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.NOTICE);
        verify(redisTemplate).hasKey("cache:posts:notice");
    }

    @Test
    @DisplayName("통합 테스트 - 전체 캐시 조회 워크플로우")
    void shouldCompleteEntireCacheQueryWorkflow() {
        // Given: 다양한 캐시 데이터 존재
        given(redisTemplate.hasKey("cache:posts:realtime")).willReturn(true);
        given(valueOperations.get("cache:posts:realtime")).willReturn(testSimplePostDTOs);
        given(valueOperations.get("cache:post:1")).willReturn(testFullPostDTO);
        
        // When: 전체 워크플로우 실행
        // 1. 캐시 존재 여부 확인
        boolean hasCache = postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.REALTIME);
        
        // 2. 캐시된 게시글 목록 조회
        List<PostSearchResult> posts = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.REALTIME);
        
        // 3. 개별 게시글 상세 조회
        PostDetail postDetail = postCacheQueryAdapter.getCachedPost(1L);
        
        // Then: 모든 단계가 정상 실행됨
        assertThat(hasCache).isTrue();
        assertThat(posts).hasSize(2);
        assertThat(postDetail).isNotNull();
        assertThat(postDetail.title()).isEqualTo("상세 게시글");
        
        // 적절한 Redis 호출 검증
        verify(redisTemplate).hasKey("cache:posts:realtime");
        verify(valueOperations).get("cache:posts:realtime");
        verify(valueOperations).get("cache:post:1");
    }

    @Test
    @DisplayName("성능 테스트 - 대량 캐시 조회")
    void shouldHandleLargeCacheData_WhenRetrievingPosts() {
        
        // Given: 대량 캐시 데이터 (100개 게시글)
        List<PostSearchResult> largePosts = Collections.nCopies(100, testSimplePostDTOs.get(0));
        given(valueOperations.get("cache:posts:weekly")).willReturn(largePosts);
        
        // When: 대량 캐시 데이터 조회
        List<PostSearchResult> result = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.WEEKLY);
        
        // Then: 성능 문제 없이 정상 처리됨
        assertThat(result).isNotNull();
        assertThat(result).hasSize(100);
        
        // 단일 Redis 호출만 발생했는지 확인 (효율성)
        verify(valueOperations, times(1)).get("cache:posts:weekly");
    }

    /**
     * <h3>추가 테스트용 PostSearchResult 생성 헬퍼</h3>
     * <p>페이징 테스트를 위한 추가 게시글 데이터 생성</p>
     */
    private PostSearchResult createAdditionalPost(Long id, String title) {
        return PostSearchResult.builder()
            .id(id)
            .title(title)
            .content("테스트 내용 " + id)
            .viewCount(50)
            .likeCount(25)
            .postCacheFlag(PostCacheFlag.REALTIME)
            .createdAt(Instant.now())
            .userId(id)
            .userName("testUser" + id)
            .commentCount(5)
            .isNotice(false)
            .build();
    }
}