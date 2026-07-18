import Link from 'next/link';
import { FilterPanel } from '@/components/search/FilterPanel';
import { apiFetch } from '@/lib/api';
import { duration, rupiah, time } from '@/lib/format';
import type { FlightSearchResponse, HotelSearchResponse, PagedResponse } from '@/types';

type Props = { searchParams: Record<string, string | string[] | undefined> };
function value(v: string | string[] | undefined, fallback = '') {
  return Array.isArray(v) ? v[0] ?? fallback : v ?? fallback;
}
function values(v: string | string[] | undefined) {
  return Array.isArray(v) ? v : v ? [v] : [];
}
export const metadata = { title: 'Search' };

export default async function Search({ searchParams }: Props) {
  const type = value(searchParams.type, 'flight');
  const maxPrice = Number(value(searchParams.maxPrice, '3000000'));
  const airlines = values(searchParams.airline);
  const bucket = value(searchParams.bucket);
  const sort = value(searchParams.sort, 'price_asc');
  let error = '';
  let flights: FlightSearchResponse[] = [];
  let hotels: HotelSearchResponse[] = [];

  try {
    if (type === 'hotel') {
      const city = value(searchParams.city, 'Bali');
      const checkIn = value(searchParams.checkIn, '2026-08-01');
      const checkOut = value(searchParams.checkOut, '2026-08-03');
      const guests = value(searchParams.guests, '2');
      const requestUrl = `/api/hotels/search?city=${city}&checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}&sort=${sort}`;
      hotels = (await apiFetch<PagedResponse<HotelSearchResponse>>(requestUrl)).content.filter(
        (h) => Number(h.pricePerNight) <= maxPrice,
      );
    } else {
      const origin = value(searchParams.origin, 'CGK');
      const destination = value(searchParams.destination, 'DPS');
      const date = value(searchParams.date, '2026-08-01');
      const passengers = value(searchParams.passengers, '1');
      const requestUrl = `/api/flights/search?origin=${origin}&destination=${destination}&date=${date}&passengers=${passengers}&sort=${sort}`;
      flights = (await apiFetch<PagedResponse<FlightSearchResponse>>(requestUrl)).content
        .filter((f) => Number(f.price) <= maxPrice)
        .filter((f) => (airlines.length ? airlines.includes(f.airline) : true))
        .filter((f) => {
          const h = new Date(f.departureTime).getHours();
          return !bucket || (bucket === 'morning' && h < 12) || (bucket === 'afternoon' && h >= 12 && h < 17) || (bucket === 'evening' && h >= 17);
        });
    }
  } catch (e) {
    error = e instanceof Error ? e.message : 'API failed';
  }

  return (
    <main className="mx-auto min-h-screen max-w-6xl px-6 pt-28">
      <div className="mb-6 rounded-2xl bg-primary p-8 text-white">
        <h1 className="display text-4xl font-bold">Search results</h1>
      </div>
      <div className="grid gap-6 md:grid-cols-[280px_1fr]">
        <FilterPanel maxPrice={maxPrice} />
        <section>
          {error && <div className="rounded-2xl bg-red-50 p-6 text-red-700">{error}</div>}
          {!error && type !== 'hotel' && flights.length === 0 && <div className="rounded-2xl bg-white p-8">No flights found.</div>}
          {!error && type === 'hotel' && hotels.length === 0 && <div className="rounded-2xl bg-white p-8">No hotels found.</div>}
          {flights.map((f) => (
            <article key={f.id} className="mb-4 rounded-2xl bg-white p-6 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="font-bold text-heading">
                    {f.airline} · {f.flightCode}
                  </p>
                  <p className="text-sm">
                    {f.origin} {time(f.departureTime)} → {f.destination} {time(f.arrivalTime)} · {duration(f.departureTime, f.arrivalTime)}
                  </p>
                  <p className="text-sm">{f.availableSeats} seats available</p>
                </div>
                <div className="text-right">
                  <p className="text-xl font-black text-heading">{rupiah(Number(f.price))}</p>
                  <Link className="mt-3 inline-block rounded-full bg-accent px-5 py-2 font-bold text-heading" href={`/flights/${f.id}`}>
                    Select
                  </Link>
                </div>
              </div>
            </article>
          ))}
          {hotels.map((h) => (
            <article key={h.id} className="mb-4 rounded-2xl bg-white p-6 shadow-sm">
              <p className="font-bold text-heading">{h.name}</p>
              <p>
                {h.city} · {h.availableRooms} rooms · {h.starRating ?? '-'} stars
              </p>
              <p className="font-black text-heading">{rupiah(Number(h.pricePerNight))}/night</p>
              <Link className="mt-3 inline-block rounded-full bg-accent px-5 py-2 font-bold text-heading" href={`/hotels/${h.id}`}>
                Select
              </Link>
            </article>
          ))}
        </section>
      </div>
    </main>
  );
}
