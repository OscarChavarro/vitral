import { AtomicLong } from "./AtomicLong.js";

/** Java LongAdder semantics; a worker owns one instance, avoiding shared-memory assumptions. */
export class LongAdder {
    private readonly value = new AtomicLong();
    public add(delta: bigint): void {
        this.value.addAndGet(delta);
    }
    public increment(): void {
        this.add(1n);
    }
    public decrement(): void {
        this.add(-1n);
    }
    public sum(): bigint {
        return this.value.get();
    }
    public reset(): void {
        this.value.set(0n);
    }
    public sumThenReset(): bigint {
        return this.value.getAndSet(0n);
    }
    public toString(): string {
        return this.sum().toString();
    }
}
