import { NextResponse, type NextRequest } from 'next/server';
export function middleware(req:NextRequest){const token=req.cookies.get('tripbook_token'); if(!token){return NextResponse.redirect(new URL('/login',req.url));} return NextResponse.next();}
export const config={matcher:['/bookings/:path*','/flights/:path*','/hotels/:path*']};
