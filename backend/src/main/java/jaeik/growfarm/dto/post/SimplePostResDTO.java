package jaeik.growfarm.dto.post;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jaeik.growfarm.entity.user.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * <h2>게시글 목록용 DTO</h2>
 * <p>
 * 게시글 목록 보기용 간단한 데이터 전송 객체
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SimplePostResDTO extends BasePostDisplayDTO {

    @JsonIgnore
    private Users user;

}
