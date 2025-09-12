// Utility Types

// Make all properties optional recursively
export type DeepPartial<T> = T extends object
  ? {
      [P in keyof T]?: DeepPartial<T[P]>;
    }
  : T;

// Make all properties required recursively
export type DeepRequired<T> = T extends object
  ? {
      [P in keyof T]-?: DeepRequired<T[P]>;
    }
  : T;

// Make all properties readonly recursively
export type DeepReadonly<T> = T extends object
  ? {
      readonly [P in keyof T]: DeepReadonly<T[P]>;
    }
  : T;

// Pick properties that are not undefined
export type NonNullableKeys<T> = {
  [K in keyof T]: T[K] extends null | undefined ? never : K;
}[keyof T];

// Pick only non-nullable properties
export type NonNullableProperties<T> = Pick<T, NonNullableKeys<T>>;

// Exclude properties from type
export type Except<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

// Make specific properties optional
export type PartialBy<T, K extends keyof T> = Except<T, K> & Partial<Pick<T, K>>;

// Make specific properties required
export type RequiredBy<T, K extends keyof T> = Except<T, K> & Required<Pick<T, K>>;

// Extract the type of array elements
export type ArrayElement<ArrayType extends readonly unknown[]> = ArrayType[number];

// Extract the return type of a promise
export type PromiseType<T extends Promise<any>> = T extends Promise<infer U> ? U : never;

// Function types
export type AnyFunction = (...args: any[]) => any;
export type VoidFunction = () => void;
export type AsyncFunction<T = any> = (...args: any[]) => Promise<T>;

// Event handler types
export type ChangeHandler<T = HTMLInputElement> = (event: React.ChangeEvent<T>) => void;
export type ClickHandler<T = HTMLElement> = (event: React.MouseEvent<T>) => void;
export type SubmitHandler = (event: React.FormEvent<HTMLFormElement>) => void;
export type KeyHandler<T = HTMLElement> = (event: React.KeyboardEvent<T>) => void;

// Discriminated union helper
export type DiscriminatedUnion<K extends PropertyKey, T extends Record<K, any>> = T;

// Status types
export type Status = 'idle' | 'pending' | 'success' | 'error';
export type AsyncStatus = 'idle' | 'loading' | 'success' | 'error';

// Nullable type
export type Nullable<T> = T | null;
export type Optional<T> = T | undefined;
export type Maybe<T> = T | null | undefined;

// String literal helpers
export type Prefix<T extends string, P extends string> = `${P}${T}`;
export type Suffix<T extends string, S extends string> = `${T}${S}`;

// Object key paths
export type Path<T> = T extends object
  ? {
      [K in keyof T]: K extends string
        ? T[K] extends object
          ? K | `${K}.${Path<T[K]>}`
          : K
        : never;
    }[keyof T]
  : never;

// Get nested property type by path
export type PathValue<T, P extends Path<T>> = P extends `${infer K}.${infer Rest}`
  ? K extends keyof T
    ? Rest extends Path<T[K]>
      ? PathValue<T[K], Rest>
      : never
    : never
  : P extends keyof T
  ? T[P]
  : never;

// Merge two types
export type Merge<T, U> = Omit<T, keyof U> & U;

// Strict type checking
export type Exact<T, Shape> = T extends Shape
  ? Exclude<keyof T, keyof Shape> extends never
    ? T
    : never
  : never;