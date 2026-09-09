export interface CharSequence { length(): number; charAt(index: number): string; subSequence(start: number, end: number): CharSequence; toString(): string; }
