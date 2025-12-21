package jaeik.bimillog.performance;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.service.PostQueryService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StopWatch;

/**
 * <h2>게시판 조회 성능 테스트</h2>
 * <p>bimillogTest-seed.sql의 데이터를 기반으로 게시판 목록 조회 성능을 측정합니다.</p>
 * <p>데이터: 게시글 10,060개 (일반 10,000 + 주간 인기글 10 + 레전드 인기글 50)</p>
 *
 * @author Jaeik
 */
@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "spring.scheduling.enabled=false"
})
@ActiveProfiles("local-integration")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostBoardQueryTest {

    private static final Logger log = LoggerFactory.getLogger(PostBoardQueryTest.class);

    private static final long TARGET_MEMBER_ID = 1L;
    private static final PageRequest FIRST_PAGE_REQUEST = PageRequest.of(0, 20);
    private static final PageRequest MIDDLE_PAGE_REQUEST = PageRequest.of(250, 20);
    private static final PageRequest LAST_PAGE_REQUEST = PageRequest.of(499, 20);
    private static final long THRESHOLD_MS = 500L; // 500ms 이내

    @Autowired
    private PostQueryService postQueryService;

    @MockitoBean
    private ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor;

    @BeforeAll
    void warmup() {
        log.info("DB 캐시 워밍업 시작 - 게시판 목록 조회");
        postQueryService.getBoard(FIRST_PAGE_REQUEST, null);
        log.info("DB 캐시 워밍업 완료");
    }

    @Test
    @DisplayName("게시판 목록 조회 - 첫 페이지 (비회원)")
    void getBoardFirstPage_Anonymous() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<PostSimpleDetail> result = postQueryService.getBoard(FIRST_PAGE_REQUEST, null);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("게시판 목록 조회 (첫 페이지, 비회원): {} ms, totalElements: {}",
                elapsedMs, result.getTotalElements());

        Assertions.assertThat(elapsedMs)
                .as("게시판 목록 조회가 %d ms 이내로 끝나야 합니다", THRESHOLD_MS)
                .isLessThan(THRESHOLD_MS);
    }

    @Test
    @DisplayName("게시판 목록 조회 - 첫 페이지 (회원)")
    void getBoardFirstPage_Member() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<PostSimpleDetail> result = postQueryService.getBoard(FIRST_PAGE_REQUEST, TARGET_MEMBER_ID);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("게시판 목록 조회 (첫 페이지, 회원 ID={}): {} ms, totalElements: {}",
                TARGET_MEMBER_ID, elapsedMs, result.getTotalElements());

        Assertions.assertThat(elapsedMs)
                .as("게시판 목록 조회가 %d ms 이내로 끝나야 합니다", THRESHOLD_MS)
                .isLessThan(THRESHOLD_MS);
    }

    @Test
    @DisplayName("게시판 목록 조회 - 중간 페이지")
    void getBoardMiddlePage() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<PostSimpleDetail> result = postQueryService.getBoard(MIDDLE_PAGE_REQUEST, null);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("게시판 목록 조회 (중간 페이지 page={}): {} ms, totalElements: {}",
                MIDDLE_PAGE_REQUEST.getPageNumber(), elapsedMs, result.getTotalElements());

        Assertions.assertThat(elapsedMs)
                .as("게시판 목록 중간 페이지 조회가 %d ms 이내로 끝나야 합니다", THRESHOLD_MS)
                .isLessThan(THRESHOLD_MS);
    }

    @Test
    @DisplayName("게시판 목록 조회 - 마지막 페이지")
    void getBoardLastPage() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<PostSimpleDetail> result = postQueryService.getBoard(LAST_PAGE_REQUEST, null);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("게시판 목록 조회 (마지막 페이지 page={}): {} ms, totalElements: {}",
                LAST_PAGE_REQUEST.getPageNumber(), elapsedMs, result.getTotalElements());

        Assertions.assertThat(elapsedMs)
                .as("게시판 목록 마지막 페이지 조회가 %d ms 이내로 끝나야 합니다", THRESHOLD_MS)
                .isLessThan(THRESHOLD_MS);
    }

    @Test
    @DisplayName("게시판 목록 조회 - 연속 조회 (캐시 효과)")
    void getBoardRepeated() {
        // 1차 조회
        StopWatch stopWatch1 = new StopWatch();
        stopWatch1.start();
        Page<PostSimpleDetail> result1 = postQueryService.getBoard(FIRST_PAGE_REQUEST, null);
        stopWatch1.stop();
        long firstQueryMs = stopWatch1.getTotalTimeMillis();

        // 2차 조회 (캐시 효과 기대)
        StopWatch stopWatch2 = new StopWatch();
        stopWatch2.start();
        Page<PostSimpleDetail> result2 = postQueryService.getBoard(FIRST_PAGE_REQUEST, null);
        stopWatch2.stop();
        long secondQueryMs = stopWatch2.getTotalTimeMillis();

        log.info("게시판 목록 연속 조회 - 1차: {} ms, 2차: {} ms, totalElements: {}",
                firstQueryMs, secondQueryMs, result1.getTotalElements());

        Assertions.assertThat(secondQueryMs)
                .as("연속 조회 시 캐시 효과로 더 빨라야 합니다")
                .isLessThanOrEqualTo(firstQueryMs);
    }
}
