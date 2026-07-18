import { InputHTMLAttributes } from 'react';
export function Input({className='',...props}:InputHTMLAttributes<HTMLInputElement>){return <input className={`rounded-full border border-slate-200 bg-white px-4 py-3 text-heading outline-none focus:border-primary ${className}`} {...props}/>}
