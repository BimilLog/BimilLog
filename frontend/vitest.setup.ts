import "@testing-library/jest-dom";
import { vi } from "vitest";

const createStorage = () => {
  let store: Record<string, string> = {};

  const storage = {
    getItem: vi.fn((key: string) => (key in store ? store[key] : null)),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value.toString();
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      store = {};
    }),
    key: vi.fn((index: number) => Object.keys(store)[index] ?? null),
  } as Storage;

  Object.defineProperty(storage, "length", {
    get: () => Object.keys(store).length,
  });

  return storage;
};

const localStorageMock = createStorage();
const sessionStorageMock = createStorage();

Object.defineProperty(window, "localStorage", {
  value: localStorageMock,
  writable: true,
});

Object.defineProperty(window, "sessionStorage", {
  value: sessionStorageMock,
  writable: true,
});

// window.matchMedia 모킹
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // Deprecated
    removeListener: vi.fn(), // Deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// EventSource 모킹
class MockEventSource {
  url: string;
  readyState: number = 0;
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  close = vi.fn();

  constructor(url: string) {
    this.url = url;
    this.readyState = 1; // OPEN
    setTimeout(() => {
      if (this.onopen) {
        this.onopen(new Event("open"));
      }
    }, 0);
  }

  addEventListener = vi.fn((type: string, handler: EventListener) => {
    if (type === "open") this.onopen = handler as any;
    if (type === "message") this.onmessage = handler as any;
    if (type === "error") this.onerror = handler as any;
  });

  removeEventListener = vi.fn();
  dispatchEvent = vi.fn();
}

global.EventSource = MockEventSource as any;

// Quill 에디터 모킹
vi.mock("quill", () => ({
  default: vi.fn().mockImplementation(() => ({
    root: { innerHTML: "" },
    setText: vi.fn(),
    getText: vi.fn(() => ""),
    getHTML: vi.fn(() => ""),
    getContents: vi.fn(() => ({ ops: [] })),
    setContents: vi.fn(),
    on: vi.fn(),
    off: vi.fn(),
    focus: vi.fn(),
    blur: vi.fn(),
    enable: vi.fn(),
    disable: vi.fn(),
  })),
}));

// react-quill 모킹
vi.mock("react-quill", () => ({
  default: vi.fn(() => {
    // 실제 컴포넌트는 테스트 파일에서 직접 모킹
    return null;
  }),
}));
