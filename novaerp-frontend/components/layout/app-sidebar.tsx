"use client";

import {
  AiBrain01Icon,
  ArrowDataTransferHorizontalIcon,
  Building03Icon,
  DashboardSquare01Icon,
  Exchange02Icon,
  FileExportIcon,
  Invoice01Icon,
  Package02Icon,
  ShieldUserIcon,
  ShoppingCart01Icon,
  Store01Icon,
  TruckDeliveryIcon,
  User02Icon,
} from "@hugeicons/core-free-icons";
import Link from "next/link";
import type React from "react";
import { NavMain, type NavMainItem } from "@/components/layout/nav-main";
import { NavUser } from "@/components/layout/nav-user";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { useAuth } from "@/providers/auth-provider";

const navMain: NavMainItem[] = [
  { title: "Dashboard", url: "/dashboard", icon: DashboardSquare01Icon },
  { title: "Aide à la décision", url: "/decisions", icon: AiBrain01Icon },
  { title: "Articles", url: "/articles", icon: ShoppingCart01Icon },
  { title: "Catégories", url: "/categories", icon: Store01Icon },
  { title: "Fournisseurs", url: "/suppliers", icon: TruckDeliveryIcon },
  { title: "Clients", url: "/clients", icon: User02Icon },
  { title: "Commandes Ventes", url: "/sales", icon: Invoice01Icon },
  { title: "Commandes Achats", url: "/purchases", icon: TruckDeliveryIcon },
  { title: "Factures", url: "/invoices", icon: Invoice01Icon },
  { title: "Paiements", url: "/payments", icon: Invoice01Icon },
  { title: "Unités", url: "/units", icon: Package02Icon },
  { title: "Entrepôts", url: "/warehouses", icon: Building03Icon },
  {
    title: "Mouvements de stock",
    url: "/stock-movements",
    icon: Exchange02Icon,
  },
  {
    title: "Transferts de stock",
    url: "/stock/transfers",
    icon: ArrowDataTransferHorizontalIcon,
  },
  {
    title: "Import / Export",
    url: "/import-export",
    icon: FileExportIcon,
  },
  {
    title: "Utilisateurs",
    url: "/users",
    icon: ShieldUserIcon,
  },
];

const ADMIN_ONLY_ROUTES = [
  "/sales",
  "/purchases",
  "/invoices",
  "/payments",
  "/warehouses",
  "/import-export",
  "/users",
];

export function AppSidebar(
  props: React.ComponentProps<typeof Sidebar>,
): React.ReactElement {
  const { user, logout } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const visibleNavItems = navMain.filter((item) => {
    if (ADMIN_ONLY_ROUTES.includes(item.url)) {
      return isAdmin;
    }
    return true;
  });

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" render={<Link href="/dashboard" />}>
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                <span className="font-heading font-semibold text-sm">N</span>
              </div>
              <div className="flex flex-col gap-0.5 leading-none">
                <span className="font-semibold">NovaERP</span>
                <span className="text-muted-foreground text-xs">
                  Gestion commerciale
                </span>
              </div>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={visibleNavItems} />
      </SidebarContent>
      <SidebarFooter>
        {user && (
          <NavUser
            user={{ name: user.fullName, email: user.email }}
            onLogout={logout}
          />
        )}
      </SidebarFooter>
    </Sidebar>
  );
}
