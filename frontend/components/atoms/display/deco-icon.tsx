import React from 'react';
import { cn } from '@/lib/utils';
import { getIconMapping } from '@/lib/constants/icon-mappings';
import { DecoType } from '@/lib/api';

interface DecoIconProps {
  decoType: DecoType;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
  showBackground?: boolean;
  animate?: 'bounce' | 'pulse' | 'none';
}

const sizeClasses = {
  sm: 'w-4 h-4',
  md: 'w-6 h-6',
  lg: 'w-8 h-8',
  xl: 'w-12 h-12'
};

const emojiSizeClasses = {
  sm: 'text-lg',
  md: 'text-2xl',
  lg: 'text-3xl',
  xl: 'text-5xl'
};

const backgroundSizeClasses = {
  sm: 'w-8 h-8',
  md: 'w-10 h-10',
  lg: 'w-12 h-12',
  xl: 'w-16 h-16'
};

const animationClasses = {
  bounce: 'animate-bounce',
  pulse: 'animate-pulse',
  none: ''
};

export const DecoIcon: React.FC<DecoIconProps> = ({
  decoType,
  size = 'lg',
  className = '',
  showBackground = true,
  animate = 'none'
}) => {
  const iconMapping = getIconMapping(decoType);
  const isEmoji = iconMapping.isEmoji;

  // 이모지 렌더링
  if (isEmoji && iconMapping.emoji) {
    if (showBackground) {
      return (
        <div
          className={cn(
            'rounded-full flex items-center justify-center relative overflow-hidden',
            'bg-gradient-to-br shadow-lg hover:shadow-xl transition-all duration-300',
            'hover:scale-105 active:scale-95',
            backgroundSizeClasses[size],
            iconMapping.bgColor || 'bg-gray-100',
            animationClasses[animate],
            className
          )}
          style={{
            background: `linear-gradient(135deg, rgba(255,255,255,0.9) 0%, rgba(255,255,255,0.7) 100%)`,
            boxShadow: '0 4px 6px rgba(0,0,0,0.1), inset 0 1px 0 rgba(255,255,255,0.5)',
          }}
        >
          <span
            className={cn(
              'select-none font-emoji relative z-10',
              emojiSizeClasses[size]
            )}
            style={{
              filter: 'drop-shadow(0 2px 2px rgba(0,0,0,0.2))',
              WebkitTextStroke: '0.5px rgba(0,0,0,0.1)',
            }}
          >
            {iconMapping.emoji}
          </span>
          {/* 반짝이 효과 */}
          <div className="absolute inset-0 bg-gradient-to-tr from-transparent via-white/20 to-transparent opacity-0 hover:opacity-100 transition-opacity duration-300" />
        </div>
      );
    }

    return (
      <span
        className={cn(
          'select-none font-emoji inline-flex items-center justify-center',
          emojiSizeClasses[size],
          animationClasses[animate],
          className
        )}
        style={{
          filter: 'drop-shadow(0 2px 3px rgba(0,0,0,0.3))',
          WebkitTextStroke: '0.5px rgba(0,0,0,0.1)',
        }}
      >
        {iconMapping.emoji}
      </span>
    );
  }

  // 기존 루시드 아이콘 렌더링
  const IconComponent = iconMapping.icon;
  if (!IconComponent) return null;

  if (showBackground) {
    return (
      <div
        className={cn(
          'rounded-full flex items-center justify-center',
          backgroundSizeClasses[size],
          iconMapping.bgColor || 'bg-gray-100',
          animationClasses[animate],
          className
        )}
      >
        <IconComponent
          className={cn(
            sizeClasses[size],
            iconMapping.color || 'text-brand-muted'
          )}
          fill="currentColor"
          strokeWidth={1.5}
        />
      </div>
    );
  }

  return (
    <IconComponent
      className={cn(
        sizeClasses[size],
        iconMapping.color || 'text-brand-muted',
        animationClasses[animate],
        className
      )}
      fill="currentColor"
      strokeWidth={1.5}
    />
  );
};


export default DecoIcon;