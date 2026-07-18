import type { Config } from 'tailwindcss';
const config: Config = { content: ['./app/**/*.{ts,tsx}','./components/**/*.{ts,tsx}'], theme: { extend: { colors: { primary:'#2563EB','primary-dark':'#1D4ED8','primary-tint':'#EFF6FF',accent:'#FACC15',background:'#F8FAFC','section-alt':'#EEF2FF',heading:'#0F172A',body:'#475569'}, borderRadius:{'2xl':'1rem'}}}, plugins: []};
export default config;
