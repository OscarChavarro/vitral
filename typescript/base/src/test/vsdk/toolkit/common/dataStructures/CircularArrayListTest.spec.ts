import { describe, expect, it } from "vitest";

import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { IndexOutOfBoundsException } from "java/lang/IndexOutOfBoundsException.js";
import type { ArrayLinkable } from "vsdk/toolkit/common/dataStructures/ArrayLinkable.js";
import { CircularArrayList } from "vsdk/toolkit/common/dataStructures/CircularArrayList.js";

/** Java's private nested `Item implements ArrayLinkable`. */
class Item implements ArrayLinkable {
    private listOwner: object | null = null;
    private listIndex = -1;

    public getListOwner(): object | null {
        return this.listOwner;
    }

    public getListIndex(): number {
        return this.listIndex;
    }

    public setListSlot(owner: object | null, index: number): void {
        this.listOwner = owner;
        this.listIndex = index;
    }
}

/**
Direct coverage for the array-backed circular list that backs every face,
loop, edge, vertex and half-edge collection of PolyhedralBoundedSolid. The
first cases are the ones of the linked list it replaced, unchanged: the
order of elements and the preconditions are the same.

<p>Traceability: this container replaces the intrusive nxt/prv pointers of
[MANT1988] Ch. 10.2-10.3 and the list procedures of Ch. 11.2 (addlist,
dellist). The cases marked "hidden defect" were found by the Fable 5.2
kernel analysis (doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md,
defects L-01 to L-03).</p>
 */
describe("CircularArrayListTest", () => {
    function abc(): CircularArrayList<string> {
        const list = new CircularArrayList<string>();
        list.add("a");
        list.add("b");
        list.add("c");
        return list;
    }

    function assertHandles(list: CircularArrayList<Item>): void {
        for (let i = 0; i < list.size(); i++) {
            const item = list.get(i)!;
            expect(item.getListOwner()).toBe(list);
            expect(item.getListIndex()).toBe(i);
            expect(list.indexOf(item)).toBe(i);
            expect(list.nextOf(item)).toBe(list.get((i + 1) % list.size()));
        }
    }

    it("given_threeElements_when_traversed_then_orderAndNeighborsAreCircular", () => {
        const list = abc();

        expect(list.size()).toBe(3);
        expect(list.get(0)).toBe("a");
        expect(list.get(2)).toBe("c");
        expect(list.nextOf("c")).toBe("a");
        expect(list.previousOf("a")).toBe("c");
    });

    it("given_threeElements_when_insertBeforeHead_then_newElementBecomesHead", () => {
        const list = abc();

        list.insertBefore("x", "a");

        expect(list.size()).toBe(4);
        expect(list.get(0)).toBe("x");
        expect(list.nextOf("x")).toBe("a");
        expect(list.previousOf("x")).toBe("c");
    });

    it("given_threeElements_when_reverse_then_orderIsMirrored", () => {
        const list = abc();

        list.reverse();

        expect(list.get(0)).toBe("c");
        expect(list.get(1)).toBe("b");
        expect(list.get(2)).toBe("a");
    });

    /**
    Defect L-01 (fixed): removeElemAtWindow() leaves `head` pointing at the
    removed node when the list becomes empty, so the next add() links the new
    element behind a stale node and get(0) resurrects the removed element.
    This is exactly the sequence lkev() performs when it kills the last edge
    of a loop (unlist both half-edges, then re-add the surviving one).
    */
    it("given_listEmptiedByRemove_when_add_then_onlyTheNewElementIsVisible", () => {
        const list = new CircularArrayList<string>();
        list.add("a");
        list.remove(0);

        list.add("b");

        expect(list.size()).toBe(1);
        expect(list.get(0)).toBe("b");
        expect(list.nextOf("b")).toBe("b");
        expect(list.previousOf("b")).toBe("b");
    });

    /**
    Defect L-01 (fixed, continued): after the stale-head corruption the ring
    holds a phantom node, so neighbor queries disagree with indexed access.
    */
    it("given_listEmptiedByRemove_when_addAndInsertBefore_then_neighborsMatchIndexOrder", () => {
        const list = new CircularArrayList<string>();
        list.add("a");
        list.remove(0);
        list.add("b");

        list.insertBefore("c", "b");

        expect(list.size()).toBe(2);
        expect(list.get(0)).toBe("c");
        expect(list.get(1)).toBe("b");
        expect(list.nextOf("c")).toBe("b");
        expect(list.nextOf("b")).toBe("c");
    });

    /**
    Defect L-02 (fixed): remove(index) with an out-of-range index used to
    delete whatever element the window was left on by an unrelated earlier
    call. It is now a hard error that leaves the list unchanged.
    */
    it("given_windowLeftOnAnElement_when_removeWithInvalidIndex_then_errorAndNothingIsRemoved", () => {
        const list = abc();
        list.locateWindowAtElem("b");

        expect(() => list.remove(7)).toThrow(IndexOutOfBoundsException);

        expect(list.size()).toBe(3);
        expect(list.get(1)).toBe("b");
    });

    /**
    Defect L-03 (fixed): insertBefore(newElem, pivot) with a pivot that is
    not in the list used to insert at the head silently. It is now a hard
    error that leaves the list unchanged. Euler operators rely on this call (addhe) to place new
    half-edges, so a wrong pivot corrupts a loop without any diagnostic.
    */
    it("given_missingPivot_when_insertBefore_then_errorAndListIsNotModified", () => {
        const list = abc();

        expect(() => list.insertBefore("x", "not-in-list")).toThrow(IllegalArgumentException);

        expect(list.size()).toBe(3);
        expect(list.get(0)).toBe("a");
    });

    it("given_linkableElements_when_reorderedAndRemoved_then_neighborsStayConsistent", () => {
        const list = new CircularArrayList<Item>();
        const a = new Item();
        const b = new Item();
        const c = new Item();
        list.add(a);
        list.add(b);
        list.add(c);

        list.reverse();
        expect(list.nextOf(c)).toBe(b);
        expect(list.previousOf(c)).toBe(a);

        list.swapElements(a, c);
        expect(list.get(0)).toBe(a);
        expect(list.nextOf(a)).toBe(b);

        list.locateWindowAtElem(b);
        list.removeElemAtWindow();
        expect(list.contains(b)).toBe(false);
        expect(b.getListOwner()).toBeNull();
        expect(list.nextOf(a)).toBe(c);
        expect(list.nextOf(c)).toBe(a);

        const other = new CircularArrayList<Item>();
        other.add(a);
        expect(list.nextOf(a), "falls back to a scan").toBe(c);
        expect(other.nextOf(a)).toBe(a);
    });

    /**
    Reading does not write: two index loops interleaved over the same list
    (the pattern of every "all pairs" pass of the kernel) see every pair,
    and several threads reading at once see the same elements.

    <p>Runtime boundary: Java runs the eight readers on a fixed thread pool.
    This runtime is single-threaded, so the eight readers run one after the
    other over the same shared list; the property checked (reads never
    disturb the order) is the same.</p>
    */
    it("given_sharedList_when_readFromSeveralThreads_then_everyReaderSeesTheSameOrder", () => {
        const list = new CircularArrayList<Item>();
        const items: Item[] = [];
        for (let i = 0; i < 2000; i++) {
            items.push(new Item());
            list.add(items[i]!);
        }
        const results: number[] = [];
        for (let t = 0; t < 8; t++) {
            const stride = t + 1;
            let mismatches = 0;
            for (let round = 0; round < 50; round++) {
                for (let i = 0; i < items.length; i += stride) {
                    if (list.get(i) !== items[i]) mismatches++;
                    if (list.nextOf(items[i]!) !== items[(i + 1) % items.length]) mismatches++;
                    if (list.get(items.length - 1 - i) !== items[items.length - 1 - i]) mismatches++;
                }
            }
            results.push(mismatches);
        }
        for (const r of results) {
            expect(r).toBe(0);
        }
    });

    it("given_linkableElements_when_everyKindOfEdit_then_handlesMatchIndexes", () => {
        const list = new CircularArrayList<Item>();
        const items: Item[] = [];
        for (let i = 0; i < 40; i++) {
            items.push(new Item());
        }
        for (let i = 0; i < 20; i++) list.add(items[i]!);
        assertHandles(list);
        for (let i = 20; i < 30; i++) list.insertBefore(items[i]!, items[i - 20]!);
        assertHandles(list);
        for (let i = 30; i < 40; i++) list.push(items[i]!);
        assertHandles(list);
        list.remove(0);
        list.remove(list.size() - 1);
        list.remove(7);
        assertHandles(list);
        list.locateWindowAtElem(items[5]!);
        list.removeElemAtWindow();
        expect(items[5]!.getListOwner()).toBeNull();
        assertHandles(list);
        list.swapElements(items[1]!, items[25]!);
        list.reverse();
        assertHandles(list);
        list.clear();
        expect(list.size()).toBe(0);
        for (const item of items) {
            expect(item.getListOwner()).toBeNull();
        }
    });

    it("given_cursor_when_iteratedBothWays_then_itWrapsAround", () => {
        const list = abc();

        expect(list.next()).toBe("a");
        expect(list.next()).toBe("b");
        expect(list.next()).toBe("c");
        expect(list.next()).toBe("a");
        list.locateWindowAtIndex(0);
        expect(list.previous()).toBe("a");
        expect(list.previous()).toBe("c");
        expect(list.getWindow()).toBe("b");
        list.push("z");
        expect(list.get(0)).toBe("z");
        expect(list.getWindow()).toBe("a");
    });
});
