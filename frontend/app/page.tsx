import Image from 'next/image';
import Link from 'next/link';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Reveal } from '@/components/ui/Reveal';
import { SearchBar } from '@/components/search/SearchBar';

const destinations = [
  ['dps', 'Bali', 'Aug getaway', 'from Rp650k'],
  ['cgk', 'Jakarta', 'City breaks', 'from Rp550k'],
  ['jog', 'Yogyakarta', 'Heritage trip', 'from Rp480k'],
  ['sub', 'Surabaya', 'Business route', 'from Rp480k'],
  ['upg', 'Makassar', 'Coastal escape', 'from Rp900k'],
  ['kno', 'Medan', 'Lake Toba gateway', 'from Rp500k'],
];

const benefits = [
  ['Real-time seat availability', 'See exactly which seats and rooms are free — no waiting for a callback to find out something is already sold out.'],
  ['No double-booked seats', 'Even if two people click the same seat at once, only one booking goes through — the other gets a clear, instant answer.'],
  ['Instant confirmation', 'Your booking reference and confirmation email are ready the moment you book — no waiting around.'],
];

export default function Home() {
  return (
    <main className="overflow-hidden">
      <section className="relative bg-primary px-6 pb-36 pt-32 text-center text-white">
        <p className="font-semibold uppercase tracking-[.35em] text-blue-100">Airplane-ready booking</p>
        <h1 className="display mt-4 text-7xl font-black md:text-[10rem]">TripBook</h1>
        <Reveal delay={0.15}>
          <p className="mx-auto mt-4 max-w-2xl text-xl text-blue-50">
            Find flights and hotels across Indonesia and book with confidence — real-time availability, instant confirmation.
          </p>
        </Reveal>
        <svg className="absolute left-[12%] top-40 w-32 text-accent" viewBox="0 0 120 40" fill="none">
          <path d="M4 30C35 2 80 2 116 22" stroke="currentColor" strokeWidth="6" strokeLinecap="round" />
        </svg>
        <Image
          src="/hero/plane.png"
          alt="Airplane cutout"
          width={900}
          height={520}
          priority
          className="plane-float absolute left-1/2 top-[65%] w-[min(88vw,760px)] drop-shadow-2xl"
        />
      </section>

      <section className="relative z-10 -mt-16 px-6">
        <SearchBar />
      </section>

      <section className="bg-white px-6 py-20">
        <div className="mx-auto max-w-6xl">
          <Reveal className="mb-8 flex items-end justify-between">
            <h2 className="display text-4xl font-bold text-heading">Explore Top Destinations</h2>
            <button className="grid h-12 w-12 place-items-center rounded-full bg-primary text-white transition hover:rotate-[-8deg]">→</button>
          </Reveal>
          <div className="no-scrollbar flex gap-5 overflow-x-auto pb-4">
            {destinations.map(([img, name, copy, price], i) => (
              <Reveal key={img} delay={i * 0.08} className="min-w-56">
                <Card className="p-3 transition hover:-translate-y-1 hover:shadow-xl">
                  <Image src={`/destinations/${img}.webp`} alt={name} width={300} height={400} sizes="224px" className="h-64 rounded-xl object-cover" />
                  <h3 className="mt-4 font-bold text-heading">{name}</h3>
                  <p>{copy}</p>
                  <Badge>{price}</Badge>
                </Card>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section className="curved bg-section-alt px-6 py-20">
        <div className="mx-auto grid max-w-6xl gap-6 md:grid-cols-3">
          {benefits.map(([title, copy], i) => (
            <Reveal key={title} delay={i * 0.1}>
              <Card className="transition hover:-translate-y-1">
                <h3 className="text-xl font-bold text-heading">{title}</h3>
                <p className="mt-2">{copy}</p>
              </Card>
            </Reveal>
          ))}
        </div>
      </section>

      <section className="bg-white px-6 py-16">
        <div className="mx-auto max-w-6xl">
          <Reveal>
            <h2 className="display text-3xl font-bold text-heading">Popular Airlines</h2>
            <div className="mt-6 flex flex-wrap gap-3">
              {['Garuda', 'Lion Air', 'Citilink', 'Batik', 'AirAsia'].map((a) => (
                <span key={a} className="rounded-full bg-primary-tint px-5 py-3 font-semibold text-primary">
                  {a}
                </span>
              ))}
            </div>
          </Reveal>
          <footer className="mt-12 text-sm text-body">
            Destination photos are local assets with attribution recorded in{' '}
            <Link className="text-primary underline" href="/destinations/CREDITS.md">
              CREDITS.md
            </Link>
            .
          </footer>
        </div>
      </section>
    </main>
  );
}
