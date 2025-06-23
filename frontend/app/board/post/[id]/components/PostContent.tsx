import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { ThumbsUp } from "lucide-react";
import { Post } from "@/lib/api";

interface PostContentProps {
  post: Post;
  isAuthenticated: boolean;
  onLike: () => void;
}

export const PostContent: React.FC<PostContentProps> = ({
  post,
  isAuthenticated,
  onLike,
}) => {
  return (
    <CardContent className="p-6">
      <div className="prose max-w-none">
        <div
          className="text-gray-800 leading-relaxed"
          dangerouslySetInnerHTML={{ __html: post.content }}
        />
      </div>

      {/* 추천 버튼 */}
      <div className="flex items-center justify-center mt-8 pt-6 border-t">
        <Button
          onClick={onLike}
          variant={post.userLike ? "default" : "outline"}
          className={`${
            post.userLike
              ? "bg-blue-500 hover:bg-blue-600 text-white"
              : "hover:bg-blue-50 border-blue-300 text-blue-600"
          } transition-colors duration-200`}
          disabled={!isAuthenticated}
        >
          <ThumbsUp
            className={`w-4 h-4 mr-2 ${post.userLike ? "fill-current" : ""}`}
          />
          추천 {post.likes}
        </Button>
      </div>
    </CardContent>
  );
};
