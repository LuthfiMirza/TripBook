import { ButtonHTMLAttributes } from 'react';
export function Button({className='',...props}:ButtonHTMLAttributes<HTMLButtonElement>){return <button className={`rounded-full bg-accent px-5 py-3 font-semibold text-heading transition hover:scale-[1.02] disabled:opacity-60 ${className}`} {...props}/>}
