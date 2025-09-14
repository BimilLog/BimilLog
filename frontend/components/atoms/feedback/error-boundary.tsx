'use client';

import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home, ChevronDown, ChevronUp, Bug } from 'lucide-react';
import { Button } from '../actions/button';
import { Card, CardContent } from '@/components';
import { logger } from '@/lib/utils/logger';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  showReportButton?: boolean;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
  showDetails: boolean;
  isReporting: boolean;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      showDetails: false,
      isReporting: false
    };
  }

  /**
   * React 18+ 에러 핸들링: 에러 발생 시 컴포넌트 상태를 업데이트
   * - 이 메서드는 렌더링 도중 하위 컴포넌트에서 에러가 발생했을 때 호출됨
   * - 에러 발생 시 hasError를 true로 설정하여 fallback UI를 렌더링하도록 함
   */
  static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error };
  }

  /**
   * 에러 캐치 및 로깅 처리 - React 클래스 컴포넌트의 라이프사이클 메서드
   *
   * 에러 처리 단계:
   * 1. 에러 정보를 컴포넌트 상태에 저장 (errorInfo)
   * 2. 개발자를 위한 콘솔 로깅 (개발 환경)
   * 3. 운영 환경을 위한 중요 에러 로깅 (사용자 환경 정보 포함)
   * 4. 부모 컴포넌트의 커스텀 에러 핸들러 호출 (선택적)
   */
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ errorInfo });

    // 로깅 - 개발 환경에서 디버깅용
    logger.error('ErrorBoundary caught an error:', error, errorInfo);

    // 중요한 에러는 운영환경에서도 로깅 - 사용자 환경 정보까지 수집
    // 추후 에러 모니터링 서비스(Sentry, LogRocket 등)와 연동 가능
    logger.critical('Application Error:', {
      message: error.message,
      stack: error.stack,
      componentStack: errorInfo.componentStack,  // React 컴포넌트 스택
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,            // 브라우저/OS 정보
      url: window.location.href                  // 에러 발생 페이지 URL
    });

    // 커스텀 에러 핸들러 호출 - 특별한 에러 처리가 필요한 경우
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }
  }

  /**
   * 에러 상태 초기화 및 페이지 새로고침
   * - 모든 에러 상태를 초기 상태로 되돌림
   * - 페이지 강제 새로고침으로 메모리 상태까지 완전 리셋
   */
  handleReset = () => {
    this.setState({
      hasError: false,
      error: undefined,
      errorInfo: undefined,
      showDetails: false,
      isReporting: false
    });
    window.location.reload();
  };

  /**
   * 홈페이지로 이동 (에러 상황에서 안전한 페이지로 탈출)
   * - 에러 상태 초기화 후 메인 페이지로 이동
   * - SPA 라우팅이 아닌 브라우저 네비게이션 사용으로 확실한 페이지 이동
   */
  handleGoHome = () => {
    this.setState({
      hasError: false,
      error: undefined,
      errorInfo: undefined,
      showDetails: false,
      isReporting: false
    });
    window.location.href = '/';
  };

  /**
   * 에러 세부 정보 표시/숨김 토글
   * - 개발 환경에서 스택 트레이스 등 디버깅 정보 확인용
   * - 일반 사용자에게는 기본적으로 숨겨져 있음
   */
  toggleDetails = () => {
    this.setState(prevState => ({ showDetails: !prevState.showDetails }));
  };

  /**
   * 에러 리포팅 시스템 - 개발자/운영팀에게 에러 정보 전달
   *
   * 에러 리포트 처리 과정:
   * 1. 중복 리포트 방지 (isReporting 상태 체크)
   * 2. 에러 정보 수집 (메시지, 스택, 환경 정보)
   * 3. 서버 전송 또는 클립보드 복사 (fallback)
   * 4. 사용자에게 완료 알림
   *
   * 추후 에러 모니터링 서비스 연동 시 주석 해제하여 사용
   */
  handleReport = async () => {
    // 에러가 없거나 이미 리포팅 중이면 중단
    if (!this.state.error || this.state.isReporting) return;

    this.setState({ isReporting: true });

    try {
      // 에러 리포트 데이터 구성 - 디버깅에 필요한 모든 정보 포함
      const errorReport = {
        message: this.state.error.message,
        stack: this.state.error.stack,
        componentStack: this.state.errorInfo?.componentStack, // React 컴포넌트 호출 스택
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,     // 브라우저/디바이스 정보
        url: window.location.href,          // 에러 발생 URL
        userId: null, // TODO: 인증된 사용자의 경우 ID 추가
      };

      logger.info('Error report generated:', errorReport);

      // 실제 구현시 서버로 전송 (에러 모니터링 API)
      // Sentry, LogRocket, Bugsnag 등과 연동 가능
      // await fetch('/api/error-report', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(errorReport)
      // });

      // 현재는 임시로 클립보드에 복사 (개발 단계)
      // 브라우저 호환성: 최신 브라우저에서 HTTPS에서만 동작
      await navigator.clipboard.writeText(JSON.stringify(errorReport, null, 2));
      alert('에러 정보가 클립보드에 복사되었습니다.');

    } catch (err) {
      // 클립보드 접근 실패 또는 네트워크 오류 처리
      logger.error('Error reporting failed:', err);
      alert('에러 리포트 생성에 실패했습니다.');
    } finally {
      // 성공/실패와 관계없이 로딩 상태 해제
      this.setState({ isReporting: false });
    }
  };

  render() {
    // 에러 상태일 때 처리
    if (this.state.hasError) {
      // 커스텀 fallback UI가 제공되면 해당 UI를 렌더링
      if (this.props.fallback) {
        return <>{this.props.fallback}</>;
      }

      return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 p-4">
          <div className="max-w-lg w-full">
            <Card variant="elevated" className="hover:shadow-brand-xl transition-shadow">
              <CardContent className="p-6 text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 mb-4">
                <AlertTriangle className="w-8 h-8 text-red-600" />
              </div>

              <h2 className="text-2xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent mb-2">
                오류가 발생했습니다
              </h2>

              <p className="text-brand-muted mb-6 leading-relaxed">
                예상치 못한 오류가 발생했습니다.<br />
                페이지를 새로고침하거나 홈으로 이동해서 다시 시도해주세요.
              </p>

              {/* 에러 상세 정보 토글 - 개발 환경 또는 에러 발생 시에만 표시 */}
              {(process.env.NODE_ENV === 'development' || this.state.error) && (
                <div className="mb-6">
                  <Button
                    onClick={this.toggleDetails}
                    variant="ghost"
                    size="sm"
                    className="mb-3 text-brand-secondary hover:text-brand-primary"
                  >
                    {this.state.showDetails ? (
                      <>
                        <ChevronUp className="w-4 h-4 mr-1" />
                        세부 정보 숨기기
                      </>
                    ) : (
                      <>
                        <ChevronDown className="w-4 h-4 mr-1" />
                        세부 정보 보기
                      </>
                    )}
                  </Button>

                  {this.state.showDetails && this.state.error && (
                    <div className="p-4 bg-gray-100 rounded-lg text-left border-2 border-gray-200">
                      <h4 className="font-semibold text-brand-primary mb-2">오류 메시지:</h4>
                      <p className="text-sm font-mono text-red-600 mb-3 break-all">
                        {this.state.error.message}
                      </p>

                      {/* 스택 트레이스는 개발 환경에서만 표시 - 보안상 프로덕션에서 숨김 */}
                      {process.env.NODE_ENV === 'development' && this.state.error.stack && (
                        <>
                          <h4 className="font-semibold text-brand-primary mb-2">스택 트레이스:</h4>
                          <pre className="text-xs font-mono text-brand-muted whitespace-pre-wrap break-all overflow-x-auto max-h-32 overflow-y-auto">
                            {this.state.error.stack}
                          </pre>
                        </>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* 액션 버튼들 */}
              <div className="space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <Button
                    onClick={this.handleReset}
                    variant="default"
                    size="default"
                    className="w-full"
                  >
                    <RefreshCw className="w-4 h-4 mr-2" />
                    새로고침
                  </Button>

                  <Button
                    onClick={this.handleGoHome}
                    variant="outline"
                    size="default"
                    className="w-full"
                  >
                    <Home className="w-4 h-4 mr-2" />
                    홈으로 이동
                  </Button>
                </div>

                  {/* 에러 리포트 버튼 - 개발자에게 에러 정보 전달 */}
                {this.props.showReportButton !== false && (
                  <Button
                    onClick={this.handleReport}
                    variant="ghost"
                    size="sm"
                    disabled={this.state.isReporting}
                    className="w-full text-brand-secondary hover:text-brand-primary"
                  >
                    <Bug className="w-4 h-4 mr-2" />
                    {this.state.isReporting ? '리포트 생성 중...' : '에러 리포트 복사'}
                  </Button>
                )}
              </div>

              {/* 추가 도움말 - 사용자가 직접 해결할 수 있는 방법 안내 */}
              <div className="mt-6 pt-4 border-t border-gray-200">
                <p className="text-xs text-brand-secondary leading-relaxed">
                  문제가 계속 발생하면 브라우저 캐시를 삭제하거나<br />
                  다른 브라우저를 사용해보세요.
                </p>
              </div>
              </CardContent>
            </Card>
          </div>
        </div>
      );
    }

    // 에러가 없는 경우 정상적으로 자식 컴포넌트들을 렌더링
    return this.props.children;
  }
}