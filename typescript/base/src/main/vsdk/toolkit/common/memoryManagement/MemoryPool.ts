import { ArrayList } from "../../../../java/util/ArrayList.js";

/** Keeps reusable objects and returns borrowed instances in LIFO order. */
export class MemoryPool<T> {
    private readonly available = new ArrayList<T>();
    private readonly borrowedStack = new ArrayList<T>();
    private initialized = false;

    public constructor(private readonly supplier: (() => T) | null) {}

    public init(sizeInBytes: bigint | number): void {
        if (this.initialized || sizeInBytes <= 0 || this.supplier === null) {
            return;
        }
        this.initialized = true;
    }

    public allocate(numberOfElements: number): T | null {
        if (!this.initialized || numberOfElements <= 0 || this.available.isEmpty()) {
            return null;
        }
        const item = this.available.remove(this.available.size() - 1);
        this.borrowedStack.add(item);
        return item;
    }

    public free(numberOfElements: number): void {
        if (!this.initialized || numberOfElements <= 0 || this.borrowedStack.isEmpty()) {
            return;
        }
        const item = this.borrowedStack.remove(this.borrowedStack.size() - 1);
        this.available.add(item);
    }

    public clear(): void {
        if (!this.initialized) {
            return;
        }
        while (!this.borrowedStack.isEmpty()) {
            this.available.add(this.borrowedStack.remove(this.borrowedStack.size() - 1));
        }
    }

    public expand(numberOfElements: number): boolean {
        if (numberOfElements <= 0 || this.supplier === null) {
            return numberOfElements <= 0;
        }
        this.initialized = true;
        for (let i = 0; i < numberOfElements; i++) {
            this.available.add(this.supplier());
        }
        return true;
    }
}
