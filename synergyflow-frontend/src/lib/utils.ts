import { twMerge } from 'tailwind-merge';

type ClassValue = string | number | null | undefined | false;

export function cn(...inputs: ClassValue[]) {
  // In Tailwind v4, twMerge still helps avoid class conflicts
  return twMerge(inputs.filter(Boolean).join(' '));
}
