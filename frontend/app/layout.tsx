import type { Metadata } from 'next';import localFont from 'next/font/local';import './globals.css';import { Nav } from '@/components/layout/Nav';
const space=localFont({src:'../public/fonts/SpaceGrotesk.ttf',variable:'--font-space'});
export const metadata:Metadata={metadataBase:new URL(process.env.NEXT_PUBLIC_SITE_URL??'http://localhost:3000'),title:{default:'TripBook',template:'%s | TripBook'},description:'Flight and hotel booking platform',icons:{icon:'/favicon.svg'},openGraph:{title:'TripBook',description:'Flights, hotels, and reliable booking flows',images:['/og.svg']}};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="en" className={space.variable}><body><Nav />{children}</body></html>}
