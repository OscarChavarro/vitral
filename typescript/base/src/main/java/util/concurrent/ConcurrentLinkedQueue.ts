import { IndexOutOfBoundsException } from "../../lang/IndexOutOfBoundsException.js";

/** FIFO queue with an API that can be backed by a worker message boundary. */
export class ConcurrentLinkedQueue<T> implements Iterable<T> {
    private readonly values: T[] = [];
    public constructor(source?: Iterable<T>) {
        if (source !== undefined) for (const value of source) this.offer(value);
    }
    public offer(value: T): boolean {
        this.values.push(value);
        return true;
    }
    public add(value: T): boolean {
        return this.offer(value);
    }
    public poll(): T | undefined {
        return this.values.shift();
    }
    public peek(): T | undefined {
        return this.values[0];
    }
    public remove(): T {
        const value = this.poll();
        if (value === undefined) throw new IndexOutOfBoundsException("Queue is empty");
        return value;
    }
    public element(): T {
        const value = this.peek();
        if (value === undefined) throw new IndexOutOfBoundsException("Queue is empty");
        return value;
    }
    public isEmpty(): boolean {
        return this.values.length === 0;
    }
    public size(): number {
        return this.values.length;
    }
    public clear(): void {
        this.values.length = 0;
    }
    public *[Symbol.iterator](): IterableIterator<T> {
        yield* this.values.slice();
    }
}
