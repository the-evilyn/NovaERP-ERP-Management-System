"use client";

import type React from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  XAxis,
  YAxis,
} from "recharts";
import {
  type ChartConfig,
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from "@/components/ui/chart";

export interface CategoryStockValue {
  categoryName: string;
  value: number;
}

// Fixed categorical hue order, validated with the dataviz skill's
// scripts/validate_palette.js against both the light (#fcfcfb) and dark
// (#1a1a19) chart surfaces — all checks pass (CVD separation in the 6-8
// floor band is mitigated by the direct category-name + value labels next
// to every bar, satisfying the "secondary encoding" requirement).
// "Autres" (overflow bucket) intentionally uses a neutral gray, not a 7th
// identity hue, since it represents a residual group rather than a category.
const CATEGORY_COLORS = [
  "#155dfc", // blue-600
  "#0092b8", // cyan-600
  "#e17100", // amber-600
  "#7f22fe", // violet-600
  "#ec003f", // rose-600
];
const OTHER_COLOR = "#6a7282"; // gray-500

const currency = new Intl.NumberFormat("fr-MA", {
  style: "currency",
  currency: "MAD",
  maximumFractionDigits: 0,
  notation: "compact",
});

const currencyFull = new Intl.NumberFormat("fr-MA", {
  style: "currency",
  currency: "MAD",
  maximumFractionDigits: 0,
});

function colorFor(categoryName: string, index: number): string {
  return categoryName === "Autres"
    ? OTHER_COLOR
    : CATEGORY_COLORS[index % CATEGORY_COLORS.length];
}

const chartConfig = {
  value: {
    label: "Valeur du stock",
  },
} satisfies ChartConfig;

export function StockValueByCategoryChart({
  data,
}: {
  data: CategoryStockValue[];
}): React.ReactElement {
  if (data.length === 0) {
    return (
      <div className="flex h-48 items-center justify-center text-muted-foreground text-sm">
        Aucune donnée de stock disponible
      </div>
    );
  }

  return (
    <ChartContainer
      config={chartConfig}
      className="aspect-auto h-[clamp(220px,_18vw,_320px)] w-full"
    >
      <BarChart
        accessibilityLayer
        data={data}
        layout="vertical"
        margin={{ left: 4, right: 76 }}
      >
        <CartesianGrid horizontal={false} stroke="none" />
        <XAxis type="number" domain={[0, "dataMax"]} hide />
        <YAxis
          dataKey="categoryName"
          type="category"
          tickLine={false}
          axisLine={false}
          tickMargin={8}
          width={110}
          tick={{ fontSize: 12 }}
        />
        <ChartTooltip
          cursor={false}
          content={
            <ChartTooltipContent
              hideLabel
              formatter={(value) => currencyFull.format(Number(value))}
            />
          }
        />
        <Bar dataKey="value" radius={4}>
          {data.map((entry, index) => (
            <Cell
              key={entry.categoryName}
              fill={colorFor(entry.categoryName, index)}
            />
          ))}
          <LabelList
            dataKey="value"
            position="right"
            className="fill-foreground"
            fontSize={12}
            formatter={(value) => currency.format(Number(value))}
          />
        </Bar>
      </BarChart>
    </ChartContainer>
  );
}
