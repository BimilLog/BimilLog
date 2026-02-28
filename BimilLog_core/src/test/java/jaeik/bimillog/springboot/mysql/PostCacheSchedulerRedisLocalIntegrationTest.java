package jaeik.bimillog.springboot.mysql;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.scheduler.PostCacheScheduler;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListQueryAdapter;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

/**
 * <h2>PostCacheScheduler Redis 연동 로컬 통합 테스트</h2>
 * <p>실제 Redis를 사용하여 스케줄러의 캐시 갱신 파이프라인을 검증합니다.</p>
 * <ul>
 *   <li>실시간 인기글: ZSet → JSON LIST 교체 흐름</li>
 *   <li>첫 페이지 캐시: DB 조회 → JSON LIST 교체 흐름</li>
 * </ul>
 * <p>DB 의존성은 Mock으로 대체하여 Redis 연동 부분만 실제 동작을 검증합니다.</p>
 */
@DisplayName("PostCacheScheduler Redis 연동 로컬 통합 테스트")
@SpringBootTest
@Tag("local-integration")
@ActiveProfiles("local-integration")
class PostCacheSchedulerRedisLocalIntegrationTest {

    @Autowired
    private PostCacheScheduler postCacheScheduler;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisPostListQueryAdapter redisPostListQueryAdapter;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private PostQueryRepository postQueryRepository;

    private static final Long POST_ID_1 = 777001L;
    private static final Long POST_ID_2 = 777002L;
    private static final Long POST_ID_3 = 777003L;

    @AfterEach
    void cleanRedis() {
        stringRedisTemplate.delete(RedisKey.FIRST_PAGE_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_REALTIME_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.REALTIME_POST_SCORE_KEY);
    }

    // ==================== 실시간 인기글 ====================

    @Test
    @DisplayName("실시간 인기글: ZSet에 점수가 있으면 JSON LIST가 교체됨")
    void refreshRealtimePopularPosts_shouldWriteToJsonList_WhenZSetHasData() {
        // Given: ZSet에 점수 추가 (실제 Redis)
        stringRedisTemplate.opsForZSet().add(RedisKey.REALTIME_POST_SCORE_KEY, POST_ID_1.toString(), 30.0);
        stringRedisTemplate.opsForZSet().add(RedisKey.REALTIME_POST_SCORE_KEY, POST_ID_2.toString(), 20.0);

        // DB Mock: findAllByIds가 Post 목록 반환
        Post post1 = PostTestDataBuilder.withId(POST_ID_1, PostTestDataBuilder.createPost(null, "실시간글1", "내용1"));
        Post post2 = PostTestDataBuilder.withId(POST_ID_2, PostTestDataBuilder.createPost(null, "실시간글2", "내용2"));
        given(postRepository.findAllByIds(any())).willReturn(List.of(post1, post2));

        // When
        postCacheScheduler.refreshRealtimePopularPosts();

        // Then: POST_REALTIME_JSON_KEY에 해당 글들이 저장됨
        List<PostSimpleDetail> cached = redisPostListQueryAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY);
        assertThat(cached).hasSize(2);
        assertThat(cached).extracting(PostSimpleDetail::getId)
                .containsExactlyInAnyOrder(POST_ID_1, POST_ID_2);
        assertThat(cached).extracting(PostSimpleDetail::getTitle)
                .containsExactlyInAnyOrder("실시간글1", "실시간글2");
    }

    @Test
    @DisplayName("실시간 인기글: ZSet이 비어있으면 JSON LIST가 갱신되지 않음")
    void refreshRealtimePopularPosts_shouldSkipUpdate_WhenZSetIsEmpty() {
        // Given: ZSet 비어있음, 기존 캐시 존재
        stringRedisTemplate.opsForList().rightPush(RedisKey.POST_REALTIME_JSON_KEY, "기존캐시");

        // When
        postCacheScheduler.refreshRealtimePopularPosts();

        // Then: 기존 캐시가 그대로 유지됨 (덮어쓰지 않음)
        Long size = stringRedisTemplate.opsForList().size(RedisKey.POST_REALTIME_JSON_KEY);
        assertThat(size).isEqualTo(1L);
    }

    @Test
    @DisplayName("실시간 인기글: 점수 내림차순 상위 5개만 캐시됨")
    void refreshRealtimePopularPosts_shouldCacheTop5ByScore() {
        // Given: 6개 글에 점수 설정 (ZSet reverseRange 0~4 = top 5)
        for (long i = 1; i <= 6; i++) {
            stringRedisTemplate.opsForZSet().add(RedisKey.REALTIME_POST_SCORE_KEY, String.valueOf(777000 + i), i * 10.0);
        }

        // DB Mock: findAllByIds가 5개 반환
        List<Post> top5Posts = List.of(
                PostTestDataBuilder.withId(777006L, PostTestDataBuilder.createPost(null, "6위글", "내용")),
                PostTestDataBuilder.withId(777005L, PostTestDataBuilder.createPost(null, "5위글", "내용")),
                PostTestDataBuilder.withId(777004L, PostTestDataBuilder.createPost(null, "4위글", "내용")),
                PostTestDataBuilder.withId(777003L, PostTestDataBuilder.createPost(null, "3위글", "내용")),
                PostTestDataBuilder.withId(777002L, PostTestDataBuilder.createPost(null, "2위글", "내용"))
        );
        given(postRepository.findAllByIds(any())).willReturn(top5Posts);

        // When
        postCacheScheduler.refreshRealtimePopularPosts();

        // Then: JSON LIST에 5개가 저장됨 (ZSet에서 상위 5개만 읽으므로)
        List<PostSimpleDetail> cached = redisPostListQueryAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY);
        assertThat(cached).hasSize(5);
    }

    // ==================== 첫 페이지 캐시 ====================

    @Test
    @DisplayName("첫 페이지 캐시: DB에 게시글이 있으면 JSON LIST가 교체됨")
    void refreshFirstPageCache_shouldWriteToJsonList_WhenDbHasPosts() {
        // Given: DB Mock
        List<PostSimpleDetail> dbPosts = List.of(
                PostTestDataBuilder.createPostSearchResult(POST_ID_1, "첫페이지글1"),
                PostTestDataBuilder.createPostSearchResult(POST_ID_2, "첫페이지글2"),
                PostTestDataBuilder.createPostSearchResult(POST_ID_3, "첫페이지글3")
        );
        given(postQueryRepository.findBoardPostsByCursor(isNull(), any(int.class))).willReturn(dbPosts);

        // When
        postCacheScheduler.refreshFirstPageCache();

        // Then: FIRST_PAGE_JSON_KEY에 해당 글들이 저장됨
        List<PostSimpleDetail> cached = redisPostListQueryAdapter.getAll(RedisKey.FIRST_PAGE_JSON_KEY);
        assertThat(cached).hasSize(3);
        assertThat(cached).extracting(PostSimpleDetail::getId)
                .containsExactly(POST_ID_1, POST_ID_2, POST_ID_3);
    }

    @Test
    @DisplayName("첫 페이지 캐시: DB가 비어있으면 JSON LIST가 갱신되지 않음")
    void refreshFirstPageCache_shouldSkipUpdate_WhenDbIsEmpty() {
        // Given: DB 비어있음, 기존 캐시 존재
        given(postQueryRepository.findBoardPostsByCursor(isNull(), any(int.class))).willReturn(List.of());
        stringRedisTemplate.opsForList().rightPush(RedisKey.FIRST_PAGE_JSON_KEY, "기존캐시");

        // When
        postCacheScheduler.refreshFirstPageCache();

        // Then: 기존 캐시가 그대로 유지됨
        Long size = stringRedisTemplate.opsForList().size(RedisKey.FIRST_PAGE_JSON_KEY);
        assertThat(size).isEqualTo(1L);
    }

    @Test
    @DisplayName("첫 페이지 캐시: 새 글 목록으로 기존 JSON LIST가 완전 교체됨")
    void refreshFirstPageCache_shouldReplaceExistingCache() {
        // Given: 기존 캐시가 있는 상태에서 새 글 목록으로 교체
        stringRedisTemplate.opsForList().rightPush(RedisKey.FIRST_PAGE_JSON_KEY, "낡은캐시1");
        stringRedisTemplate.opsForList().rightPush(RedisKey.FIRST_PAGE_JSON_KEY, "낡은캐시2");

        List<PostSimpleDetail> freshPosts = List.of(
                PostTestDataBuilder.createPostSearchResult(POST_ID_1, "새글1"),
                PostTestDataBuilder.createPostSearchResult(POST_ID_2, "새글2")
        );
        given(postQueryRepository.findBoardPostsByCursor(isNull(), any(int.class))).willReturn(freshPosts);

        // When
        postCacheScheduler.refreshFirstPageCache();

        // Then: 기존 2개가 아닌 새 2개로 교체됨
        List<PostSimpleDetail> cached = redisPostListQueryAdapter.getAll(RedisKey.FIRST_PAGE_JSON_KEY);
        assertThat(cached).hasSize(2);
        assertThat(cached.get(0).getTitle()).isEqualTo("새글1");
        assertThat(cached.get(1).getTitle()).isEqualTo("새글2");
    }
}
