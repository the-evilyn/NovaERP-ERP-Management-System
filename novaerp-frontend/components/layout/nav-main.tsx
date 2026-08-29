"use client";

import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type React from "react";
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";

export type NavMainItem = {
  title: string;
  url: string;
  icon: IconSvgElement;
};

export function NavMain({
  items,
}: {
  items: NavMainItem[];
}): React.ReactElement {
  const pathname = usePathname();

  return (
    <SidebarGroup className="mt-2">
      <SidebarGroupContent className="flex flex-col gap-2">
        <SidebarMenu>
          {items.map((item, index) => {
            const isActive =
              pathname === item.url || pathname.startsWith(`${item.url}/`);
            const isFirstInGroup = index % 3 === 0 && index !== 0;

            return (
              <SidebarMenuItem
                key={item.url}
                className={isFirstInGroup ? "mt-4" : ""}
              >
                <SidebarMenuButton
                  isActive={isActive}
                  tooltip={item.title}
                  render={<Link href={item.url} />}
                  className="h-9"
                >
                  <HugeiconsIcon
                    icon={item.icon}
                    strokeWidth={1.5}
                    className="size-4.5"
                  />
                  <span className="text-sm">{item.title}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            );
          })}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
