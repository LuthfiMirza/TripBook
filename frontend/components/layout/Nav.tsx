'use client';
import { useState } from 'react';
import Link from 'next/link';

export function Nav() {
  const [open, setOpen] = useState(false);

  return (
    <nav className="fixed left-1/2 top-4 z-50 w-[min(94%,980px)] -translate-x-1/2">
      <div className="flex items-center justify-between rounded-full bg-white/80 px-4 py-3 shadow-lg backdrop-blur">
        <Link href="/" className="display text-lg font-bold text-primary sm:text-xl">
          TripBook
        </Link>
        <div className="flex items-center gap-2 text-sm font-semibold sm:gap-3">
          <Link className="hidden sm:inline" href="/search">
            Search
          </Link>
          <Link className="hidden sm:inline" href="/bookings">
            Bookings
          </Link>
          <Link className="rounded-full bg-accent px-4 py-2 text-heading" href="/login">
            Login
          </Link>
          <button
            type="button"
            aria-label="Toggle menu"
            aria-expanded={open}
            onClick={() => setOpen((v) => !v)}
            className="grid h-9 w-9 place-items-center rounded-full bg-primary text-white sm:hidden"
          >
            {open ? '✕' : '☰'}
          </button>
        </div>
      </div>
      {open && (
        <div className="mt-2 flex flex-col gap-1 rounded-2xl bg-white p-3 shadow-lg sm:hidden">
          <Link className="rounded-xl px-3 py-2 font-semibold hover:bg-primary-tint" href="/search" onClick={() => setOpen(false)}>
            Search
          </Link>
          <Link className="rounded-xl px-3 py-2 font-semibold hover:bg-primary-tint" href="/bookings" onClick={() => setOpen(false)}>
            Bookings
          </Link>
        </div>
      )}
    </nav>
  );
}
