import React from "react";
import { CardContent, SafeHTML } from "@/components";
import { Post } from "@/lib/api";
import { PostContentActions } from "./PostContentActions";

interface PostContentProps {
  post: Post;
  isAuthenticated: boolean;
  onLike: () => void;
}

export const PostContent = React.memo<PostContentProps>(({
  post,
  isAuthenticated,
  onLike,
}) => {
  return (
    <CardContent className="p-6">
      <div className="prose max-w-none">
        <SafeHTML
          html={post.content}
          className="text-brand-primary leading-relaxed whitespace-pre-wrap"
        />
      </div>

      <PostContentActions
        post={post}
        isAuthenticated={isAuthenticated}
        onLike={onLike}
      />
    </CardContent>
  );
});
PostContent.displayName = "PostContent";
