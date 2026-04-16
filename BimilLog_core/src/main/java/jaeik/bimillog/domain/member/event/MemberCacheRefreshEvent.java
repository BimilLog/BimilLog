package jaeik.bimillog.domain.member.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberCacheRefreshEvent {
    private Pageable pageable;

    public static MemberCacheRefreshEvent from(Pageable pageable) {
        return new MemberCacheRefreshEvent(pageable);
    }
}
