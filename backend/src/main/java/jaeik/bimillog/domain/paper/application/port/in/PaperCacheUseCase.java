package jaeik.bimillog.domain.paper.application.port.in;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaperCacheUseCase {

    Page<PopularPaperInfo> getRealtimePapers(Pageable pageable);
}
