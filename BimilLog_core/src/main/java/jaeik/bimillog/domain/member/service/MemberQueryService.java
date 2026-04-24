package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.controller.MemberQueryController;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.domain.member.repository.SettingRepository;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter.CachedMemberPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <h2>사용자 조회 서비스</h2>
 *
 * @author Jaeik
 * @version 2.9.0
 */
@Service
@RequiredArgsConstructor
public class MemberQueryService {
    private final MemberQueryRepository memberQueryRepository;
    private final MemberRepository memberRepository;
    private final SettingRepository settingRepository;
    private final RedisMemberAdapter redisMemberAdapter;
    private final MemberCacheRefresher memberCacheRefresher;
    private final ConcurrentHashMap<String, CompletableFuture<Page<SimpleMemberDTO>>> inFlight = new ConcurrentHashMap<>();

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     * <p>해당 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     * <p>{@link MemberQueryController}에서 닉네임 중복 확인 API 시 호출됩니다.</p>
     *
     * @param memberName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public boolean existsByMemberName(String memberName) {
        return memberRepository.existsByMemberName(memberName);
    }

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>닉네임을 사용하여 사용자를 조회합니다.</p>
     *
     * @param memberName 사용자 닉네임
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Optional<Member> findByMemberName(String memberName) {
        return memberRepository.findByMemberName(memberName);
    }

    /**
     * <h3>설정 ID로 설정 조회</h3>
     * <p>Member 엔티티 전체 조회 없이 Setting만 직접 조회합니다.</p>
     * <p>{@link MemberQueryController}에서 사용자 설정 조회 API 시 호출됩니다.</p>
     *
     * @param settingId 설정 ID
     * @return 설정 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    @Transactional(readOnly = true)
    public Setting findBySettingId(Long settingId) {
        return settingRepository.findById(settingId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_SETTINGS_NOT_FOUND));
    }

    /**
     * <h3>여러 사용자 ID로 사용자명 배치 조회</h3>
     * <p>여러 사용자 ID에 해당하는 사용자명을 한 번에 조회합니다.</p>
     *
     * @param memberIds 조회할 사용자 ID 목록
     * @return Map<Long, String> 사용자 ID를 키로, 사용자명을 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Map<Long, String> findMemberNamesByIds(List<Long> memberIds) {
        return memberQueryRepository.findMemberNamesByIds(memberIds);
    }

    /**
     * <h3>모든 회원 페이지 조회</h3>
     * <p>캐시를 조회한다. 캐시가 없으면 DB에서 조회 후 갱신하고 반환한다.</p>
     *
     * @param pageable 페이지 정보
     * @return Page<Member> 조회된 회원 페이지
     */
    public Page<SimpleMemberDTO> findAllMembers(Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        String flightKey = page + ":" + size;

        CachedMemberPage cached = redisMemberAdapter.lookup(page, size);
        if (cached != null) {
            Page<SimpleMemberDTO> data = toPage(cached, page, size);
            if (cached.isStale()) {
                CompletableFuture<Page<SimpleMemberDTO>> marker = CompletableFuture.completedFuture(data);
                if (inFlight.putIfAbsent(flightKey, marker) == null) {
                    memberCacheRefresher.refresh(page, size, () -> inFlight.remove(flightKey, marker));
                }
            }
            return data;
        }

        CompletableFuture<Page<SimpleMemberDTO>> newFuture = new CompletableFuture<>();
        CompletableFuture<Page<SimpleMemberDTO>> existing = inFlight.putIfAbsent(flightKey, newFuture);
        if (existing != null) {
            try {
                return existing.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                CachedMemberPage again = redisMemberAdapter.lookup(page, size);
                return again != null ? toPage(again, page, size) : Page.empty();
            }
        }

        try {
            Page<SimpleMemberDTO> result = memberQueryRepository.findAllMembers(pageable);
            redisMemberAdapter.saveMemberPage(page, size, result.getContent());
            newFuture.complete(result);
            return result;
        } catch (Exception e) {
            newFuture.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(flightKey, newFuture);
        }
    }

    private static Page<SimpleMemberDTO> toPage(CachedMemberPage cached, int page, int size) {
        return new PageImpl<>(cached.data(), PageRequest.of(page, size), cached.data().size());
    }

    /**
     * <h3>사용자명 검색</h3>
     * <p>검색어로 사용자명을 검색합니다.</p>
     * <p>검색 전략: 4글자 이상이면 접두사 검색, 그 외에는 부분 검색을 사용합니다.</p>
     * <p>{@link MemberQueryController}에서 사용자 검색 API 시 호출됩니다.</p>
     *
     * @param query    검색어
     * @param pageable 페이징 정보
     * @return Page<SimpleMemberDTO> 검색된 사용자명 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimpleMemberDTO> searchMembers(String query, Pageable pageable) {
        if (query.length() >= 4) {
            return memberRepository.findByMemberNameStartingWithOrderByMemberNameAsc(query, pageable);
        }
        return memberRepository.findByMemberNameContainingOrderByMemberNameAsc(query, pageable);
    }

    /**
     * <h3>SocialId와 Provider로 사용자 조회</h3>
     *
     * @param provider 제공자
     * @param socialId 소셜Id
     * @return Optional<Member> 사용자
     * @since 2.1.0
     */
    public Optional<Member> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return memberRepository.findByProviderAndSocialId(provider, socialId);
    }

    public List<String> fcmEligibleFcmTokens(Long memberId, NotificationType type) {
        return memberQueryRepository.fcmEligibleFcmTokens(memberId, type);
    }
}
