import { getPostDetailServer } from "@/lib/api/server";
import EditPostClient from "./EditPostClient";

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
