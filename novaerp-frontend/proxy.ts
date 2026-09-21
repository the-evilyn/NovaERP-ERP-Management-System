import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const TOKEN_COOKIE_NAME = 'novaerp_token';
const ROLE_COOKIE_NAME = 'novaerp_role';
const AUTH_PAGES = ['/login', '/register', '/forgot-password', '/reset-password'];
const ADMIN_ONLY_ROUTES = [
  '/import-export',
  '/users',
  '/stock/transfers/new',
];

function extractRoleFromToken(token: string): string | null {
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    const parsed = JSON.parse(jsonPayload);
    return parsed.role ?? null;
  } catch {
    return null;
  }
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get(TOKEN_COOKIE_NAME)?.value;
  const hasToken = Boolean(token);
  const isAuthPage = AUTH_PAGES.some((page) => pathname === page || pathname.startsWith(`${page}/`));

  if (!hasToken && !isAuthPage && pathname !== '/') {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  if (hasToken && isAuthPage) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  if (hasToken) {
    const isAdminRoute =
      ADMIN_ONLY_ROUTES.some(
        (route) => pathname === route || pathname.startsWith(`${route}/`)
      ) ||
      (pathname.startsWith('/stock/transfers/') && pathname.endsWith('/edit'));

    if (isAdminRoute) {
      const role =
        request.cookies.get(ROLE_COOKIE_NAME)?.value ||
        (token ? extractRoleFromToken(token) : null);

      if (role !== 'ADMIN') {
        return NextResponse.redirect(new URL('/dashboard', request.url));
      }
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico).*)'],
};
