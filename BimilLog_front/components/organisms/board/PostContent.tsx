import React from "react";
import { CardContent, SafeHTML } from "@/components";
import { Post } from "@/lib/api";
import { PostContentActions } from "./PostContentActions";

interface PostContentProps {
  post: Post;
  isAuthenticated: boolean;
  onLike: () => void;
  isLiking?: boolean;
}

export const PostContent = React.memo<PostContentProps>(({
  post,
  isAuthenticated,
  onLike,
  isLiking = false,
}) => {
  return (
    <CardContent className="p-6">
      {/* 라운드 8: prose dark:prose-invert + break-keep + paper 토큰 */}
      <div className="prose dark:prose-invert max-w-none">
        <SafeHTML
          html={post.content}
          className="text-ink dark:text-paper-50 leading-relaxed whitespace-pre-wrap break-keep"
        />
      </div>

      <PostContentActions
        post={post}
        isAuthenticated={isAuthenticated}
        onLike={onLike}
        isLiking={isLiking}
      />
    </CardContent>
  );
});
PostContent.displayName = "PostContent";
