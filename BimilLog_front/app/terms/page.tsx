import type { Metadata } from "next";
import { CleanLayout } from "@/components/organisms/layout/BaseLayout";
import {
  PolicyDocument,
  type PolicySection,
} from "@/components/organisms/common/PolicyDocument";

// 라운드 14: F-14-BUG-15 — page-level metadata
export const metadata: Metadata = {
  title: "이용약관",
  description:
    "비밀로그 서비스 이용 시 지켜야 할 규칙과 회사·이용자의 권리·의무를 안내합니다.",
  robots: { index: true, follow: true },
  alternates: { canonical: "/terms" },
  openGraph: {
    title: "이용약관 | 비밀로그",
    description:
      "비밀로그 서비스 이용 시 지켜야 할 규칙과 회사·이용자의 권리·의무를 안내합니다.",
    url: "https://grow-farm.com/terms",
  },
};

const sections: PolicySection[] = [
  {
    id: "article-1",
    heading: "제1조 (목적)",
    content: (
      <p>
        이 약관은 &ldquo;비밀로그&rdquo; 서비스를 이용함에 있어 회사와 이용자
        간의 권리·의무 및 기타 필요한 사항을 규정함을 목적으로 합니다.
      </p>
    ),
  },
  {
    id: "article-2",
    heading: "제2조 (정의)",
    content: (
      <ol className="list-decimal pl-6 space-y-3">
        <li className="break-keep">
          &quot;서비스&quot;란 사용자가 메시지를 남기고, 수신자가 로그인하여
          메시지를 확인하는 웹 기반의 익명 메시지 전달 시스템을 의미합니다.
        </li>
        <li className="break-keep">
          &quot;이용자&quot;란 본 서비스를 이용하는 모든 주체로, 메시지를 남기는
          사람과 수신자를 포함합니다.
        </li>
        <li className="break-keep">
          &quot;수신자&quot;란 카카오톡 로그인을 통해 메시지를 수신·확인할 수
          있는 사용자를 의미합니다.
        </li>
      </ol>
    ),
  },
  {
    id: "article-3",
    heading: "제3조 (이용 조건)",
    content: (
      <ol className="list-decimal pl-6 space-y-3">
        <li className="break-keep">
          메시지를 남기는 이용자는 별도의 회원가입 또는 로그인 없이 서비스를
          이용할 수 있습니다.
        </li>
        <li className="break-keep">
          메시지를 수신하고 확인하려면 카카오톡 로그인이 필수이며, 로그인 시
          회사는 카카오 ID, 닉네임, 프로필 이미지를 수집합니다.
        </li>
        <li className="break-keep">
          수신자는 자신에게 도착한 메시지를 직접 확인하고, 원할 경우 삭제할 수
          있습니다.
        </li>
      </ol>
    ),
  },
  {
    id: "article-4",
    heading: "제4조 (메시지 저장 및 관리)",
    content: (
      <ol className="list-decimal pl-6 space-y-3">
        <li className="break-keep">
          이용자가 남긴 메시지는 전송 시점에 서버에 암호화되어 저장됩니다.
        </li>
        <li className="break-keep">
          메시지는 수신자가 직접 삭제하는 경우에만 삭제되며, 별도의 자동 삭제
          기능은 제공하지 않습니다.
        </li>
      </ol>
    ),
  },
  {
    id: "article-5",
    heading: "제5조 (서비스 이용 제한)",
    content: (
      <>
        <p>
          회사는 아래에 해당하는 경우, 이용자의 서비스 이용을 제한할 수 있습니다.
        </p>
        <ul className="list-disc pl-6 space-y-2">
          <li className="break-keep">
            시스템의 정상적인 작동을 방해하거나 방해하려는 행위
          </li>
          <li className="break-keep">서버나 데이터에 대한 비정상적인 접근 시도</li>
        </ul>
      </>
    ),
  },
  {
    id: "article-6",
    heading: "제6조 (지적재산권)",
    content: (
      <p>
        서비스 내에 포함된 모든 자료에 대한 저작권 및 지적재산권은 회사에
        귀속됩니다. 단, 이용자가 작성한 메시지의 내용은 해당 이용자에게 권리가
        있습니다.
      </p>
    ),
  },
  {
    id: "article-7",
    heading: "제7조 (면책조항)",
    content: (
      <ol className="list-decimal pl-6 space-y-3">
        <li className="break-keep">
          회사는 익명 메시지의 특성상, 메시지 내용에 대한 사실 여부, 적절성,
          신뢰도 등에 대해 책임을 지지 않습니다.
        </li>
        <li className="break-keep">
          수신자가 받은 메시지로 인해 발생하는 정신적 피해 등에 대해 회사는 법적
          책임을 지지 않습니다.
        </li>
      </ol>
    ),
  },
  {
    id: "article-8",
    heading: "제8조 (약관의 변경)",
    content: (
      <p>
        본 약관은 필요 시 변경될 수 있으며, 변경 시 최소 7일 전에 공지합니다.
        변경된 약관은 서비스에 게시함으로써 효력을 가집니다.
      </p>
    ),
  },
];

export default function TermsPage() {
  return (
    <CleanLayout className="bg-paper">
      <PolicyDocument
        title="이용약관"
        intro={
          <>
            본 약관은 <strong>비밀로그 사용 시 지켜야 할 규칙</strong>을 정리한
            문서예요. 핵심만 빠르게 보고 싶다면 좌측 목차에서 원하는 조항을
            바로 열어볼 수 있어요.
          </>
        }
        sections={sections}
        effectiveDate="2025년 6월 26일"
        revisions={[
          {
            date: "2025-06-26",
            summary: "최초 시행",
          },
        ]}
      />
    </CleanLayout>
  );
}
