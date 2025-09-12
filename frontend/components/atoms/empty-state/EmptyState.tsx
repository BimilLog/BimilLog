import React from "react";
import { FileText, MessageCircle, Heart, ThumbsUp } from "lucide-react";

interface EmptyStateProps {
  type: "posts" | "comments" | "liked-posts" | "liked-comments";
  className?: string;
}

const emptyStates = {
  posts: {
    icon: <FileText className="w-12 h-12 text-gray-400" />,
    title: "작성한 글이 없습니다",
    description: "첫 번째 글을 작성해보세요!",
  },
  comments: {
    icon: <MessageCircle className="w-12 h-12 text-gray-400" />,
    title: "작성한 댓글이 없습니다",
    description: "다른 사람의 글에 댓글을 달아보세요!",
  },
  "liked-posts": {
    icon: <Heart className="w-12 h-12 text-gray-400" />,
    title: "추천한 글이 없습니다",
    description: "마음에 드는 글에 추천을 눌러보세요!",
  },
  "liked-comments": {
    icon: <ThumbsUp className="w-12 h-12 text-gray-400" />,
    title: "추천한 댓글이 없습니다",
    description: "좋은 댓글에 추천을 눌러보세요!",
  },
};

export const EmptyState: React.FC<EmptyStateProps> = ({ type, className }) => {
  const state = emptyStates[type];

  return (
    <div className={`flex flex-col items-center justify-center py-16 text-center ${className || ""}`}>
      <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-4">
        {state.icon}
      </div>
      <h3 className="text-lg font-semibold text-gray-800 mb-2">
        {state.title}
      </h3>
      <p className="text-gray-600 max-w-sm">{state.description}</p>
    </div>
  );
};