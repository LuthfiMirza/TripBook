'use client';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

const schema = z.object({
  fullName: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(8),
});
type Form = z.infer<typeof schema>;

export default function Register() {
  const { register, handleSubmit } = useForm<Form>({ resolver: zodResolver(schema) });
  const [msg, setMsg] = useState('');

  async function onSubmit(values: Form) {
    setMsg('');
    // Same-origin proxy, not a direct browser call to the backend — see the
    // comment in app/(auth)/login/page.tsx for why.
    const api = await fetch('/api/backend/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(values),
    });
    const data = await api.json();
    setMsg(api.ok ? `Registered ${data.email}` : data.message);
  }

  return (
    <main className="grid min-h-screen place-items-center pt-20">
      <form onSubmit={handleSubmit(onSubmit)} className="w-[min(92%,420px)] rounded-2xl bg-white p-8 shadow">
        <h1 className="display text-3xl font-bold text-heading">Create account</h1>
        <Input className="mt-6 w-full" placeholder="Full name" {...register('fullName')} />
        <Input className="mt-3 w-full" placeholder="Email" {...register('email')} />
        <Input className="mt-3 w-full" type="password" placeholder="Password" {...register('password')} />
        {msg && <p className="mt-3 rounded-xl bg-primary-tint p-3 text-sm text-primary">{msg}</p>}
        <Button className="mt-5 w-full">Register</Button>
      </form>
    </main>
  );
}
