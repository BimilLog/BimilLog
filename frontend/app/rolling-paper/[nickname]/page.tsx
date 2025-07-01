import { Metadata, ResolvingMetadata } from "next";
import PublicRollingPaperClient from "./public-rolling-paper-client";

type Props = {
  params: Promise<{ nickname: string }>;
};

export async function generateMetadata(
  { params }: Props,
  parent: ResolvingMetadata
): Promise<Metadata> {
  const { nickname } = await params;
  const decodedNickname = decodeURIComponent(nickname);

  const previousImages = (await parent).openGraph?.images || [];

  return {
    title: `${decodedNickname}님의 롤링페이퍼`,
    description: `${decodedNickname}님에게 익명으로 따뜻한 메시지를 남겨보세요.`,
    openGraph: {
      title: `${decodedNickname}님의 비밀로그 롤링페이퍼`,
      description: `친구 ${decodedNickname}님에게 익명으로 메시지를 남겨보세요!`,
      url: `https://grow-farm.com/rolling-paper/${nickname}`,
      images: [...previousImages],
    },
  };
}

export default async function PublicRollingPaperPage({
  params,
}: {
  params: Promise<{ nickname: string }>;
}) {
  const { nickname } = await params;
  return <PublicRollingPaperClient nickname={nickname} />;
}
