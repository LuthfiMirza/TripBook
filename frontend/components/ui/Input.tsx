import { forwardRef, InputHTMLAttributes } from 'react';

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function Input({ className = '', ...props }, ref) {
    return (
      <input
        ref={ref}
        className={`rounded-full border border-slate-200 bg-white px-4 py-3 text-heading outline-none focus:border-primary ${className}`}
        {...props}
      />
    );
  },
);
