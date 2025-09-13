import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";

export default function TermsPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 via-blue-50 to-indigo-50">
      <AuthHeader />

      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden">
          <div className="bg-gradient-to-r from-green-600 to-blue-600 px-8 py-6">
            <h1 className="text-3xl font-bold text-white text-center">
              이용약관
            </h1>
          </div>

          <div className="px-8 py-6">
            <div className="prose prose-lg max-w-none">
              <p className="text-gray-700 leading-relaxed mb-6">
                본 약관은 익명 메시지 서비스 &ldquo;비밀로그&rdquo;(이하
                &ldquo;서비스&rdquo;)의 이용과 관련하여 회사와 이용자 간의 권리,
                의무 및 책임사항 등을 규정함을 목적으로 합니다.
              </p>

              <hr className="border-gray-200 mb-6" />

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b border-gray-200 pb-2">
                  제1조 (목적)
                </h2>
                <p className="text-gray-700 leading-relaxed">
                  이 약관은 &ldquo;비밀로그&rdquo; 서비스를 이용함에 있어 회사와
                  이용자 간의 권리·의무 및 기타 필요한 사항을 규정함을 목적으로
                  합니다.
                </p>
              </section>

              <hr className="border-gray-200 mb-6" />

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b border-gray-200 pb-2">
                  제2조 (정의)
                </h2>
                <ol className="list-decimal pl-6 text-gray-700 space-y-3">
                  <li>
                    &quot;서비스&quot;란 사용자가 메시지를 남기고, 수신자가
                    로그인하여 메시지를 확인하는 웹 기반의 익명 메시지 전달
                    시스템을 의미합니다.
                  </li>
                  <li>
                    &quot;이용자&quot;란 본 서비스를 이용하는 모든 주체로,
                    메시지를 남기는 사람과 수신자를 포함합니다.
                  </li>
                  <li>
                    &quot;수신자&quot;란 카카오톡 로그인을 통해 메시지를
                    수신·확인할 수 있는 사용자를 의미합니다.
                  </li>
                </ol>
              </section>

              <hr className="border-gray-200 mb-6" />

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b border-gray-200 pb-2">
                  제3조 (이용 조건)
                </h2>
                <ol className="list-decimal pl-6 text-gray-700 space-y-3">
                  <li>
                    메시지를 남기는 이용자는 별도의 회원가입 또는 로그인 없이
                    서비스를 이용할 수 있습니다.
                  </li>
                  <li>
                    메시지를 수신하고 확인하려면 카카오톡 로그인이 필수이며,
                    로그인 시 회사는 카카오 ID, 닉네임, 프로필 이미지를
                    수집합니다.
                  </li>
                  <li>
                    수신자는 자신에게 도착한 메시지를 직접 확인하고, 원할 경우
                    삭제할 수 있습니다.
                  </li>
                </ol>
              </section>

              <hr className="border-gray-200 mb-6" />

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b border-gray-200 pb-2">
                  제4조 (메시지 저장 및 관리)
                </h2>
                <ol className="list-decimal pl-6 text-gray-700 space-y-3">
                  <li>
                    이용자가 남긴 메시지는 전송 시점에 서버에 암호화되어
                    저장됩니다.
                  </li>
                  <li>
                    메시지는 수신자가 직접 삭제하는 경우에만 삭제되며, 별도의
                    자동 삭제 기능은 제공하지 않습니다.
                  </li>
                </ol>
              </section>

              <hr className="border-gray-200 mb-6" />

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b border-gray-200 pb-2">
                  제5조 (서비스 이용 제한)
                </h2>
                <p className="text-gray-700 leading-relaxed mb-4">
                  회사는 아래에 해당하는 경우, 이용자의 서비스 이용을 제한할 수
                  있습니다.
                </p>
                <ul className="list-disc pl-6 text-gray-700 space-y-2">
                  <li>시스템의 정상적인 작동을 방해하거나 방해하려는 행위</li>
                  <li>서버나 데이터에 대한 비정상적인 접근 시도</li>
                </ul>
              </section>

              <hr className="border-gray-200 mb-6" />

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b border-gray-200 pb-2">
                  제6조 (지적재산권)
                </h2>
                <p className="text-gray-700 leading-relaxed">
                  서비스 내에 포함된 모든 자료에 대한 저작권 및 지적재산권은
                  회사에 귀속됩니다. 단, 이용자가 작성한 메시지의 내용은 해당
                  이용자에게 권리가 있습니다.
                </p>
              </section>

              <hr className="border-gray-200 mb-6" />

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b border-gray-200 pb-2">
                  제7조 (면책조항)
                </h2>
                <ol className="list-decimal pl-6 text-gray-700 space-y-3">
                  <li>
                    회사는 익명 메시지의 특성상, 메시지 내용에 대한 사실 여부,
                    적절성, 신뢰도 등에 대해 책임을 지지 않습니다.
                  </li>
                  <li>
                    수신자가 받은 메시지로 인해 발생하는 정신적 피해 등에 대해
                    회사는 법적 책임을 지지 않습니다.
                  </li>
                </ol>
              </section>

              <hr className="border-gray-200 mb-6" />

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b border-gray-200 pb-2">
                  제8조 (약관의 변경)
                </h2>
                <p className="text-gray-700 leading-relaxed">
                  본 약관은 필요 시 변경될 수 있으며, 변경 시 최소 7일 전에
                  공지합니다. 변경된 약관은 서비스에 게시함으로써 효력을
                  가집니다.
                </p>
              </section>

              <hr className="border-gray-200 mb-6" />

              <div className="text-right">
                <div className="bg-gray-50 rounded-lg p-4 border inline-block">
                  <p className="text-gray-700 font-medium">
                    시행일자: 2025년 6월 26일
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <HomeFooter />
    </div>
  );
}
