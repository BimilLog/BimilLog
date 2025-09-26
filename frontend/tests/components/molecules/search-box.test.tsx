import { useState, type ComponentProps } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SearchBox } from "@/components/molecules/forms/search-box";

function ControlledSearchBox(props: Partial<ComponentProps<typeof SearchBox>>) {
  const [value, setValue] = useState("");

  return (
    <SearchBox
      value={value}
      onChange={(nextValue) => {
        setValue(nextValue);
        props.onChange?.(nextValue);
      }}
      {...props}
    />
  );
}

describe("SearchBox", () => {
  it("엔터 입력 시 onSearch를 호출한다", async () => {
    const user = userEvent.setup();
    const handleSearch = vi.fn();

    render(
      <ControlledSearchBox
        placeholder="검색"
        onSearch={handleSearch}
      />
    );

    const input = screen.getByPlaceholderText("검색");
    await user.type(input, "테스트{enter}");

    expect(handleSearch).toHaveBeenCalledTimes(1);
  });

  it("초기화 버튼 클릭 시 값을 비우고 onClear를 호출한다", async () => {
    const user = userEvent.setup();
    const handleChange = vi.fn();
    const handleClear = vi.fn();

    render(
      <ControlledSearchBox
        placeholder="검색"
        onChange={handleChange}
        onClear={handleClear}
      />
    );

    const input = screen.getByPlaceholderText("검색");
    await user.type(input, "hello");

    const buttons = screen.getAllByRole("button");
    await user.click(buttons[0]);

    expect(handleChange).toHaveBeenLastCalledWith("");
    expect(handleClear).toHaveBeenCalledTimes(1);
    expect(input).toHaveValue("");
  });
});
