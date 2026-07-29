import type { ComponentPropsWithoutRef, ReactNode } from 'react'

type CardProps = ComponentPropsWithoutRef<'section'> & {
  children: ReactNode
}

export function Card({ children, className = '', ...props }: CardProps) {
  return <section className={`card ${className}`} {...props}>{children}</section>
}
