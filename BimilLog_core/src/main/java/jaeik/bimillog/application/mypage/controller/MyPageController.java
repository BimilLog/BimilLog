package jaeik.bimillog.application.mypage.controller;

import jaeik.bimillog.application.mypage.dto.MyPageDTO;
import jaeik.bimillog.domain.comment.entity.MemberActivityComment;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.comment.service.CommentQueryService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.service.PostQueryService;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final CommentQueryService commentQueryService;
    private final PostQueryService postQueryService;

    @GetMapping("/")
    public ResponseEntity<MyPageDTO> getUserComments(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                     Pageable pageable) {

        MemberActivityComment memberActivityComment = commentQueryService.getMemberActivityComments(userDetails.getMemberId(), pageable);
        Page<PostSimpleDetail> SimplePostList = postQueryService.getMemberPosts(userDetails.getMemberId(), pageable);
        Page<PostSimpleDetail> likedPosts = postQueryService.getMemberLikedPosts(userDetails.getMemberId(), pageable);

        MyPageDTO myPageDTO = MyPageDTO.from(memberActivityComment, SimplePostList, likedPosts);

        return ResponseEntity.ok(myPageDTO);
    }
}
