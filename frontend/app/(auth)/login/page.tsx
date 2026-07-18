'use client';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

const schema = z.object({ email: z.string().email(), password: z.string().min(1) });
type Form = z.infer<typeof schema>;

export default function Login() {
  const { register, handleSubmit } = useForm<Form>({ resolver: zodResolver(schema) });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(values: Form) {
    setLoading(true);
    setError('');
    try {
      // Goes through the same-origin Next.js proxy, not the backend directly —
      // a direct browser fetch to the backend only works when the frontend's
      // origin happens to match the backend's CORS allowlist (localhost:3000),
      // which breaks the moment the frontend runs on any other port or origin
      // (a port conflict in dev, or any real deployment).
      const api = await fetch('/api/backend/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(values),
      });
      const data = await api.json();
      if (!api.ok) throw new Error(data.message);
      await fetch('/api/auth/session', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      location.href = '/bookings';
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="grid min-h-screen place-items-center pt-20">
      <form onSubmit={handleSubmit(onSubmit)} className="w-[min(92%,420px)] rounded-2xl bg-white p-8 shadow">
        <h1 className="display text-3xl font-bold text-heading">Welcome back</h1>
        <Input className="mt-6 w-full" placeholder="Email" {...register('email')} />
        <Input className="mt-3 w-full" type="password" placeholder="Password" {...register('password')} />
        {error && <p className="mt-3 rounded-xl bg-red-50 p-3 text-sm text-red-600">{error}</p>}
        <Button disabled={loading} className="mt-5 w-full">
          {loading ? 'Signing in...' : 'Login'}
        </Button>
      </form>
    </main>
  );
}
