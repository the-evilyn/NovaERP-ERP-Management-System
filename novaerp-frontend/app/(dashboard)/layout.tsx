'use client';

import { useEffect } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { AppSidebar } from '@/components/layout/app-sidebar';
import { SiteHeader } from '@/components/layout/site-header';
import { Spinner } from '@/components/ui/spinner';
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar';
import { useAuth } from '@/providers/auth-provider';

const ADMIN_ONLY_ROUTES = [
  '/import-export',
  '/purchases',
  '/invoices',
  '/payments',
  '/warehouses',
  '/users',
  '/stock/transfers/new',
];

export default function DashboardLayout({ children }: LayoutProps<'/'>) {
  const router = useRouter();
  const pathname = usePathname();
  const { user, isLoading } = useAuth();

  const isAdminOnlyRoute =
    ADMIN_ONLY_ROUTES.some(
      (route) => pathname === route || pathname.startsWith(`${route}/`)
    ) ||
    (pathname.startsWith('/stock/transfers/') && pathname.endsWith('/edit'));

  useEffect(() => {
    if (!isLoading) {
      if (!user) {
        router.replace('/login');
      } else if (user.role !== 'ADMIN' && isAdminOnlyRoute) {
        router.replace('/dashboard');
      }
    }
  }, [isLoading, user, router, isAdminOnlyRoute]);

  if (isLoading || !user || (user.role !== 'ADMIN' && isAdminOnlyRoute)) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner className="size-6 text-muted-foreground" />
      </div>
    );
  }

  return (
    <SidebarProvider>
      <AppSidebar variant="inset" />
      <SidebarInset>
        <SiteHeader />
        <div className="flex flex-1 flex-col gap-4 p-4">{children}</div>
      </SidebarInset>
    </SidebarProvider>
  );
}
