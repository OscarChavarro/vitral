import type { MapEntry } from "./HashMap.js";

/**
One bin node of the emulated `java.util.HashMap` table. A plain node only uses
`next`; a node of a treeified bin also carries the red-black tree links and the
`prev` link a `HashMap.TreeNode` keeps.
*/
interface _BinNode<V> {
    readonly hash: number;
    readonly key: number;
    value: V;
    next: _BinNode<V> | null;
    tree: boolean;
    parent: _BinNode<V> | null;
    left: _BinNode<V> | null;
    right: _BinNode<V> | null;
    prev: _BinNode<V> | null;
    red: boolean;
}

/**
`java.util.HashMap<Integer, V>`, with the iteration order a JVM produces.

The package's general `HashMap` iterates in insertion order, which is what every
caller so far has needed: either the Java code only looked values up, or it
iterated a `LinkedHashMap`. Some callers do observe the order of a plain
`HashMap` keyed by `Integer` — `_StepSolidBuilder` numbers the vertices of a
solid read from a STEP file, and orders its edges, by walking such maps — and
for an `Integer` key that order is fully determined: `Integer.hashCode()` is
the value itself, so the order is a function of the keys and of the order they
were inserted in, never of identity hashes.

This class reproduces it by keeping the same table the JDK keeps: capacity
sixteen growing by doubling past a load factor of three quarters, the spread
hash `h ^ (h >>> 16)`, bins appended at their tail, the order-preserving
low/high split on resize, and — for a bin that reaches nine nodes in a table of
at least sixty-four — the red-black tree bins of `HashMap.TreeNode`, whose
`treeify`, `putTreeVal`, `split`, `untreeify` and `moveRootToFront` each change
the order a bin is iterated in. Lookups go through a native `Map`, which is
where they cost nothing; the table exists only to answer iteration.

`remove` is not offered: no caller removes from such a map, and a partial
emulation of it would be the only part of this class not checked against the
JDK's.
*/
export class IntegerKeyHashMap<V> implements Iterable<MapEntry<number, V>> {
    private static readonly DEFAULT_INITIAL_CAPACITY = 16;
    private static readonly LOAD_FACTOR = 0.75;
    private static readonly TREEIFY_THRESHOLD = 8;
    private static readonly UNTREEIFY_THRESHOLD = 6;
    private static readonly MIN_TREEIFY_CAPACITY = 64;

    private table: (_BinNode<V> | null)[] | null = null;
    private threshold = 0;
    private readonly lookup = new Map<number, _BinNode<V>>();

    public size(): number {
        return this.lookup.size;
    }

    public isEmpty(): boolean {
        return this.lookup.size === 0;
    }

    public containsKey(key: number): boolean {
        return this.lookup.has(key);
    }

    public get(key: number): V | undefined {
        return this.lookup.get(key)?.value;
    }

    /** Java `Map.put`: answers the prior value, or undefined when absent. */
    public put(key: number, value: V): V | undefined {
        const existing = this.lookup.get(key);
        if (existing !== undefined) {
            const previous = existing.value;
            existing.value = value;
            return previous;
        }

        const hash: number = IntegerKeyHashMap.spread(key);
        if (this.table === null || this.table.length === 0) {
            this.resize();
        }
        const tab = this.table!;
        const n: number = tab.length;
        const i: number = (n - 1) & hash;
        const first = tab[i] ?? null;
        let node: _BinNode<V>;

        if (first === null) {
            node = IntegerKeyHashMap.newNode(hash, key, value, null);
            tab[i] = node;
        } else if (first.tree) {
            node = this.putTreeVal(tab, first, hash, key, value);
        } else {
            let p: _BinNode<V> = first;
            let binCount = 0;
            for (;;) {
                const e = p.next;
                if (e === null) {
                    node = IntegerKeyHashMap.newNode(hash, key, value, null);
                    p.next = node;
                    if (binCount >= IntegerKeyHashMap.TREEIFY_THRESHOLD - 1) {
                        this.treeifyBin(tab, hash);
                    }
                    break;
                }
                p = e;
                binCount++;
            }
        }

        this.lookup.set(key, node);
        if (this.lookup.size > this.threshold) {
            this.resize();
        }
        return undefined;
    }

    /** The keys, in the order `keySet()` iterates them on a JVM. */
    public keySet(): number[] {
        const keys: number[] = [];
        this.forEachNode((node) => keys.push(node.key));
        return keys;
    }

    /** The values, in the order `values()` iterates them on a JVM. */
    public values(): V[] {
        const values: V[] = [];
        this.forEachNode((node) => values.push(node.value));
        return values;
    }

    /** The entries, in the order `entrySet()` iterates them on a JVM. */
    public entrySet(): MapEntry<number, V>[] {
        const entries: MapEntry<number, V>[] = [];
        this.forEachNode((node) => entries.push({ key: node.key, value: node.value }));
        return entries;
    }

    public [Symbol.iterator](): IterableIterator<MapEntry<number, V>> {
        return this.entrySet()[Symbol.iterator]();
    }

    //=================================================================

    private forEachNode(visit: (node: _BinNode<V>) => void): void {
        if (this.table === null) {
            return;
        }
        for (const bin of this.table) {
            for (let e = bin ?? null; e !== null; e = e.next) {
                visit(e);
            }
        }
    }

    /** `HashMap.hash(Object)` for an `Integer`, whose hash code is itself. */
    private static spread(key: number): number {
        const h: number = key | 0;
        return h ^ (h >>> 16);
    }

    private static newNode<V>(hash: number, key: number, value: V, next: _BinNode<V> | null): _BinNode<V> {
        return { hash, key, value, next, tree: false, parent: null, left: null, right: null, prev: null, red: false };
    }

    /**
    `TreeNode` ordering for two `Integer` keys: by spread hash, then by
    `Integer.compareTo`. Distinct keys never tie, so `tieBreakOrder` is never
    reached.
    */
    private static compareForTree<V>(h: number, k: number, p: _BinNode<V>): number {
        if (p.hash > h) {
            return -1;
        }
        if (p.hash < h) {
            return 1;
        }
        return k < p.key ? -1 : k > p.key ? 1 : 0;
    }

    private resize(): void {
        const oldTab = this.table;
        const oldCap: number = oldTab === null ? 0 : oldTab.length;
        const newCap: number = oldCap > 0 ? oldCap << 1 : IntegerKeyHashMap.DEFAULT_INITIAL_CAPACITY;
        this.threshold = newCap * IntegerKeyHashMap.LOAD_FACTOR;
        const newTab: (_BinNode<V> | null)[] = new Array<_BinNode<V> | null>(newCap).fill(null);
        this.table = newTab;
        if (oldTab === null) {
            return;
        }

        for (let j = 0; j < oldCap; j++) {
            const e = oldTab[j] ?? null;
            if (e === null) {
                continue;
            }
            oldTab[j] = null;
            if (e.next === null) {
                newTab[e.hash & (newCap - 1)] = e;
            } else if (e.tree) {
                this.split(newTab, e, j, oldCap);
            } else {
                let loHead: _BinNode<V> | null = null;
                let loTail: _BinNode<V> | null = null;
                let hiHead: _BinNode<V> | null = null;
                let hiTail: _BinNode<V> | null = null;
                let cursor: _BinNode<V> | null = e;
                while (cursor !== null) {
                    const next: _BinNode<V> | null = cursor.next;
                    if ((cursor.hash & oldCap) === 0) {
                        if (loTail === null) loHead = cursor;
                        else loTail.next = cursor;
                        loTail = cursor;
                    } else {
                        if (hiTail === null) hiHead = cursor;
                        else hiTail.next = cursor;
                        hiTail = cursor;
                    }
                    cursor = next;
                }
                if (loTail !== null) {
                    loTail.next = null;
                    newTab[j] = loHead;
                }
                if (hiTail !== null) {
                    hiTail.next = null;
                    newTab[j + oldCap] = hiHead;
                }
            }
        }
    }

    private treeifyBin(tab: (_BinNode<V> | null)[], hash: number): void {
        const n: number = tab.length;
        if (n < IntegerKeyHashMap.MIN_TREEIFY_CAPACITY) {
            this.resize();
            return;
        }
        const index: number = (n - 1) & hash;
        const hd = tab[index] ?? null;
        let tl: _BinNode<V> | null = null;
        for (let e = hd; e !== null; e = e.next) {
            e.tree = true;
            e.prev = tl;
            tl = e;
        }
        if (hd !== null) {
            IntegerKeyHashMap.treeify(tab, hd);
        }
    }

    private static treeify<V>(tab: (_BinNode<V> | null)[], head: _BinNode<V>): void {
        let root: _BinNode<V> | null = null;
        let next: _BinNode<V> | null;
        for (let x: _BinNode<V> | null = head; x !== null; x = next) {
            next = x.next;
            x.left = null;
            x.right = null;
            if (root === null) {
                x.parent = null;
                x.red = false;
                root = x;
                continue;
            }
            for (let p: _BinNode<V> = root; ;) {
                const dir: number = IntegerKeyHashMap.compareForTree(x.hash, x.key, p);
                const xp: _BinNode<V> = p;
                const child = dir <= 0 ? p.left : p.right;
                if (child === null) {
                    x.parent = xp;
                    if (dir <= 0) xp.left = x;
                    else xp.right = x;
                    root = IntegerKeyHashMap.balanceInsertion(root, x);
                    break;
                }
                p = child;
            }
        }
        IntegerKeyHashMap.moveRootToFront(tab, root);
    }

    private static untreeify<V>(head: _BinNode<V>): _BinNode<V> {
        for (let e: _BinNode<V> | null = head; e !== null; e = e.next) {
            e.tree = false;
            e.parent = null;
            e.left = null;
            e.right = null;
            e.prev = null;
            e.red = false;
        }
        return head;
    }

    private static root<V>(node: _BinNode<V>): _BinNode<V> {
        let r: _BinNode<V> = node;
        while (r.parent !== null) {
            r = r.parent;
        }
        return r;
    }

    private putTreeVal(tab: (_BinNode<V> | null)[], first: _BinNode<V>, h: number, k: number, v: V): _BinNode<V> {
        const root: _BinNode<V> = first.parent !== null ? IntegerKeyHashMap.root(first) : first;
        for (let p: _BinNode<V> = root; ;) {
            const dir: number = IntegerKeyHashMap.compareForTree(h, k, p);
            const xp: _BinNode<V> = p;
            const child = dir <= 0 ? p.left : p.right;
            if (child === null) {
                const xpn: _BinNode<V> | null = xp.next;
                const x: _BinNode<V> = IntegerKeyHashMap.newNode(h, k, v, xpn);
                x.tree = true;
                if (dir <= 0) xp.left = x;
                else xp.right = x;
                xp.next = x;
                x.parent = xp;
                x.prev = xp;
                if (xpn !== null) {
                    xpn.prev = x;
                }
                IntegerKeyHashMap.moveRootToFront(tab, IntegerKeyHashMap.balanceInsertion(root, x));
                return x;
            }
            p = child;
        }
    }

    private split(tab: (_BinNode<V> | null)[], head: _BinNode<V>, index: number, bit: number): void {
        let loHead: _BinNode<V> | null = null;
        let loTail: _BinNode<V> | null = null;
        let hiHead: _BinNode<V> | null = null;
        let hiTail: _BinNode<V> | null = null;
        let lc = 0;
        let hc = 0;
        let next: _BinNode<V> | null;
        for (let e: _BinNode<V> | null = head; e !== null; e = next) {
            next = e.next;
            e.next = null;
            if ((e.hash & bit) === 0) {
                e.prev = loTail;
                if (loTail === null) loHead = e;
                else loTail.next = e;
                loTail = e;
                lc++;
            } else {
                e.prev = hiTail;
                if (hiTail === null) hiHead = e;
                else hiTail.next = e;
                hiTail = e;
                hc++;
            }
        }

        if (loHead !== null) {
            if (lc <= IntegerKeyHashMap.UNTREEIFY_THRESHOLD) {
                tab[index] = IntegerKeyHashMap.untreeify(loHead);
            } else {
                tab[index] = loHead;
                if (hiHead !== null) {
                    IntegerKeyHashMap.treeify(tab, loHead);
                }
            }
        }
        if (hiHead !== null) {
            if (hc <= IntegerKeyHashMap.UNTREEIFY_THRESHOLD) {
                tab[index + bit] = IntegerKeyHashMap.untreeify(hiHead);
            } else {
                tab[index + bit] = hiHead;
                if (loHead !== null) {
                    IntegerKeyHashMap.treeify(tab, hiHead);
                }
            }
        }
    }

    private static moveRootToFront<V>(tab: (_BinNode<V> | null)[], root: _BinNode<V> | null): void {
        if (root === null) {
            return;
        }
        const index: number = (tab.length - 1) & root.hash;
        const first = tab[index] ?? null;
        if (root === first) {
            return;
        }
        tab[index] = root;
        const rn: _BinNode<V> | null = root.next;
        const rp: _BinNode<V> | null = root.prev;
        if (rn !== null) {
            rn.prev = rp;
        }
        if (rp !== null) {
            rp.next = rn;
        }
        if (first !== null) {
            first.prev = root;
        }
        root.next = first;
        root.prev = null;
    }

    private static rotateLeft<V>(root: _BinNode<V>, p: _BinNode<V> | null): _BinNode<V> {
        let r: _BinNode<V> | null;
        if (p !== null && (r = p.right) !== null) {
            const rl: _BinNode<V> | null = r.left;
            p.right = rl;
            if (rl !== null) {
                rl.parent = p;
            }
            const pp: _BinNode<V> | null = p.parent;
            r.parent = pp;
            if (pp === null) {
                root = r;
                r.red = false;
            } else if (pp.left === p) {
                pp.left = r;
            } else {
                pp.right = r;
            }
            r.left = p;
            p.parent = r;
        }
        return root;
    }

    private static rotateRight<V>(root: _BinNode<V>, p: _BinNode<V> | null): _BinNode<V> {
        let l: _BinNode<V> | null;
        if (p !== null && (l = p.left) !== null) {
            const lr: _BinNode<V> | null = l.right;
            p.left = lr;
            if (lr !== null) {
                lr.parent = p;
            }
            const pp: _BinNode<V> | null = p.parent;
            l.parent = pp;
            if (pp === null) {
                root = l;
                l.red = false;
            } else if (pp.right === p) {
                pp.right = l;
            } else {
                pp.left = l;
            }
            l.right = p;
            p.parent = l;
        }
        return root;
    }

    private static balanceInsertion<V>(root: _BinNode<V>, x: _BinNode<V>): _BinNode<V> {
        x.red = true;
        for (;;) {
            let xp: _BinNode<V> | null = x.parent;
            let xpp: _BinNode<V> | null;
            if (xp === null) {
                x.red = false;
                return x;
            }
            if (!xp.red || (xpp = xp.parent) === null) {
                return root;
            }
            const xppl: _BinNode<V> | null = xpp.left;
            if (xp === xppl) {
                const xppr: _BinNode<V> | null = xpp.right;
                if (xppr !== null && xppr.red) {
                    xppr.red = false;
                    xp.red = false;
                    xpp.red = true;
                    x = xpp;
                } else {
                    if (x === xp.right) {
                        x = xp;
                        root = IntegerKeyHashMap.rotateLeft(root, x);
                        xp = x.parent;
                        xpp = xp === null ? null : xp.parent;
                    }
                    if (xp !== null) {
                        xp.red = false;
                        if (xpp !== null) {
                            xpp.red = true;
                            root = IntegerKeyHashMap.rotateRight(root, xpp);
                        }
                    }
                }
            } else {
                if (xppl !== null && xppl.red) {
                    xppl.red = false;
                    xp.red = false;
                    xpp.red = true;
                    x = xpp;
                } else {
                    if (x === xp.left) {
                        x = xp;
                        root = IntegerKeyHashMap.rotateRight(root, x);
                        xp = x.parent;
                        xpp = xp === null ? null : xp.parent;
                    }
                    if (xp !== null) {
                        xp.red = false;
                        if (xpp !== null) {
                            xpp.red = true;
                            root = IntegerKeyHashMap.rotateLeft(root, xpp);
                        }
                    }
                }
            }
        }
    }
}
