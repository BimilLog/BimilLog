import { Metadata, ResolvingMetadata } from "next";
import PublicRollingPaperClient from "./public-rolling-paper-client";

type Props = {
  params: { nickname: string };
};

export async function generateMetadata(
  { params }: Props,
  parent: ResolvingMetadata
): Promise<Metadata> {
  const nickname = decodeURIComponent(params.nickname);

  const previousImages = (await parent).openGraph?.images || [];

  return {
    title: `${nickname}님의 롤링페이퍼`,
    description: `${nickname}님에게 익명으로 따뜻한 메시지를 남겨보세요.`,
    openGraph: {
      title: `${nickname}님의 비밀로그 롤링페이퍼`,
      description: `친구 ${nickname}님에게 익명으로 메시지를 남겨보세요!`,
      url: `https://grow-farm.com/rolling-paper/${params.nickname}`,
      images: [...previousImages],
    },
  };
}

export default function PublicRollingPaperPage({
  params,
}: {
  params: { nickname: string };
}) {
  return <PublicRollingPaperClient nickname={params.nickname} />;
}
