package jaeik.bimillog.infrastructure.adapter.in.paper.web;

import jaeik.bimillog.domain.paper.application.port.in.PaperCacheUseCase;
import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/paper")
public class PaperCacheController {

    private final PaperCacheUseCase paperCacheUseCase;

    /**
     * <h3>실시간 인기 롤링페이퍼 조회 API</h3>
     * <p>실시간 인기 롤링페이퍼 목록을 조회합니다.</p>
     *
     * @return HTTP 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<PopularPaperInfo>> popularPaper(Pageable pageable) {
        Page<PopularPaperInfo> paperInfos = paperCacheUseCase.getRealtimePapers(pageable);
        return ResponseEntity.ok(paperInfos);
    }
}
