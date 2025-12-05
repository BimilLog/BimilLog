"use client";

import { CleanLayout } from "@/components/organisms/layout/BaseLayout";
import { LegalDocumentHeader } from "@/components/organisms/common/LegalDocumentHeader";

export default function PrivacyPage() {
  return (
    <CleanLayout className="bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]">
      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="bg-card rounded-2xl shadow-brand-xl border border-border overflow-hidden">
          <LegalDocumentHeader title="개인정보 처리방침" />

          <div className="px-8 py-6 text-foreground">
            <div className="prose prose-lg max-w-none dark:prose-invert">
              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [1. 총 칙]
                </h2>
                <p className="text-brand-primary leading-relaxed">
                  &apos;비밀로그&apos;(이하 &ldquo;회사&rdquo;라 합니다)는
                  이용자가 회사의 서비스를 이용함과 동시에 온라인상에서 회사에
                  제공한 개인정보가 보호 받을 수 있도록 최선을 다하고 있습니다.
                  이에 회사는 정보통신망 이용촉진 및 정보보호 등에 관한 법률 등
                  정보통신서비스 제공자가 준수하여야 할 관련 법규상의
                  개인정보보호 규정 및 지침을 준수하고 있습니다. 회사는 개인정보
                  처리방침을 통하여 이용자들이 제공하는 개인정보가 어떠한 용도와
                  방식으로 이용되고 있으며 개인정보 보호를 위해 어떠한 조치가
                  취해지고 있는지 알려 드립니다. 회사는 개인정보 처리방침을
                  홈페이지 첫 화면에 공개함으로써 이용자들이 언제나 용이하게
                  보실수 있도록 조치하고 있습니다. 회사의 개인정보 처리방침은
                  정부의 법률 및 지침 변경이나 회사의 내부 방침 변경 등으로
                  인하여 수시로 변경될 수 있으므로 이용자들께서는 사이트
                  첫페이지에 고지되는 개인정보처리방침을 사이트 방문 시 수시로
                  확인하시기 바랍니다. 회사의 개인정보 처리방침은 다음과 같은
                  내용을 담고 있습니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [2. 개인정보 수집에 대한 동의]
                </h2>
                <p className="text-brand-primary leading-relaxed">
                  회사는 이용자들이 회사의 개인정보 처리방침 또는 이용약관의
                  내용에 대해 &quot;동의&quot;버튼 또는 &quot;취소&quot;버튼을
                  클릭할 수 있는 절차를 마련하여, &quot;동의&quot;버튼을
                  클릭하면 개인정보 수집에 대해 동의한 것으로 봅니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [3. 개인정보의 수집 및 이용목적]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  &quot;개인정보&quot;라 함은 생존하는 개인에 관한 정보로서 당해
                  정보에 포함되어 있는 카카오톡 닉네임, 카카오톡 프로필사진 등의
                  사항에 의하여 당해 개인을 식별할 수 있는 정보(당해
                  정보만으로는 특정 개인을 식별할 수 없더라도 다른 정보와
                  용이하게 결합하여 식별할 수 있는 것을 포함)를 말합니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  개인회원에 대하여 수집하는 개인정보 항목과 수집 및 이용목적은
                  다음과 같습니다.
                </p>
                <ul className="list-disc pl-6 text-brand-primary space-y-1">
                  <li>회원가입 및 관리 : 카카오톡 로그인 API</li>
                  <li>재화 또는 서비스 제공 : 서비스 이용 기록, 쿠키</li>
                  <li>선택서비스 : 비밀로그에 가입한 카카오톡 친구목록</li>
                </ul>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [4. 수집하는 개인정보의 수집방법]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 회사는 이용자들이 회원서비스를 이용하기 위해 회원으로
                  가입하실 때 서비스 제공을 위한 필수적인 정보들을 온라인상에서
                  입력 받고 있습니다. 회원 가입 시에 받는 필수적인 정보는
                  카카오ID(식별자), 카카오톡 닉네임, 카카오톡 프로필사진
                  등입니다. 또한 양질의 서비스 제공을 위하여 이용자들이
                  선택적으로 입력할 수 있는 사항으로서 전화번호 등을 입력 받고
                  있습니다. 또한 쇼핑몰 내에서의 설문조사나 이벤트 행사 시
                  통계분석이나 경품제공 등을 위해 선별적으로 개인정보 입력을
                  요청할 수 있습니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ② 이용자의 기본적 인권 침해의 우려가 있는 민감한 개인정보(인종
                  및 민족, 사상 및 신조, 출신지 및 본적지, 정치적 성향 및
                  범죄기록, 건강상태 및 성생활 등)는 수집하지 않으며 부득이하게
                  수집해야 할 경우 이용자들의 사전동의를 반드시 구할 것입니다.
                  그리고, 어떤 경우에라도 입력하신 정보를 이용자들에게 사전에
                  밝힌 목적 이외에 다른 목적으로는 사용하지 않으며 외부로
                  유출하지 않습니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [5. 개인정보의 보유 및 이용기간]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 회사가 이용자의 개인정보를 수집하는 경우 그 보유기간은
                  회원가입 하신후 해지(탈퇴신청, 직권탈퇴 포함)시까지 입니다.
                  또한 해지 시 회사는 이용자의 개인정보를 파기하며 개인정보가
                  제3자에게 제공된 경우에는 제3자에게도 파기하도록 지시합니다.
                  다만 상법 등 법령의 규정에 의하여 보존할 필요성이 있는
                  경우에는 법령에서 규정한 보존기간 동안 거래내역과 최소한의
                  기본정보를 보유하고 있고 있으며 보유기간을 이용자에게 미리
                  고지하고 그 보유기간이 경과하지 아니한 경우와 개별적으로
                  이용자의 동의를 받을 경우에는 약속한 보유기간 동안 개인정보를
                  보유합니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ② 회사는 회원 가입 및 탈퇴를 반복하여 서비스를 부정 이용하는
                  경우를 방지하기 위하여 탈퇴한 이용자의 &quot;최소한의
                  개인정보&quot;를 암호화하여 복호화 불가능하도록 처리하여
                  6개월간 보관합니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ③ 이용자의 동의를 받아 보유하고 있는 거래정보 등을 이용자가
                  열람을 요구하는 경우 회사는 지체 없이 그 열람, 확인할 수
                  있도록 조치합니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [6. 개인정보 파기절차 및 방법]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 파기절차 - 이용자가 회원가입 등을 위해 입력한 정보는 목적이
                  달성된 후 내부 방침 및 기타 관련 법령에 의한 정보보호 사유에
                  따라(보유 및 이용기간 참조)일정 기간 저장된 후 파기됩니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ② 파기방법 - 종이에 출력된 개인정보는 분쇄기로 분쇄하거나
                  소각을 통하여 파기합니다. - 전자적 파일 형태로 저장된
                  개인정보는 기록을 재생할 수 없는 기술적 방법을 사용하여
                  삭제합니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ③ 회사는 서비스를 장기간(1년 또는 다른법령에서 별도로 정한
                  기간, 회원이 달리 정한 기간) 동안 이용하지 아니하는 회원의
                  개인정보를 보호하기 위하여, 해당 기간 경과 후 다른 회원의
                  개인정보와 분리하여 별도·저장 관리하고 해당 회원의 서비스
                  이용을 제한 할 수 있습니다. 단, 회원이 서비스 이용의사를
                  표시한 경우 즉시 서비스 이용이 가능합니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [7. 수집한 개인정보의 공유 및 제공]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 회사는 이용자들의 개인정보를 &quot;수집하는 개인정보 항목 및
                  수집∙이용목적&quot;에서 고지한 범위 내에서 사용하며, 이용자의
                  사전 동의 없이는 동 범위를 초과하여 이용하거나 제3자에게
                  제공하지 않습니다. 다만, 아래의 경우에는 예외로 합니다.
                </p>
                <ul className="list-disc pl-6 text-brand-primary space-y-2 mb-4">
                  <li>
                    금융실명거래및비밀보장에관한법률,
                    신용정보의이용및보호에관한법률,
                    전기통신기본법,전기통신사업법, 지방세법, 소비자 기본법,
                    한국은행법, 형사송법등 법령에 특별한 규정이 있는 경우
                  </li>
                  <li>
                    기타 법에 의해 요구된다고 선의로 판단되는 경우 (ex. 관련법에
                    의거 적법한 절차에 의한 정부/수사기관의 요청이 있는 경우 등)
                  </li>
                  <li>이용자들이 사전에 공개에 동의한 경우</li>
                  <li>
                    홈페이지에 게시한 서비스 이용 약관 및 기타 회원 서비스 등의
                    이용약관 또는 운영원칙을 위반한 경우
                  </li>
                  <li>
                    자사 서비스를 이용하여 타인에게 정신적, 물질적 피해를
                    줌으로써 그에 대한 법적인 조치를 취하기 위하여 개인정보를
                    공개해야 한다고 판단되는 충분한 근거가 있는 경우
                  </li>
                  <li>서비스제공에 따른 요금정산을 위하여 필요한 경우</li>
                  <li>
                    고객센터를 운영하는 전문업체에 해당 민원사항의 처리에 필요한
                    개인 정보를 제공하는 경우
                  </li>
                  <li>
                    통계작성,학술연구 또는 시장조사를 위하여 필요한 경우로서
                    특정개인을 식별할 수 없는 형태로 제공하는 경우
                  </li>
                </ul>
                <p className="text-brand-primary leading-relaxed">
                  ② 회사는 이용자에게 보다 더 나은 서비스를 제공하기 위하여
                  개인정보를 제휴사 내지 협력업체에게 제공하거나 공유할 수
                  있습니다. 개인정보를 제공하거나 공유할 경우에는 사전에
                  이용자에게 제휴사등이 누구인지, 제공 또는 공유되는
                  개인정보항목이 무엇인지, 왜 그러한 개인정보가 제공되거나
                  공유되어야 하는지, 언제까지 어떻게 보호, 관리되는지에 대해
                  개별적으로 고지하여 동의를 구하는 절차를 거치게 되며, 이용자가
                  동의하지 않은 경우에는 제휴사 등에게 제공하거나 공유하지
                  않습니다. 또한 이용자가 일단 개인정보의 제공에 동의하더라도
                  언제든지 그 동의를 철회할 수 있습니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [8. 이용자 자신의 개인정보 관리(열람,정정,삭제 등)에 관한
                  사항]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 이용자는 언제든지 비밀로그 홈페이지(https://grow-farm.com)에
                  로그인하셔서 회원정보변경에서 이용자의 개인정보를 열람하시거나
                  정정하실 수 있으며 회사 홈페이지(https://grow-farm.com)의
                  개인정보보호책임자에게 전자우편 또는 서면으로 요청하신 경우
                  정정하여 드리겠습니다. 단, 카카오톡ID(식별자)는 정정이
                  불가능합니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ② 이용자는 개인정보의 수집, 이용에 대한 동의 철회(해지) 및
                  제3자에게 제공한 동의의 철회는 카카오를 통해 연결 끊기를
                  요청하여 가능합니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [9. 개인정보 자동 수집 장치의 설치∙운영 및 그 거부에 관한
                  사항]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 회사는 서비스의 유연한 작동을 위해서 &apos;쿠키(Cookie,
                  접속정보파일)&apos;를 운용합니다. 회사는 쿠키 운용과 관련하여
                  이용자의 컴퓨터는 식별하지만 이용자를 개인적으로 식별하지는
                  않습니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ② 이용자는 &apos;쿠키&apos;에 대한 선택권을 가지고 있습니다.
                  이용자는 웹브라우저에서 [도구]-[인터넷 옵션]-[보안]-[사용자
                  정의 수준]을 선택함으로써 모든 쿠키를 허용하거나, 쿠키가
                  저장될 때 마다 확인을 거치거나, 아니면 모든 쿠키의 저장을
                  거부할 수 있습니다. 단, 모든 쿠키의 저장을 거부하는 경우,
                  쿠키를 통해 회사에서 제공하는 서비스를 이용할 수 없습니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [10. 개인정보관련 의견수렴 및 불만처리]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 회사는 귀하의 의견을 소중하게 생각하며, 귀하는
                  의문사항으로부터 언제나 성실한 답변을 받을 권리가 있으며,
                  귀하와의 원활환 의사소통을 위해 고객센터를 운영하고 있습니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ② 개인정보와 관련하여 의견 또는 불만이 있는 경우,
                  개인정보보호책임자에게 해당 내용을 알려주시면 회사는 최선을
                  다하여 이용자들의 의견을 수렴하고 또한 불만에 대해서는 충분한
                  답변을 해 드리고 문제를 해결할 수 있도록 노력 하겠습니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [11. 아동의 개인정보 보호]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 회사는 만14세 미만 아동(이하 &apos;아동&apos;이라 합니다)의
                  개인정보를 수집하는 경우 법정대리인의 동의를 받습니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ② 전항의 동의를 받기 위하여 동의서, 회사는 법정대리인의 성명,
                  연락처, 증빙자료 등 필요한 최소한의 정보를 요구할 수 있으며
                  이와 같이 수집된 법정대리인의 개인정보는 해당 법정대리인의
                  동의 여부를 확인하는 목적 외의 용도로 이용되거나 제3자에게
                  제공하지 아니합니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ③ 법정 대리인의 동의서는 미성년과 회사의 계약, 청약철회,
                  대금결제, 재화 공급 등이 발생할시 소비자의 불만 및 분쟁해결
                  등을 위한 용도로 이용됩니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ④ 탈퇴한 아동의 법정대리인 동의서 또는 동의 철회, 유효기간이
                  만료된 법정대리인 동의서는 계약, 청약철회, 대금결제, 재화 공급
                  등이 있는 경우 5년동안 보관됩니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ⑤ 아동의 법정대리인은 당해 아동 개인정보의 열람, 정정요구,
                  또는 개인정보의 수집 이용 또는 제공에 대한 동의의 철회를
                  요청할 수 있으며, 이러한 요청이 있을 경우 회사는 필요한 조치를
                  취하겠습니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [12. 이용자의 권리와 의무]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 이용자의 개인정보를 최신의 상태로 정확하게 입력하여 불의의
                  사고를 예방해주시기 바랍니다. 이용자가 입력한 부정확한 정보로
                  인해 발생하는 사고의 책임은 이용자 자신에게 있으며 타인 정보의
                  도용 등 허위정보를 입력할 경우 계정의 이용이 제한될 수
                  있습니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ② 이용자는 개인정보를 보호받을 권리를 보유하나 이와 동시에
                  본인의 정보를 스스로 보호하고 또한 타인의 정보를 침해하지 않을
                  의무도 가지고 있습니다. 비밀번호를 포함한 이용자의 개인정보가
                  유출되지 않도록 조심하시고 게시물을 포함한 타인의 개인정보를
                  훼손하지 않도록 유의해 주십시오. 만약 이와 같은 책임을 다하지
                  못하고 타인의 정보 및 존엄성을 훼손할 시에는 『정보통신망
                  이용촉진 및 정보보호 등에 관한 법률』등에 의해 처벌받을 수
                  있습니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [13. 의견수렴 및 불만처리]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 회사는 귀하의 의견을 소중하게 생각하며, 귀하는
                  의문사항으로부터 언제나 성실한 답변을 받을 권리가 있으며,
                  귀하와의 원활환 의사소통을 위해 고객센터를 운영하고 있습니다.
                </p>
                <p className="text-brand-primary leading-relaxed">
                  ② 개인정보와 관련하여 의견 또는 불만이 있는 경우, 하기
                  개인정보보호책임자 및 담당자에게 해당 내용을 알려주시면 회사는
                  최선을 다하여 이용자들의 의견을 수렴하고 또한 불만에 대해서는
                  충분한 답변을 해 드리고 문제를 해결할 수 있도록 노력
                  하겠습니다.
                </p>
              </section>

              <section className="mb-8">
                <h2 className="text-xl font-semibold text-foreground mb-4 border-b border-border pb-2">
                  [14. 개인정보보호 책임자 및 담당자]
                </h2>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ① 회사는 귀하가 좋은 정보를 안전하게 이용할 수 있도록 최선을
                  다하고 있습니다. 개인정보를 보호하는데 있어 귀하께 고지한
                  사항들에 반하는 사고가 발생할 경우 개인정보보호책임자가 책임을
                  집니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ② 이용자 개인정보와 관련한 아이디(ID)의 비밀번호에 대한
                  보안유지책임은 해당 이용자 자신에게 있습니다. 회사는
                  비밀번호에 대해 어떠한 방법으로도 이용자에게 직접적으로
                  질문하는 경우는 없으므로 타인에게 비밀번호가 유출되지 않도록
                  각별히 주의하시기 바랍니다. 특히 공공장소에서 온라인상에서
                  접속해 있을 경우에는 더욱 유의하셔야 합니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ③ 회사가 기술적인 보완조치를 했음에도 불구하고, 해킹 등
                  기본적인 네트워크 상의 위험성에 의해 발생하는 예기치 못한
                  사고로 인한 정보의 훼손 및 방문자가 작성한 게시물에 의한 각종
                  분쟁에 관해서는 책임이 없습니다.
                </p>
                <p className="text-brand-primary leading-relaxed mb-4">
                  ④ 회사는 개인정보에 대한 의견수렴 및 불만처리를 담당하는
                  개인정보 보호책임자 및 담당자를 지정하고 있고, 연락처는 아래와
                  같습니다.
                </p>
                <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 border border-gray-200 dark:border-gray-700">
                  <h4 className="font-semibold text-brand-primary mb-2">
                    [개인정보 보호 책임자 및 담당자]
                  </h4>
                  <ul className="text-brand-primary space-y-1">
                    <li>담당자 : 비밀로그개발자</li>
                    <li>e-mail : wodlr1207@naver.com</li>
                  </ul>
                </div>
              </section>
            </div>
          </div>
        </div>
      </main>
    </CleanLayout>
  );
}
