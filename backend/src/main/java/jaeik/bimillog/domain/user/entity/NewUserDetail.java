package jaeik.bimillog.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewUserDetail implements UserDetail{

    private String uuid;

    public static NewUserDetail of(String uuid) {
        return NewUserDetail.builder()
                .uuid(uuid)
                .build();
    }
}
