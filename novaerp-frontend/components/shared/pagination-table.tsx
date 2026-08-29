"use client";

import {
  ChevronFirstIcon,
  ChevronLastIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
} from "lucide-react";
import type React from "react";
import { useId } from "react";
import { Label } from "@/components/ui/label";
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
} from "@/components/ui/pagination";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type PaginationTableProps = {
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalItems: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
};

export default function PaginationTable({
  currentPage,
  totalPages,
  pageSize,
  totalItems,
  onPageChange,
  onPageSizeChange,
}: PaginationTableProps): React.ReactElement {
  const id = useId();

  const pageSizeOptions = [10, 25, 50, 100];
  if (!pageSizeOptions.includes(pageSize)) {
    pageSizeOptions.push(pageSize);
    pageSizeOptions.sort((a, b) => a - b);
  }

  const handlePageSizeChange = (value: string | null) => {
    if (!value) return;
    onPageSizeChange(Number(value));
    onPageChange(1);
  };

  const handlePageChange = (page: number) => {
    if (page >= 1 && page <= totalPages && totalPages > 1) {
      onPageChange(page);
    }
  };

  const startItem = totalItems === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const endItem = Math.min(currentPage * pageSize, totalItems);

  const isFirst = currentPage === 1 || totalPages <= 1;
  const isLast = currentPage === totalPages || totalPages <= 1;

  return (
    <div className="flex items-center justify-between gap-8">
      {/* Results per page */}
      <div className="flex items-center gap-3">
        <Label htmlFor={id}>Lignes par page</Label>
        <Select value={String(pageSize)} onValueChange={handlePageSizeChange}>
          <SelectTrigger className="w-fit whitespace-nowrap" id={id}>
            <SelectValue placeholder="Select number of results" />
          </SelectTrigger>
          <SelectContent className="[&_*[role=option]>span]:start-auto [&_*[role=option]>span]:end-2 [&_*[role=option]]:ps-2 [&_*[role=option]]:pe-8">
            {pageSizeOptions.map((size) => (
              <SelectItem key={size} value={String(size)}>
                {size}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Page number information */}
      <div className="flex grow justify-end whitespace-nowrap text-muted-foreground text-sm">
        <p
          aria-live="polite"
          className="whitespace-nowrap text-muted-foreground text-sm"
        >
          <span className="text-foreground">
            {startItem}-{endItem}
          </span>{" "}
          sur <span className="text-foreground">{totalItems}</span>
        </p>
      </div>

      {/* Pagination */}
      <div>
        <Pagination>
          <PaginationContent>
            {/* First page button */}
            <PaginationItem>
              <PaginationLink
                aria-disabled={isFirst ? true : undefined}
                aria-label="Go to first page"
                className="aria-disabled:pointer-events-none aria-disabled:opacity-50 cursor-pointer"
                onClick={() => {
                  if (!isFirst) handlePageChange(1);
                }}
              >
                <ChevronFirstIcon aria-hidden="true" size={16} />
              </PaginationLink>
            </PaginationItem>

            {/* Previous page button */}
            <PaginationItem>
              <PaginationLink
                aria-disabled={isFirst ? true : undefined}
                aria-label="Go to previous page"
                className="aria-disabled:pointer-events-none aria-disabled:opacity-50 cursor-pointer"
                onClick={() => {
                  if (!isFirst) handlePageChange(currentPage - 1);
                }}
              >
                <ChevronLeftIcon aria-hidden="true" size={16} />
              </PaginationLink>
            </PaginationItem>

            {/* Next page button */}
            <PaginationItem>
              <PaginationLink
                aria-disabled={isLast ? true : undefined}
                aria-label="Go to next page"
                className="aria-disabled:pointer-events-none aria-disabled:opacity-50 cursor-pointer"
                onClick={() => {
                  if (!isLast) handlePageChange(currentPage + 1);
                }}
              >
                <ChevronRightIcon aria-hidden="true" size={16} />
              </PaginationLink>
            </PaginationItem>

            {/* Last page button */}
            <PaginationItem>
              <PaginationLink
                aria-disabled={isLast ? true : undefined}
                aria-label="Go to last page"
                className="aria-disabled:pointer-events-none aria-disabled:opacity-50 cursor-pointer"
                onClick={() => {
                  if (!isLast) handlePageChange(totalPages);
                }}
              >
                <ChevronLastIcon aria-hidden="true" size={16} />
              </PaginationLink>
            </PaginationItem>
          </PaginationContent>
        </Pagination>
      </div>
    </div>
  );
}
