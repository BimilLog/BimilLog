package jaeik.bimillog.springboot.mysql.performance;

import jaeik.bimillog.domain.post.dto.CursorPageResponse;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.service.PostQueryService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.StopWatch;

/**
 * <h2>게시판 조회 성능 테스트 (커서 기반)</h2>
 * <p>bimillogTest-seed.sql의 데이터를 기반으로 게시판 목록 조회 성능을 측정합니다.</p>
 * <p>데이터: 게시글 10,060개 (일반 10,000 + 주간 인기글 10 + 레전드 인기글 50)</p>
 *
 * @author Jaeik
 */
@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "spring.scheduling.enabled=false"
})
@Tag("local-integration")
@Tag("performance")
@ActiveProfiles("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(scripts = "/bimillogTest-seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class PostBoardQueryTest {

    private static final Logger log = LoggerFactory.getLogger(PostBoardQueryTest.class);

    private static final long TARGET_MEMBER_ID = 1L;
    private static final int PAGE_SIZE = 20;
    private static final long THRESHOLD_MS = 500L; // 500ms 이내

    // 커서 기반 테스트용 - 중간/마지막 커서는 첫 페이지 조회 후 설정
    private Long middleCursor;
    private Long lastCursor;

    @Autowired
    private PostQueryService postQueryService;

    @MockitoBean
    private ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor;

    @BeforeAll
    void warmup() {
        log.info("DB 캐시 워밍업 시작 - 게시판 목록 조회 (커서 기반)");
        CursorPageResponse<PostSimpleDetail> result = postQueryService.getBoardByCursor(null, PAGE_SIZE, TARGET_MEMBER_ID);
        log.info("DB 캐시 워밍업 완료");

        // 중간/마지막 페이지 테스트를 위한 커서 설정
        // 중간 커서: 약 5000번째 게시글 (전체의 절반)
        middleCursor = 5000L;
        // 마지막 커서: 약 100번째 게시글 (거의 끝)
        lastCursor = 100L;
    }

    @Test
    @DisplayName("회원 게시판 목록 조회 - 첫 페이지 (커서 없음)")
    void getBoardFirstPage() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        CursorPageResponse<PostSimpleDetail> result = postQueryService.getBoardByCursor(null, PAGE_SIZE, TARGET_MEMBER_ID);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("게시판 목록 조회 (첫 페이지, 회원 ID={}): {} ms, size: {}, nextCursor: {}",
                TARGET_MEMBER_ID, elapsedMs, result.content().size(), result.nextCursor());

        Assertions.assertThat(elapsedMs)
                .as("게시판 목록 조회가 %d ms 이내로 끝나야 합니다", THRESHOLD_MS)
                .isLessThan(THRESHOLD_MS);
    }

    @Test
    @DisplayName("회원 게시판 목록 조회 - 중간 페이지 (커서 기반)")
    void getBoardMiddlePage() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        CursorPageResponse<PostSimpleDetail> result = postQueryService.getBoardByCursor(middleCursor, PAGE_SIZE, TARGET_MEMBER_ID);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("게시판 목록 조회 (중간 페이지 cursor={}, 회원 ID={}): {} ms, size: {}, nextCursor: {}",
                middleCursor, TARGET_MEMBER_ID, elapsedMs, result.content().size(), result.nextCursor());

        Assertions.assertThat(elapsedMs)
                .as("게시판 목록 중간 페이지 조회가 %d ms 이내로 끝나야 합니다", THRESHOLD_MS)
                .isLessThan(THRESHOLD_MS);
    }

    @Test
    @DisplayName("회원 게시판 목록 조회 - 마지막 페이지 (커서 기반)")
    void getBoardLastPage() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        CursorPageResponse<PostSimpleDetail> result = postQueryService.getBoardByCursor(lastCursor, PAGE_SIZE, TARGET_MEMBER_ID);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("게시판 목록 조회 (마지막 페이지 cursor={}, 회원 ID={}): {} ms, size: {}, nextCursor: {}",
                lastCursor, TARGET_MEMBER_ID, elapsedMs, result.content().size(), result.nextCursor());

        Assertions.assertThat(elapsedMs)
                .as("게시판 목록 마지막 페이지 조회가 %d ms 이내로 끝나야 합니다", THRESHOLD_MS)
                .isLessThan(THRESHOLD_MS);
    }
}
