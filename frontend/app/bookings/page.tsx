import { apiFetch } from '@/lib/api';import type { Booking, PagedResponse } from '@/types';import { BookingsList } from '@/components/booking/BookingsList';
export const metadata={title:'My Bookings'};
export default async function Bookings(){const data=await apiFetch<PagedResponse<Booking>>('/api/bookings');return <main className="mx-auto min-h-screen max-w-5xl px-6 pt-28"><h1 className="display text-4xl font-bold text-heading">My Bookings</h1><BookingsList initial={data.content}/></main>}
