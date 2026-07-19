'use client';
import type { ReactNode } from 'react';
import { motion, useReducedMotion, type HTMLMotionProps } from 'framer-motion';

type RevealProps = Omit<HTMLMotionProps<'div'>, 'children'> & {
  /** Stagger delay in seconds — pass index * 0.1 for a card grid. */
  delay?: number;
  children: ReactNode;
};

/**
 * Fade + slide-up on scroll, once per element — matches the reveal pattern
 * used across the reference site (opacity 0->1, translateY ~40px->0,
 * ~0.6-0.8s, triggered on viewport entry rather than looping). Disabled
 * entirely under prefers-reduced-motion instead of just shortening the
 * duration, since a user who asked for reduced motion shouldn't get any
 * movement at all.
 */
export function Reveal({ delay = 0, children, ...props }: RevealProps) {
  const reduceMotion = useReducedMotion();

  if (reduceMotion) {
    return <div {...(props as React.HTMLAttributes<HTMLDivElement>)}>{children}</div>;
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 40 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.2 }}
      transition={{ duration: 0.6, delay, ease: 'easeOut' }}
      {...props}
    >
      {children}
    </motion.div>
  );
}
