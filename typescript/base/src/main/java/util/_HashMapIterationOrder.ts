/**
The order in which a `java.util.HashMap` that only received `put` calls
iterates its entries, for callers whose Java code walks a plain `HashMap` and
whose keys have a deterministic `hashCode()`.

The package's `HashMap` iterates in insertion order. A JVM iterates its table
instead: bins in index order and, inside a bin, nodes in the order they were
appended. This class keeps the shape of that table while the keys arrive:
capacity sixteen on the first put, doubled when the size exceeds three
quarters of it, the spread hash `h ^ (h >>> 16)`, bins appended at their tail,
and the order-preserving low/high split on resize. A bin reaching nine nodes
in a table smaller than sixty-four makes the JDK resize instead of building a
tree, which is reproduced too. A bin that would become a red-black tree (nine
nodes in a table of at least sixty-four) is reported by `hasTreeBin`: the JDK
then orders that bin by comparisons and, for keys that are not `Comparable`,
by identity hashes, which no port can reproduce.

Only new keys are recorded: a `put` on a key already present does not change
the table, and `remove` is not supported.
*/
export class _HashMapIterationOrder {
    private static readonly TREEIFY_THRESHOLD = 8;
    private static readonly MIN_TREEIFY_CAPACITY = 64;

    private table: number[][] | null = null;
    private size = 0;
    private threshold = 0;
    private readonly hashes: number[] = [];
    private treeBin = false;

    /**
    Records the insertion of a key that was not in the map.
    @param hash the key's `hashCode()`
    @return the insertion index of the key (0 for the first key, and so on)
    */
    public add(hash: number): number {
        const index = this.hashes.length;
        const spread = (hash ^ (hash >>> 16)) | 0;
        this.hashes.push(spread);
        if (this.table === null) this.resize();
        const bin = this.table![spread & (this.table!.length - 1)]!;
        const nodesBefore = bin.length;
        bin.push(index);
        if (nodesBefore >= _HashMapIterationOrder.TREEIFY_THRESHOLD) {
            if (this.table!.length < _HashMapIterationOrder.MIN_TREEIFY_CAPACITY) this.resize();
            else this.treeBin = true;
        }
        if (++this.size > this.threshold) this.resize();
        return index;
    }

    /** @return the insertion indexes in the order the JDK iterates them */
    public order(): number[] {
        const result: number[] = [];
        if (this.table === null) return result;
        for (const bin of this.table) for (const index of bin) result.push(index);
        return result;
    }

    /** @return true if a bin of the JDK table would have become a tree */
    public hasTreeBin(): boolean {
        return this.treeBin;
    }

    private resize(): void {
        const oldTable = this.table;
        const oldCapacity = oldTable === null ? 0 : oldTable.length;
        const newCapacity = oldCapacity === 0 ? 16 : oldCapacity * 2;
        const newTable: number[][] = [];
        for (let i = 0; i < newCapacity; i++) newTable.push([]);
        if (oldTable !== null) {
            for (let j = 0; j < oldCapacity; j++) {
                for (const index of oldTable[j]!) {
                    const high = (this.hashes[index]! & oldCapacity) !== 0;
                    newTable[high ? j + oldCapacity : j]!.push(index);
                }
            }
        }
        this.table = newTable;
        this.threshold = Math.trunc(newCapacity * 0.75);
    }
}
