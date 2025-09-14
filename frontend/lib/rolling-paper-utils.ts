// Rolling paper utility functions

import { DecoType, decoTypeMap } from '@/types/domains/paper'
import { getIconMapping } from './constants/icon-mappings'

// 헬퍼 함수들 - icon mapping 추가
export const getDecoInfo = (decoType: DecoType | string) => {
  // 타입 매핑 조합: decoTypeMap에서 기본 정보(이름, 색상) 가져오기
  // decoType이 유효하지 않으면 기본값으로 폴백
  const baseInfo = decoTypeMap[decoType as keyof typeof decoTypeMap] || {
    name: "기본",
    color: "from-gray-100 to-slate-100"
  };

  // 타입 캐스팅을 통한 아이콘 매핑 조회
  // string으로 들어온 decoType을 DecoType으로 캐스팅하여 아이콘 정보 추출
  const iconMapping = getIconMapping(decoType as DecoType);

  // 스프레드 연산자로 기본 정보와 아이콘 매핑 정보 결합
  return {
    ...baseInfo,
    iconMapping
  };
}