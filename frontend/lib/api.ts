import { cookies } from 'next/headers';
import type { ApiErrorShape } from '@/types';
export const API_BASE_URL=process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
export class ApiError extends Error{shape:ApiErrorShape; constructor(shape:ApiErrorShape){super(shape.message);this.shape=shape;}}
export async function apiFetch<T>(path:string, init:RequestInit={}){const headers=new Headers(init.headers); if(!headers.has('Content-Type')&&init.body)headers.set('Content-Type','application/json'); const token=cookies().get('tripbook_token')?.value; if(token)headers.set('Authorization',`Bearer ${token}`); const res=await fetch(`${API_BASE_URL}${path}`,{...init,headers,cache:'no-store'}); const text=await res.text(); const data=text?JSON.parse(text):null; if(!res.ok) throw new ApiError(data); return data as T;}
