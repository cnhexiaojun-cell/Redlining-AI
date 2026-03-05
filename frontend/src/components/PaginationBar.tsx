import { useState } from "react";

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];

interface PaginationBarProps {
  total: number;
  page: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
  t: (key: string) => string;
  pageSizeOptions?: number[];
}

export default function PaginationBar({
  total,
  page,
  pageSize,
  onPageChange,
  onPageSizeChange,
  t,
  pageSizeOptions = PAGE_SIZE_OPTIONS,
}: PaginationBarProps) {
  const [jumpInput, setJumpInput] = useState("");
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const currentPage1 = page + 1;

  /** Page numbers (1-based) to show between prev/next, with optional ellipsis */
  const getPageNumbers = (): (number | "ellipsis")[] => {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i + 1);
    }
    const pages: (number | "ellipsis")[] = [];
    const showLeft = currentPage1 <= 4;
    const showRight = currentPage1 >= totalPages - 3;
    if (showLeft) {
      for (let i = 1; i <= 5; i++) pages.push(i);
      pages.push("ellipsis");
      pages.push(totalPages);
    } else if (showRight) {
      pages.push(1);
      pages.push("ellipsis");
      for (let i = totalPages - 4; i <= totalPages; i++) pages.push(i);
    } else {
      pages.push(1);
      pages.push("ellipsis");
      for (let i = currentPage1 - 1; i <= currentPage1 + 1; i++) pages.push(i);
      pages.push("ellipsis");
      pages.push(totalPages);
    }
    return pages;
  };

  const handleJump = () => {
    const num = parseInt(jumpInput.trim(), 10);
    if (!Number.isNaN(num) && num >= 1 && num <= totalPages) {
      onPageChange(num - 1);
      setJumpInput("");
    }
  };

  return (
    <div className="px-4 py-2 border-t border-gray-100 flex flex-wrap items-center justify-between gap-2 text-sm text-gray-500">
      <span>{t("admin_total").replace("{total}", String(total))}</span>
      <div className="flex flex-wrap items-center gap-3">
        <span className="flex items-center gap-1">
          {t("admin_per_page")}
          <select
            value={pageSize}
            onChange={(e) => {
              onPageSizeChange(Number(e.target.value));
              onPageChange(0);
            }}
            className="ml-1 border border-gray-300 rounded px-2 py-1 text-sm"
          >
            {pageSizeOptions.map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </span>
        <button
          type="button"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          className="px-3 py-1 rounded border border-gray-200 disabled:opacity-50"
        >
          {t("admin_prev_page")}
        </button>
        <div className="flex items-center gap-1">
          {getPageNumbers().map((item, idx) =>
            item === "ellipsis" ? (
              <span key={`ellipsis-${idx}`} className="px-1 text-gray-400" aria-hidden>
                …
              </span>
            ) : (
              <button
                key={item}
                type="button"
                onClick={() => onPageChange(item - 1)}
                className={`min-w-[2rem] px-2 py-1 rounded border text-sm ${
                  item === currentPage1
                    ? "border-blue-500 bg-blue-50 text-blue-700 font-medium"
                    : "border-gray-200 hover:bg-gray-50"
                }`}
                aria-current={item === currentPage1 ? "page" : undefined}
              >
                {item}
              </button>
            )
          )}
        </div>
        <button
          type="button"
          disabled={currentPage1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
          className="px-3 py-1 rounded border border-gray-200 disabled:opacity-50"
        >
          {t("admin_next_page")}
        </button>
        <span className="flex items-center gap-1">
          {t("admin_jump_to")}
          <input
            type="number"
            min={1}
            max={totalPages}
            value={jumpInput}
            onChange={(e) => setJumpInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), handleJump())}
            placeholder={String(currentPage1)}
            className="w-14 border border-gray-300 rounded px-2 py-1 text-sm text-center"
          />
          {t("admin_page")}
          <button
            type="button"
            onClick={handleJump}
            className="ml-1 px-2 py-1 rounded border border-gray-200 hover:bg-gray-50"
          >
            {t("admin_go_to_page")}
          </button>
        </span>
      </div>
    </div>
  );
}

export { PAGE_SIZE_OPTIONS };
