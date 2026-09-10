import { IllegalStateException } from "../lang/IllegalStateException.js";

export interface MapEntry<K, V> {
    readonly key: K;
    value: V;
}

/** Java Map semantics with Java-style equals dispatch for object keys. */
export class HashMap<K, V> implements Iterable<MapEntry<K, V>> {
    private readonly entriesArray: MapEntry<K, V>[] = [];
    private modificationCount = 0;

    public constructor(initial?: Iterable<readonly [K, V]>) {
        if (initial !== undefined) for (const [key, value] of initial) this.put(key, value);
    }
    public size(): number {
        return this.entriesArray.length;
    }
    public isEmpty(): boolean {
        return this.entriesArray.length === 0;
    }
    public containsKey(key: K): boolean {
        return this.findIndex(key) >= 0;
    }
    public containsValue(value: V): boolean {
        return this.entriesArray.some((entry) => HashMap.javaEquals(entry.value, value));
    }
    public get(key: K): V | undefined {
        const entry = this.entriesArray[this.findIndex(key)];
        return entry?.value;
    }
    public getOrDefault(key: K, defaultValue: V): V {
        const index = this.findIndex(key);
        return index < 0 ? defaultValue : (this.entriesArray[index] as MapEntry<K, V>).value;
    }
    public tryGet(key: K, valueOut?: { value: V }): boolean {
        const index = this.findIndex(key);
        if (index < 0) return false;
        if (valueOut !== undefined) valueOut.value = (this.entriesArray[index] as MapEntry<K, V>).value;
        return true;
    }

    /** Java Map.put: returns the prior value, or undefined for Java null/absent. */
    public put(key: K, value: V): V | undefined {
        const index = this.findIndex(key);
        if (index >= 0) {
            const entry = this.entriesArray[index] as MapEntry<K, V>;
            const previous = entry.value;
            entry.value = value;
            return previous;
        }
        this.entriesArray.push({ key, value });
        this.modificationCount++;
        return undefined;
    }

    public putAll(entries: Iterable<readonly [K, V]>): void {
        for (const [key, value] of entries) this.put(key, value);
    }
    /** Java Map.remove: returns the prior value, or undefined for Java null/absent. */
    public remove(key: K): V | undefined {
        const index = this.findIndex(key);
        if (index < 0) return undefined;
        const value = (this.entriesArray.splice(index, 1)[0] as MapEntry<K, V>).value;
        this.modificationCount++;
        return value;
    }
    public clear(): void {
        if (this.entriesArray.length > 0) {
            this.entriesArray.length = 0;
            this.modificationCount++;
        }
    }
    public keySet(): K[] {
        return this.entriesArray.map((entry) => entry.key);
    }
    public values(): V[] {
        return this.entriesArray.map((entry) => entry.value);
    }
    public entrySet(): MapEntry<K, V>[] {
        return this.entriesArray.map((entry) => ({ key: entry.key, value: entry.value }));
    }

    public *[Symbol.iterator](): IterableIterator<MapEntry<K, V>> {
        const expectedModificationCount = this.modificationCount;
        for (const entry of this.entriesArray) {
            if (this.modificationCount !== expectedModificationCount)
                throw new IllegalStateException("Map modified during iteration");
            yield entry;
        }
    }

    private findIndex(key: K): number {
        return this.entriesArray.findIndex((entry) => HashMap.javaEquals(entry.key, key));
    }
    private static javaEquals(left: unknown, right: unknown): boolean {
        if (
            left === right ||
            (typeof left === "number" && typeof right === "number" && Number.isNaN(left) && Number.isNaN(right))
        )
            return true;
        if (
            left !== null &&
            typeof left === "object" &&
            "equals" in left &&
            typeof (left as { equals?: unknown }).equals === "function"
        )
            return (left as { equals(other: unknown): boolean }).equals(right);
        return false;
    }
}
