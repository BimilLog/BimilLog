package jaeik.bimillog.domain.member.entity;

import lombok.Getter;

/**
 * 친구관계 상태
 */
@Getter
public enum FriendStatus {
    PENDING, // 대기 중
    ACCEPTED // 수락
}
