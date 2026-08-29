"use client";

import { usePathname } from "next/navigation";
import type React from "react";
import { useMemo } from "react";
import { Separator } from "@/components/ui/separator";
import { SidebarTrigger } from "@/components/ui/sidebar";

const routeTitles: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/articles": "Articles",
  "/categories": "Catégories",
  "/suppliers": "Fournisseurs",
  "/units": "Unités",
  "/stock-movements": "Mouvements de stock",
  "/import-export": "Import / Export",
};

export function SiteHeader(): React.ReactElement {
  const pathname = usePathname();

  const title = useMemo(() => {
    if (routeTitles[pathname]) return routeTitles[pathname];

    const base = `/${pathname.split("/").filter(Boolean)[0] ?? ""}`;
    return routeTitles[base] ?? "Dashboard";
  }, [pathname]);

  return (
    <header className="flex h-14 shrink-0 items-center gap-2 border-b px-4">
      <SidebarTrigger />
      <Separator orientation="vertical" className="h-4" />
      <h1 className="font-medium text-base">{title}</h1>
    </header>
  );
}
