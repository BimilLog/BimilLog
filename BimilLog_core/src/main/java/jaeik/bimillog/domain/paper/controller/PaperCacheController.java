package jaeik.bimillog.domain.paper.controller;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.domain.paper.service.PaperCacheService;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>롤링페이퍼 캐시 컨트롤러</h2>
 * <p>Paper 도메인의 캐시 기반 조회를 담당하는 웹 어댑터입니다.</p>
 * <p>실시간 인기 롤링페이퍼 조회 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
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
     * <p>실시간 인기 롤링페이퍼 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보 (page, size, sort)
     * @return 실시간 인기 롤링페이퍼 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<PopularPaperInfo>> popularPaper(Pageable pageable) {
        Page<PopularPaperInfo> paperInfos = paperCacheService.getRealtimePapers(pageable);
        return ResponseEntity.ok(paperInfos);
    }
}
