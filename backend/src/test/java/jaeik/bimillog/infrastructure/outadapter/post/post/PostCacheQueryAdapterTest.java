package jaeik.bimillog.infrastructure.outadapter.post.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.adapter.post.out.post.PostCacheQueryAdapter;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.*;
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
    
    @Mock
    private org.springframework.data.redis.core.ZSetOperations<String, Object> zSetOperations;

    private PostCacheQueryAdapter postCacheQueryAdapter;

    private List<PostSearchResult> testSimplePostDTOs;
    private PostDetail testFullPostDTO;

    @BeforeEach
    void setUp() {
        // RedisTemplate Mock 설정 - lenient()로 불필요한 스터빙 허용
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        
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
        // Given: LEGEND 캐시에 Sorted Set으로 ID 저장, 개별 캐시에 상세 정보 저장
        given(zSetOperations.reverseRange("cache:posts:legend", 0, -1))
            .willReturn(java.util.Set.of("1", "2"));
        given(valueOperations.get("cache:post:1")).willReturn(testFullPostDTO);
        given(valueOperations.get("cache:post:2")).willReturn(createTestPostDetail(2L, "인기 게시글 2"));
        
        // When: 캐시된 게시글 목록 조회
        List<PostSearchResult> result = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.LEGEND);
        
        // Then: 캐시된 게시글 목록이 변환되어 반환됨 (Set의 순서는 보장되지 않을 수 있음)
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(PostSearchResult::getTitle).toList())
            .containsExactlyInAnyOrder("상세 게시글", "인기 게시글 2");
        
        verify(zSetOperations).reverseRange("cache:posts:legend", 0, -1);
        verify(valueOperations).get("cache:post:1");
        verify(valueOperations).get("cache:post:2");
    }

    @Test
    @DisplayName("정상 케이스 - NOTICE 캐시 미존재 시 빈 목록 반환")
    void shouldReturnEmptyList_WhenNoticeCacheNotExists() {
        // Given: NOTICE 캐시가 존재하지 않음 (Sorted Set이 비어있음)
        given(zSetOperations.reverseRange("cache:posts:notice", 0, -1))
            .willReturn(java.util.Collections.emptySet());
        
        // When: 캐시된 게시글 목록 조회
        List<PostSearchResult> result = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.NOTICE);
        
        // Then: 빈 목록 반환
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(zSetOperations).reverseRange("cache:posts:notice", 0, -1);
    }

    @Test
    @DisplayName("정상 케이스 - 개별 게시글 캐시 조회")
    void shouldReturnCachedPost_WhenPostCacheExists() {
        // Given: 개별 게시글 캐시에 DTO 존재
        Long postId = 1L;
        String expectedKey = "cache:post:" + postId;
        given(valueOperations.get(expectedKey)).willReturn(testFullPostDTO);
        
        // When: 캐시된 게시글 조회
        PostDetail result = postCacheQueryAdapter.getCachedPostIfExists(postId);
        
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
        PostDetail result = postCacheQueryAdapter.getCachedPostIfExists(postId);
        
        // Then: null 반환
        assertThat(result).isNull();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("정상 케이스 - 페이지별 캐시 조회")
    void shouldReturnPagedCachedPosts_WhenCacheExistsWithPagination() {
        // Given: LEGEND 캐시에 5개 게시글 존재 (ZSet + 개별 캐시 구조)
        // ZSet에는 총 5개 항목이 있고, 첫 번째 페이지(0~1)에는 ID "1", "2"가 있음
        given(zSetOperations.count("cache:posts:legend", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
            .willReturn(5L);
        java.util.Set<Object> orderedSet = new java.util.LinkedHashSet<>();
        orderedSet.add("1");
        orderedSet.add("2");
        given(zSetOperations.reverseRange("cache:posts:legend", 0, 1))
            .willReturn(orderedSet);
        given(valueOperations.get("cache:post:1")).willReturn(testFullPostDTO);
        given(valueOperations.get("cache:post:2")).willReturn(createTestPostDetail(2L, "인기 게시글 2"));
        
        Pageable pageable = PageRequest.of(0, 2);
        
        // When: 페이지별 캐시 조회
        Page<PostSearchResult> result = postCacheQueryAdapter.getCachedPostListPaged(pageable);
        
        // Then: 페이지 데이터 반환 (첫 번째 페이지, 2개 항목)
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5L);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("상세 게시글");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("인기 게시글 2");
        
        verify(zSetOperations).count("cache:posts:legend", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        verify(zSetOperations).reverseRange("cache:posts:legend", 0, 1);
        verify(valueOperations).get("cache:post:1");
        verify(valueOperations).get("cache:post:2");
    }

    @Test
    @DisplayName("경계값 - 빈 페이지 캐시 조회")
    void shouldReturnEmptyPage_WhenCacheListIsEmpty() {
        // Given: 빈 캐시 (ZSet count가 0 반환)
        given(zSetOperations.count("cache:posts:legend", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
            .willReturn(0L);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When: 페이지별 캐시 조회
        Page<PostSearchResult> result = postCacheQueryAdapter.getCachedPostListPaged(pageable);
        
        // Then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
        assertThat(result.getTotalPages()).isEqualTo(0);
        
        verify(zSetOperations).count("cache:posts:legend", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
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
        .isInstanceOf(PostCustomException.class)
        .satisfies(ex -> {
            PostCustomException customEx = (PostCustomException) ex;
            assertThat(customEx.getPostErrorCode().getStatus()).isEqualTo(PostErrorCode.REDIS_READ_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("예외 케이스 - Redis 읽기 오류 시 getCachedPostList CustomException 발생")
    void shouldThrowCustomException_WhenRedisReadErrorInGetCachedList() {
        // Given: Redis ZSet 읽기 오류 발생
        given(zSetOperations.reverseRange("cache:posts:weekly", 0, -1))
            .willThrow(new RuntimeException("Redis read failed"));
        
        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheQueryAdapter.getCachedPostList(PostCacheFlag.WEEKLY);
        })
        .isInstanceOf(PostCustomException.class)
        .satisfies(ex -> {
            PostCustomException customEx = (PostCustomException) ex;
            assertThat(customEx.getPostErrorCode().getStatus()).isEqualTo(PostErrorCode.REDIS_READ_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("예외 케이스 - Redis 읽기 오류 시 getCachedPostIfExists CustomException 발생")
    void shouldThrowCustomException_WhenRedisReadErrorInGetCachedPost() {
        // Given: Redis 읽기 오류 발생
        given(valueOperations.get(anyString()))
            .willThrow(new RuntimeException("Redis read failed"));
        
        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheQueryAdapter.getCachedPostIfExists(1L);
        })
        .isInstanceOf(PostCustomException.class)
        .satisfies(ex -> {
            PostCustomException customEx = (PostCustomException) ex;
            assertThat(customEx.getPostErrorCode().getStatus()).isEqualTo(PostErrorCode.REDIS_READ_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("예외 케이스 - Redis ZSet 조회 오류 시 getCachedPostListPaged CustomException 발생")
    void shouldThrowCustomException_WhenRedisZSetErrorInGetCachedPaged() {
        // Given: Redis ZSet count 조회 오류 발생
        given(zSetOperations.count("cache:posts:legend", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
            .willThrow(new RuntimeException("Redis ZSet operation failed"));
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When & Then: CustomException 발생
        assertThatThrownBy(() -> {
            postCacheQueryAdapter.getCachedPostListPaged(pageable);
        })
        .isInstanceOf(PostCustomException.class)
        .satisfies(ex -> {
            PostCustomException customEx = (PostCustomException) ex;
            assertThat(customEx.getPostErrorCode().getStatus()).isEqualTo(PostErrorCode.REDIS_READ_ERROR.getStatus());
        });
    }

    @Test
    @DisplayName("경계값 - 잘못된 타입의 개별 게시글 캐시 데이터가 있을 때 필터링")
    void shouldFilterOutInvalidData_WhenIndividualCacheContainsWrongDataType() {
        // Given: ZSet에는 정상적인 ID가 있지만, 개별 캐시에 잘못된 타입 데이터 포함
        given(zSetOperations.reverseRange("cache:posts:realtime", 0, -1))
            .willReturn(java.util.Set.of("1", "2"));
        given(valueOperations.get("cache:post:1")).willReturn(testFullPostDTO); // 정상 데이터
        given(valueOperations.get("cache:post:2")).willReturn("wrong type data"); // 잘못된 타입
        
        // When: 캐시된 게시글 목록 조회
        List<PostSearchResult> result = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.REALTIME);
        
        // Then: 유효한 데이터만 반환 (잘못된 타입은 필터링)
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("상세 게시글");
        
        verify(zSetOperations).reverseRange("cache:posts:realtime", 0, -1);
        verify(valueOperations).get("cache:post:1");
        verify(valueOperations).get("cache:post:2");
    }

    @Test
    @DisplayName("경계값 - 잘못된 타입의 개별 게시글 캐시 데이터가 있을 때 null 반환")
    void shouldReturnNull_WhenPostCacheContainsWrongDataType() {
        // Given: 잘못된 타입의 데이터가 캐시에 있음
        Long postId = 1L;
        String expectedKey = "cache:post:" + postId;
        given(valueOperations.get(expectedKey)).willReturn("wrong type data");
        
        // When: 캐시된 게시글 조회
        PostDetail result = postCacheQueryAdapter.getCachedPostIfExists(postId);
        
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
        given(zSetOperations.reverseRange("cache:posts:realtime", 0, -1))
            .willReturn(java.util.Set.of("1", "2"));
        given(valueOperations.get("cache:post:1")).willReturn(testFullPostDTO);
        given(valueOperations.get("cache:post:2")).willReturn(createTestPostDetail(2L, "인기 게시글 2"));
        
        // When: 전체 워크플로우 실행
        // 1. 캐시 존재 여부 확인
        boolean hasCache = postCacheQueryAdapter.hasPopularPostsCache(PostCacheFlag.REALTIME);
        
        // 2. 캐시된 게시글 목록 조회
        List<PostSearchResult> posts = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.REALTIME);
        
        // 3. 개별 게시글 상세 조회
        PostDetail postDetail = postCacheQueryAdapter.getCachedPostIfExists(1L);
        
        // Then: 모든 단계가 정상 실행됨
        assertThat(hasCache).isTrue();
        assertThat(posts).hasSize(2);
        assertThat(postDetail).isNotNull();
        assertThat(postDetail.title()).isEqualTo("상세 게시글");
        
        // 적절한 Redis 호출 검증 (개별 호출도 중복 고려)
        verify(redisTemplate).hasKey("cache:posts:realtime");
        verify(zSetOperations).reverseRange("cache:posts:realtime", 0, -1);
        verify(valueOperations, times(2)).get("cache:post:1"); // getCachedPostList + getCachedPostIfExists
        verify(valueOperations).get("cache:post:2");
    }

    @Test
    @DisplayName("성능 테스트 - 대량 캐시 조회")
    void shouldHandleLargeCacheData_WhenRetrievingPosts() {
        
        // Given: 대량 캐시 데이터 (100개 게시글 ID)
        java.util.Set<Object> largePostIds = new java.util.LinkedHashSet<>();
        for (int i = 1; i <= 100; i++) {
            largePostIds.add(String.valueOf(i));
        }
        given(zSetOperations.reverseRange("cache:posts:weekly", 0, -1))
            .willReturn(largePostIds);
        
        // 모든 개별 캐시에 대해 PostDetail 반환
        for (int i = 1; i <= 100; i++) {
            given(valueOperations.get("cache:post:" + i))
                .willReturn(createTestPostDetail((long) i, "게시글 " + i));
        }
        
        // When: 대량 캐시 데이터 조회
        List<PostSearchResult> result = postCacheQueryAdapter.getCachedPostList(PostCacheFlag.WEEKLY);
        
        // Then: 성능 문제 없이 정상 처리됨
        assertThat(result).isNotNull();
        assertThat(result).hasSize(100);
        
        // 효율적인 Redis 호출 검증 (1회 ZSet 조회 + 100회 개별 조회)
        verify(zSetOperations, times(1)).reverseRange("cache:posts:weekly", 0, -1);
        verify(valueOperations, times(100)).get(startsWith("cache:post:"));
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
    
    /**
     * <h3>테스트용 PostDetail 생성 헬퍼</h3>
     * <p>테스트를 위한 PostDetail 객체 생성</p>
     */
    private PostDetail createTestPostDetail(Long id, String title) {
        return PostDetail.builder()
            .id(id)
            .title(title)
            .content("테스트 내용 " + id)
            .viewCount(100)
            .likeCount(50)
            .postCacheFlag(PostCacheFlag.LEGEND)
            .createdAt(Instant.now())
            .userId(id)
            .userName("testUser" + id)
            .commentCount(10)
            .isNotice(false)
            .isLiked(false)
            .build();
    }
}