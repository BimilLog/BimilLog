package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.friend.service.FriendAdminService;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>친구 도메인 Redis 복구 어드민 컨트롤러</h2>
 * <p>Redis 데이터 유실 시 어드민이 수동으로 친구 관계 및 상호작용 점수를 재구축합니다.</p>
 *
 * @version 2.7.0
 * @author Jaeik
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        message = "친구 Redis 복구 어드민 API 요청")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/friend")
public class FriendAdminController {

    private final FriendAdminService friendAdminService;

    /**
     * <h3>친구 관계 Redis 전체 재구축 API</h3>
     * <p>DB의 friendship 테이블을 기반으로 Redis friend:* Set을 재구축합니다.</p>
     *
     * @return 처리 결과 메시지
     */
    @PostMapping("/friendship/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rebuildFriendship() {
        friendAdminService.getFriendshipDB();
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>상호작용 점수 Redis 전체 재구축 API</h3>
     * <p>DB의 post_like, comment, comment_like 집계를 기반으로 Redis interaction:* ZSet을 재구축합니다.</p>
     *
     * @return 처리 결과 메시지
     */
    @PostMapping("/interaction-score/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rebuildInteractionScore() {
        friendAdminService.rebuildInteractionScoreRedis();
        return ResponseEntity.ok().build();
    }
}
