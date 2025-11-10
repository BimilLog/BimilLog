package jaeik.bimillog.infrastructure.api.social.naver;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "naver-auth", url = "https://nid.naver.com")
public interface NaverAuthClient {


}
