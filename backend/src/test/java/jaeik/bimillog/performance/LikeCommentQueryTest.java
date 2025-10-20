package jaeik.bimillog.performance;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

// bimillogTest-seed.sql의 상황을 기반으로한다
@SpringBootTest
@ActiveProfiles("local-integration")
@Tag("integration")
class LikeCommentQueryTest {

    private static final Logger log = LoggerFactory.getLogger(LikeCommentQueryTest.class);

    private static final long TARGET_MEMBER_ID = 1L;
    private static final long GENERAL_MEMBER_ID = 2L;
    private static final PageRequest PAGE_REQUEST = PageRequest.of(0, 50);
    private static final PageRequest MIDDLE_PAGE_REQUEST = PageRequest.of(200, 50);
    private static final long TARGET_LIKE_EXPECTATION = 20_000L;
    private static final long GENERAL_LIKE_EXPECTATION = 90L;
    private static final long TARGET_THRESHOLD_MS = 1_500L;
    private static final long GENERAL_THRESHOLD_MS = 700L;

    @Autowired
    private CommentQueryUseCase commentQueryUseCase;

    @Test
    @DisplayName("타겟 회원의 추천 댓글 조회가 임계 시간 이내로 수행된다")
    void likedCommentsQueryForTargetMember() {
        Page<SimpleCommentInfo> warmup = commentQueryUseCase.getMemberLikedComments(TARGET_MEMBER_ID, PAGE_REQUEST);
        Assumptions.assumeTrue(warmup.getTotalElements() >= TARGET_LIKE_EXPECTATION,
            "bimillogTest 시드 데이터(타겟 회원 2만 추천)가 필요합니다.");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<SimpleCommentInfo> result = commentQueryUseCase.getMemberLikedComments(TARGET_MEMBER_ID, PAGE_REQUEST);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("타겟 회원({}) 추천 댓글 조회 시간: {} ms, totalElements: {}", TARGET_MEMBER_ID, elapsedMs,
            result.getTotalElements());

        Assertions.assertThat(elapsedMs)
            .as("타겟 회원 추천 댓글 조회가 %d ms 이내로 끝나야 합니다", TARGET_THRESHOLD_MS)
            .isLessThan(TARGET_THRESHOLD_MS);
    }

    @Test
    @DisplayName("일반 회원의 추천 댓글 조회도 안정적인 성능을 유지한다")
    void likedCommentsQueryForGeneralMember() {
        Page<SimpleCommentInfo> warmup = commentQueryUseCase.getMemberLikedComments(GENERAL_MEMBER_ID, PAGE_REQUEST);
        Assumptions.assumeTrue(warmup.getTotalElements() >= GENERAL_LIKE_EXPECTATION,
            "bimillogTest 시드 데이터(일반 회원 9십 건 이상 추천)가 필요합니다.");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<SimpleCommentInfo> result = commentQueryUseCase.getMemberLikedComments(GENERAL_MEMBER_ID, PAGE_REQUEST);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("일반 회원({}) 추천 댓글 조회 시간: {} ms, totalElements: {}", GENERAL_MEMBER_ID, elapsedMs,
            result.getTotalElements());

        Assertions.assertThat(elapsedMs)
            .as("일반 회원 추천 댓글 조회가 %d ms 이내로 끝나야 합니다", GENERAL_THRESHOLD_MS)
            .isLessThan(GENERAL_THRESHOLD_MS);
    }

    @Test
    @DisplayName("타겟 회원의 추천 댓글 중간 페이지 조회가 임계 시간 이내로 수행된다")
    void likedCommentsQueryForTargetMemberMiddlePage() {
        Page<SimpleCommentInfo> warmup = commentQueryUseCase.getMemberLikedComments(TARGET_MEMBER_ID, MIDDLE_PAGE_REQUEST);
        Assumptions.assumeTrue(warmup.getTotalElements() >= TARGET_LIKE_EXPECTATION,
            "bimillogTest 시드 데이터(타겟 회원 2만 추천)가 필요합니다.");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<SimpleCommentInfo> result = commentQueryUseCase.getMemberLikedComments(TARGET_MEMBER_ID, MIDDLE_PAGE_REQUEST);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("타겟 회원({}) 추천 댓글 중간 페이지(page={}) 조회 시간: {} ms, totalElements: {}",
            TARGET_MEMBER_ID, MIDDLE_PAGE_REQUEST.getPageNumber(), elapsedMs, result.getTotalElements());

        Assertions.assertThat(elapsedMs)
            .as("타겟 회원 추천 댓글 중간 페이지 조회가 %d ms 이내로 끝나야 합니다", TARGET_THRESHOLD_MS)
            .isLessThan(TARGET_THRESHOLD_MS);
    }

    @Test
    @DisplayName("타겟 회원의 추천 댓글 마지막 페이지 조회가 임계 시간 이내로 수행된다")
    void likedCommentsQueryForTargetMemberLastPage() {
        Page<SimpleCommentInfo> warmup = commentQueryUseCase.getMemberLikedComments(TARGET_MEMBER_ID, PAGE_REQUEST);
        Assumptions.assumeTrue(warmup.getTotalElements() >= TARGET_LIKE_EXPECTATION,
            "bimillogTest 시드 데이터(타겟 회원 2만 추천)가 필요합니다.");

        int lastPageNumber = warmup.getTotalPages() - 1;
        PageRequest lastPageRequest = PageRequest.of(lastPageNumber, 50);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Page<SimpleCommentInfo> result = commentQueryUseCase.getMemberLikedComments(TARGET_MEMBER_ID, lastPageRequest);
        stopWatch.stop();

        long elapsedMs = stopWatch.getTotalTimeMillis();
        log.info("타겟 회원({}) 추천 댓글 마지막 페이지(page={}/{}) 조회 시간: {} ms, totalElements: {}",
            TARGET_MEMBER_ID, lastPageNumber, warmup.getTotalPages(), elapsedMs, result.getTotalElements());

        Assertions.assertThat(elapsedMs)
            .as("타겟 회원 추천 댓글 마지막 페이지 조회가 %d ms 이내로 끝나야 합니다", TARGET_THRESHOLD_MS)
            .isLessThan(TARGET_THRESHOLD_MS);
    }
}
