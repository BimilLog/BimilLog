import { WithClassName, WithChildren, WithStyle } from './common';

// Component Size Variants
export type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

// Component Variant Types
export type ButtonVariant = 'default' | 'primary' | 'secondary' | 'danger' | 'ghost' | 'link';
export type AlertVariant = 'default' | 'info' | 'success' | 'warning' | 'error';
export type BadgeVariant = 'default' | 'primary' | 'secondary' | 'outline';

// Form Types
export interface FormFieldProps extends WithClassName {
  label?: string;
  name: string;
  error?: string;
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
  helperText?: string;
}

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement>, WithClassName {
  error?: boolean;
  fullWidth?: boolean;
}

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement>, WithClassName {
  error?: boolean;
  fullWidth?: boolean;
  rows?: number;
}

export interface SelectOption<T = string> {
  value: T;
  label: string;
  disabled?: boolean;
}

// Modal Types
export interface ModalProps extends WithClassName, WithChildren {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
  closeOnOverlayClick?: boolean;
  closeOnEsc?: boolean;
}

// Card Types
export interface CardProps extends WithClassName, WithChildren, WithStyle {
  hoverable?: boolean;
  clickable?: boolean;
  onClick?: () => void;
  padding?: Size;
}

// List Types
export interface ListItemProps<T = any> {
  item: T;
  index: number;
  isSelected?: boolean;
  onSelect?: (item: T) => void;
  onEdit?: (item: T) => void;
  onDelete?: (item: T) => void;
}

// Table Types
export interface Column<T = any> {
  key: keyof T | string;
  title: string;
  width?: string | number;
  align?: 'left' | 'center' | 'right';
  sortable?: boolean;
  render?: (value: any, record: T, index: number) => React.ReactNode;
}

export interface TableProps<T = any> extends WithClassName {
  columns: Column<T>[];
  data: T[];
  loading?: boolean;
  emptyMessage?: string;
  rowKey?: keyof T | ((record: T) => string | number);
  onRowClick?: (record: T, index: number) => void;
}

// Navigation Types
export interface BreadcrumbItem {
  title: string;
  href?: string;
  onClick?: () => void;
}

export interface TabItem {
  key: string;
  label: string;
  content?: React.ReactNode;
  disabled?: boolean;
  icon?: React.ReactNode;
}

// Toast/Notification Types
export interface ToastProps {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  description?: string;
  duration?: number;
  onClose?: () => void;
}

// Empty State Types
export interface EmptyStateProps extends WithClassName {
  title?: string;
  description?: string;
  icon?: React.ReactNode;
  action?: {
    label: string;
    onClick: () => void;
  };
}

// Loading Types
export interface LoadingProps extends WithClassName {
  size?: Size;
  text?: string;
  fullScreen?: boolean;
  overlay?: boolean;
}

// Avatar Types
export interface AvatarProps extends WithClassName {
  src?: string;
  alt?: string;
  name?: string;
  size?: Size;
  shape?: 'circle' | 'square';
  fallback?: React.ReactNode;
}

// Badge Types
export interface BadgeProps extends WithClassName, WithChildren {
  variant?: BadgeVariant;
  size?: Size;
  dot?: boolean;
  count?: number;
  maxCount?: number;
}