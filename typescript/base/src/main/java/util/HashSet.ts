/** Insertion-preserving set with Java equality dispatch for common primitive values. */
export class HashSet<T> implements Iterable<T> {
    protected readonly values: T[] = [];
    public constructor(source?: Iterable<T>) {
        if (source !== undefined) for (const value of source) this.add(value);
    }
    public add(value: T): boolean {
        if (this.contains(value)) return false;
        this.values.push(value);
        return true;
    }
    public remove(value: T): boolean {
        const index = this.values.findIndex((current) => HashSet.equals(current, value));
        if (index < 0) return false;
        this.values.splice(index, 1);
        return true;
    }
    public contains(value: T): boolean {
        return this.values.some((current) => HashSet.equals(current, value));
    }
    public clear(): void {
        this.values.length = 0;
    }
    public size(): number {
        return this.values.length;
    }
    public isEmpty(): boolean {
        return this.values.length === 0;
    }
    public *[Symbol.iterator](): IterableIterator<T> {
        yield* this.values.slice();
    }
    protected static equals(a: unknown, b: unknown): boolean {
        if (a === b || (typeof a === "number" && typeof b === "number" && Number.isNaN(a) && Number.isNaN(b)))
            return true;
        return (
            a !== null &&
            typeof a === "object" &&
            "equals" in a &&
            typeof (a as { equals?: unknown }).equals === "function" &&
            (a as { equals(other: unknown): boolean }).equals(b)
        );
    }
}
