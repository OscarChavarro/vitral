import { Double } from "../lang/Double.js";
import { ArrayList } from "./ArrayList.js";
import type { Comparator } from "./Comparator.js";
import { TimSort } from "./TimSort.js";

/**
Subset of `java.util.Collections` needed by ported algorithms whose output
depends on Java's exact sorting and reversal semantics.
*/
export class Collections {
    private constructor() {}

    /**
    `Collections.sort(List)` and `Collections.sort(List, Comparator)`: a
    stable sort with Java's TimSort comparison sequence. Without a
    comparator, elements are compared by their natural ordering: numbers as
    `Double.compareTo`, strings as `String.compareTo`, and other objects by
    their `compareTo` method.
    */
    public static sort<T>(list: T[] | ArrayList<T>, c?: Comparator<T> | null): void {
        const compare =
            c === undefined || c === null
                ? (left: T, right: T): number => Collections.naturalCompare(left, right)
                : (left: T, right: T): number => c.compare(left, right);
        if (list instanceof ArrayList) {
            const elements = list.toArray();
            TimSort.sort(elements, 0, elements.length, compare);
            for (let i = 0; i < elements.length; i++) {
                list.set(i, elements[i]!);
            }
            return;
        }
        TimSort.sort(list, 0, list.length, compare);
    }

    /** `Collections.reverse(List)`. */
    public static reverse<T>(list: T[] | ArrayList<T>): void {
        const size = list instanceof ArrayList ? list.size() : list.length;
        for (let i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) {
            if (list instanceof ArrayList) {
                list.set(i, list.set(j, list.get(i)));
            } else {
                const tmp = list[i]!;
                list[i] = list[j]!;
                list[j] = tmp;
            }
        }
    }

    private static naturalCompare<T>(left: T, right: T): number {
        if (typeof left === "number" && typeof right === "number") {
            return Double.compare(left, right);
        }
        if (typeof left === "string" && typeof right === "string") {
            const n = Math.min(left.length, right.length);
            for (let i = 0; i < n; i++) {
                const d = left.charCodeAt(i) - right.charCodeAt(i);
                if (d !== 0) {
                    return d;
                }
            }
            return left.length - right.length;
        }
        return (left as unknown as { compareTo(other: T): number }).compareTo(right);
    }
}
