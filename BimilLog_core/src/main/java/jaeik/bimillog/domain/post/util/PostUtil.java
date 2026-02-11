package jaeik.bimillog.domain.post.util;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostUtil {

    public Page<PostSimpleDetail> paginate(List<PostSimpleDetail> posts, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), posts.size());

        if (start >= posts.size()) {
            return new PageImpl<>(List.of(), pageable, posts.size());
        }

        return new PageImpl<>(posts.subList(start, end), pageable, posts.size());
    }
}
