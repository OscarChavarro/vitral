import { describe, expect, it } from "vitest";
import { CircularDoubleLinkedList } from "vsdk/toolkit/common/dataStructures/CircularDoubleLinkedList.js";
import { _CircularDoubleLinkedListNode } from "vsdk/toolkit/common/dataStructures/_CircularDoubleLinkedListNode.js";
describe("CircularDoubleLinkedList", () =>
    it("mutates circular order", () => {
        const l = new CircularDoubleLinkedList<string>();
        l.add("b");
        l.add("c");
        l.push("a");
        l.insertBefore("x", "c");
        expect([l.get(0), l.get(1), l.get(2), l.get(3)]).toEqual(["a", "b", "x", "c"]);
        l.swapElements("a", "c");
        l.remove(1);
        l.reverse();
        expect([l.get(0), l.get(1), l.get(2)]).toEqual(["a", "x", "c"]);
    }));
describe("_CircularDoubleLinkedListNode", () =>
    it("links itself", () => {
        const n = new _CircularDoubleLinkedListNode<number>();
        n.data = 1;
        n.next = n;
        n.previous = n;
        expect(n.next?.data).toBe(1);
    }));
