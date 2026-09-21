"use client";

import type React from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  XAxis,
  YAxis,
} from "recharts";
import {
  type ChartConfig,
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from "@/components/ui/chart";
import { formatCurrency } from "@/lib/formatters";
import type { MonthlySalesEvolution } from "@/types/models";

const chartConfig = {
  revenue: {
    label: "Commandes validées (TTC)",
    color: "#2563eb", // blue-600
  },
  draftRevenue: {
    label: "Brouillons / En cours (TTC)",
    color: "#f59e0b", // amber-500
  },
} satisfies ChartConfig;

export function SalesEvolutionChart({
  data,
}: {
  data: MonthlySalesEvolution[];
}): React.ReactElement {
  if (!data || data.length === 0) {
    return (
      <div className="flex h-48 items-center justify-center text-muted-foreground text-sm">
        Aucune donnée d&apos;évolution des ventes disponible
      </div>
    );
  }

  const hasAnySales = data.some((d) => (d.totalRevenue ?? 0) > 0);

  return (
    <div className="flex flex-col gap-4">
      <ChartContainer
        config={chartConfig}
        className="aspect-auto h-[clamp(240px,_20vw,_340px)] w-full"
      >
        <BarChart
          accessibilityLayer
          data={data}
          margin={{ top: 12, right: 16, left: 16, bottom: 8 }}
        >
          <CartesianGrid vertical={false} strokeDasharray="3 3" opacity={0.3} />
          <XAxis
            dataKey="label"
            tickLine={false}
            axisLine={false}
            tickMargin={8}
            tick={{ fontSize: 12 }}
          />
          <YAxis
            tickLine={false}
            axisLine={false}
            tickMargin={8}
            width={70}
            tick={{ fontSize: 11 }}
            tickFormatter={(value) =>
              formatCurrency(Number(value), { compact: true })
            }
          />
          <ChartTooltip
            cursor={{ fill: "rgba(0, 0, 0, 0.04)" }}
            content={
              <ChartTooltipContent
                formatter={(value, name, item) => {
                  const num = Number(value);
                  const count =
                    name === "revenue"
                      ? item.payload.orderCount
                      : item.payload.totalOrderCount - item.payload.orderCount;
                  const countLabel =
                    count > 0
                      ? ` (${count} commande${count > 1 ? "s" : ""})`
                      : "";
                  return (
                    <div className="flex items-center justify-between gap-4 font-medium">
                      <span>{formatCurrency(num)}</span>
                      {countLabel && (
                        <span className="text-muted-foreground text-xs font-normal">
                          {countLabel}
                        </span>
                      )}
                    </div>
                  );
                }}
              />
            }
          />
          <Legend
            verticalAlign="top"
            align="right"
            iconType="circle"
            wrapperStyle={{ paddingBottom: "12px", fontSize: "12px" }}
          />
          <Bar
            dataKey="revenue"
            name="Commandes validées"
            stackId="sales"
            fill="var(--color-revenue, #2563eb)"
            radius={[0, 0, 4, 4]}
          />
          <Bar
            dataKey="draftRevenue"
            name="En cours / Brouillon"
            stackId="sales"
            fill="var(--color-draftRevenue, #f59e0b)"
            radius={[4, 4, 0, 0]}
          />
        </BarChart>
      </ChartContainer>

      {!hasAnySales && (
        <div className="text-center text-xs text-muted-foreground">
          Aucune commande n&apos;a encore été enregistrée sur cette période de 6 mois.
        </div>
      )}
    </div>
  );
}
