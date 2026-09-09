export interface Collection<T> extends Iterable<T> { size(): number; isEmpty(): boolean; contains(value: T): boolean; }
