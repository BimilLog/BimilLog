package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.in.PaperCacheUseCase;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.port.out.RedisPaperQueryPort;
import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperCacheService implements PaperCacheUseCase{

    private final RedisPaperQueryPort redisPaperQueryPort;
    private final PaperQueryPort paperQueryPort;

    // 페이지당 10개씩 0페이지 토탈 10개만 반환 향후 페이지 확장
    @Override
    public Page<PopularPaperInfo> getRealtimePapers(Pageable pageable) {
        // 1. Redis에서 상위 10개 조회 (memberId, rank, popularityScore)
        List<PopularPaperInfo> popularPapers = redisPaperQueryPort.getRealtimePopularPapersWithRankAndScore();
        if (popularPapers.isEmpty()) {
            return Page.empty();
        }

        // 2. DB에서 memberName과 24시간 이내 메시지 수 조회 후 채우기
        paperQueryPort.enrichPopularPaperInfos(popularPapers);

        // 3. Page로 변환하여 반환
        return new PageImpl<>(popularPapers);
    }
}
