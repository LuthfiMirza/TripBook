'use client';
import { useEffect,useState } from 'react';import type { User } from '@/types';
export function useAuth(){const [user,setUser]=useState<User|null>(null);const [loading,setLoading]=useState(true);useEffect(()=>{fetch('/api/auth/session').then(r=>r.ok?r.json():null).then(d=>setUser(d?.user??null)).finally(()=>setLoading(false));},[]);return{user,loading};}
