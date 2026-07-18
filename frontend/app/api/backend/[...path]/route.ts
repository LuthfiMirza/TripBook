import { NextResponse, type NextRequest } from 'next/server';
import { API_BASE_URL } from '@/lib/api';

type Ctx = { params: { path: string[] } };

async function proxy(req: NextRequest, ctx: Ctx) {
  const token = req.cookies.get('tripbook_token')?.value;
  const headers = new Headers(req.headers);
  headers.delete('host');
  // This hop is server-to-server (Next.js -> Spring Boot), never a browser
  // request, so it must not carry browser-origin headers. Spring Security's
  // CORS filter checks Origin regardless of who actually sent it — forwarding
  // the browser's real Origin here made the backend reject every proxied call
  // as an invalid cross-origin request (403 "Invalid CORS request") the
  // moment the frontend ran on any port other than the one in
  // app.cors.allowed-origin.
  headers.delete('origin');
  headers.delete('referer');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const body = ['GET', 'HEAD'].includes(req.method) ? undefined : await req.text();
  const res = await fetch(`${API_BASE_URL}/api/${ctx.params.path.join('/')}${req.nextUrl.search}`, {
    method: req.method,
    headers,
    body,
    cache: 'no-store',
  });
  const text = await res.text();
  return new NextResponse(text, {
    status: res.status,
    headers: { 'Content-Type': res.headers.get('Content-Type') ?? 'application/json' },
  });
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const DELETE = proxy;
