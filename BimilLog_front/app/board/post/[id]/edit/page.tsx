import { Metadata } from "next";
import { getPostDetailServer } from "@/lib/api/server";
import EditPostClient from "./EditPostClient";

export const metadata: Metadata = {
  title: "게시글 수정 - 비밀로그",
  description: "비밀로그 게시글을 수정합니다.",
};

interface EditPostPageProps {
  params: Promise<{ id: string }>;
}

export default async function EditPostPage({ params }: EditPostPageProps) {
  const { id } = await params;
  const postId = Number.parseInt(id);

  const response = await getPostDetailServer(postId);
  const initialPost = response?.success && response.data ? response.data : null;

  return <EditPostClient initialPost={initialPost} postId={postId} />;
}
