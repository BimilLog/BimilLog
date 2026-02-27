package jaeik.bimillog.performance;

import jaeik.bimillog.domain.comment.entity.MemberActivityComment;
import jaeik.bimillog.domain.comment.repository.CommentQueryRepository;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.StopWatch;

// bimillogTest-seed.sql의 상황을 기반으로한다
@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "spring.scheduling.enabled=false"
})
@ActiveProfiles("local-integration")
@Tag("local-integration")
@Tag("performance")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LikeCommentQueryTest {

    private static final Logger log = LoggerFactory.getLogger(LikeCommentQueryTest.class);

    private static final long TARGET_MEMBER_ID = 1L;
    private static final long GENERAL_MEMBER_ID = 2L;
    private static final PageRequest PAGE_REQUEST = PageRequest.of(0, 50);
    private static final PageRequest MIDDLE_PAGE_REQUEST = PageRequest.of(200, 50);
    private static final PageRequest LAST_PAGE_REQUEST = PageRequest.of(399, 50);
    private static final long TARGET_THRESHOLD_MS = 1_500L;
    private static final long GENERAL_THRESHOLD_MS = 700L;

    @Autowired
    private CommentQueryRepository commentQueryRepository;

    @MockitoBean
    private ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor;

    @BeforeAll
    void warmup() {
        log.info("DB 캐시 워밍업 시작");
        commentQueryRepository.findLikedCommentsByMemberId(TARGET_MEMBER_ID, PAGE_REQUEST);
        log.info("DB 캐시 워밍업 완료");
    }

    @Test
    @DisplayName("타겟 회원의 추천 댓글 조회 첫 페이지")
    void likedCommentsQueryForTargetMember() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<MemberActivityComment.SimpleCommentInfo> result = commentQueryRepository.findLikedCommentsByMemberId(TARGET_MEMBER_ID, PAGE_REQUEST);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("타겟 회원({}) 추천 댓글 조회 시간: {} ms, totalElements: {}", TARGET_MEMBER_ID, elapsedMs,
            result.getTotalElements());
    }

    @Test
    @DisplayName("타겟 회원의 추천 댓글 조회 중간 페이지")
    void likedCommentsQueryForTargetMemberMiddlePage() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<MemberActivityComment.SimpleCommentInfo> result = commentQueryRepository.findLikedCommentsByMemberId(TARGET_MEMBER_ID, MIDDLE_PAGE_REQUEST);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("타겟 회원({}) 추천 댓글 중간 페이지(page={}) 조회 시간: {} ms, totalElements: {}",
                TARGET_MEMBER_ID, MIDDLE_PAGE_REQUEST.getPageNumber(), elapsedMs, result.getTotalElements());

        Assertions.assertThat(elapsedMs)
                .as("타겟 회원 추천 댓글 중간 페이지 조회가 %d ms 이내로 끝나야 합니다", TARGET_THRESHOLD_MS)
                .isLessThan(TARGET_THRESHOLD_MS);
    }

    @Test
    @DisplayName("타겟 회원의 추천 댓글 조회 마지막 페이지")
    void likedCommentsQueryForTargetMemberLastPage() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<MemberActivityComment.SimpleCommentInfo> result = commentQueryRepository.findLikedCommentsByMemberId(TARGET_MEMBER_ID, LAST_PAGE_REQUEST);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("타겟 회원({}) 추천 댓글 마지막 페이지(page={}) 조회 시간: {} ms, totalElements: {}",
                TARGET_MEMBER_ID, LAST_PAGE_REQUEST.getPageNumber(), elapsedMs, result.getTotalElements());

        Assertions.assertThat(elapsedMs)
                .as("타겟 회원 추천 댓글 마지막 페이지 조회가 %d ms 이내로 끝나야 합니다", TARGET_THRESHOLD_MS)
                .isLessThan(TARGET_THRESHOLD_MS);
    }

    @Test
    @DisplayName("일반 회원의 추천 댓글 조회")
    void likedCommentsQueryForGeneralMember() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<MemberActivityComment.SimpleCommentInfo> result = commentQueryRepository.findLikedCommentsByMemberId(GENERAL_MEMBER_ID, PAGE_REQUEST);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("일반 회원({}) 추천 댓글 조회 시간: {} ms, totalElements: {}", GENERAL_MEMBER_ID, elapsedMs,
            result.getTotalElements());

        Assertions.assertThat(elapsedMs)
            .as("일반 회원 추천 댓글 조회가 %d ms 이내로 끝나야 합니다", GENERAL_THRESHOLD_MS)
            .isLessThan(GENERAL_THRESHOLD_MS);
    }
}
