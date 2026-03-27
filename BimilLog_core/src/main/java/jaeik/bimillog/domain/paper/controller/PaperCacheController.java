package jaeik.bimillog.domain.paper.controller;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.domain.paper.service.PaperCacheService;
import jaeik.bimillog.domain.global.dto.CursorPageResponse;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>롤링페이퍼 캐시 컨트롤러</h2>
 * <p>Paper 도메인의 캐시 기반 조회를 담당하는 웹 어댑터입니다.</p>
 * <p>실시간 인기 롤링페이퍼 조회 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/paper")
public class PaperCacheController {
    private final PaperCacheService paperCacheService;

    /**
     * <h3>실시간 인기 롤링페이퍼 조회 API</h3>
     * <p>실시간 인기 롤링페이퍼 목록을 커서 기반 페이지네이션으로 조회합니다.</p>
     *
     * @param cursor 마지막으로 조회한 순위 (null이면 처음부터)
     * @param size 조회할 개수 (기본값: 10)
     * @return CursorPageResponse 커서 기반 페이지 응답
     */
    @GetMapping("/popular")
    public ResponseEntity<CursorPageResponse<PopularPaperInfo>> popularPaper(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {
        CursorPageResponse<PopularPaperInfo> paperInfos = paperCacheService.getRealtimePapers(cursor, size);
        return ResponseEntity.ok(paperInfos);
    }
}
