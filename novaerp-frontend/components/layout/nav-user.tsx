"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Logout03Icon, MoreVerticalIcon, Settings01Icon } from "@hugeicons/core-free-icons";
import type React from "react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  Menu,
  MenuItem,
  MenuGroup,
  MenuGroupLabel,
  MenuPopup,
  MenuSeparator,
  MenuTrigger,
} from "@/components/ui/menu";
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";

export function NavUser({
  user,
  onLogout,
}: {
  user: { name: string; email: string };
  onLogout?: () => void;
}): React.ReactElement {
  const { isMobile } = useSidebar();
  const initials = user.name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <Menu>
          <MenuTrigger
            render={
              <SidebarMenuButton
                size="lg"
                className="data-[popup-open]:bg-sidebar-accent data-[popup-open]:text-sidebar-accent-foreground"
              />
            }
          >
            <Avatar className="size-8 rounded-lg">
              <AvatarFallback className="rounded-lg">
                {initials}
              </AvatarFallback>
            </Avatar>
            <div className="grid flex-1 text-left text-sm leading-tight">
              <span className="truncate font-medium">{user.name}</span>
              <span className="truncate text-muted-foreground text-xs">
                {user.email}
              </span>
            </div>
            <HugeiconsIcon
              icon={MoreVerticalIcon}
              strokeWidth={2}
              className="ms-auto size-4"
            />
          </MenuTrigger>
          <MenuPopup
            className="w-(--anchor-width) min-w-56"
            side={isMobile ? "bottom" : "right"}
            align="end"
            sideOffset={4}
          >
            <MenuGroup>
              <MenuGroupLabel className="p-0 font-normal">
                <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                  <Avatar className="size-8 rounded-lg">
                    <AvatarFallback className="rounded-lg">
                      {initials}
                    </AvatarFallback>
                  </Avatar>
                  <div className="grid flex-1 text-left text-sm leading-tight">
                    <span className="truncate font-medium">{user.name}</span>
                    <span className="truncate text-muted-foreground text-xs">
                      {user.email}
                    </span>
                  </div>
                </div>
              </MenuGroupLabel>
            </MenuGroup>
            <MenuSeparator />
            <MenuItem>
              <HugeiconsIcon icon={Settings01Icon} strokeWidth={2} />
              Paramètres
            </MenuItem>
            <MenuSeparator />
            <MenuItem variant="destructive" onClick={onLogout}>
              <HugeiconsIcon icon={Logout03Icon} strokeWidth={2} />
              Déconnexion
            </MenuItem>
          </MenuPopup>
        </Menu>
      </SidebarMenuItem>
    </SidebarMenu>
  );
}
