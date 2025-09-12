// Rolling paper utility functions

import { DecoType, decoTypeMap } from '@/types/rolling-paper'
import { getIconMapping } from './icon-mappings'

// 헬퍼 함수들 - icon mapping 추가
export const getDecoInfo = (decoType: DecoType | string) => {
  const baseInfo = decoTypeMap[decoType as keyof typeof decoTypeMap] || {
    name: "기본",
    color: "from-gray-100 to-slate-100"
  };
  
  const iconMapping = getIconMapping(decoType as DecoType);
  
  return {
    ...baseInfo,
    iconMapping
  };
}