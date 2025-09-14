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
  const IconComponent = iconMapping.icon;

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
    />
  );
};


export default DecoIcon;