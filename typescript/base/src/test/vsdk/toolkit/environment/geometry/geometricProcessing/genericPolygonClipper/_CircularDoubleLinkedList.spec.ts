import { describe, expect, it } from "vitest";
import { _CircularDoubleLinkedList } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/_CircularDoubleLinkedList.js";

describe("_CircularDoubleLinkedList", () => {
    it("keeps node links circular through indexed insertion and removal", () => {
        const list = new _CircularDoubleLinkedList<number>();
        list.add(1);
        list.add(3);
        list.add(1, 2);

        const head = list.getHead()!;
        expect(list.size()).toBe(3);
        expect([head.data, head.next.data, head.next.next.data]).toEqual([1, 2, 3]);
        expect(head.previous.data).toBe(3);
        list.remove(0);
        expect(list.getHead()!.data).toBe(2);
        expect(list.getHead()!.previous.data).toBe(3);
    });

    it("inserts before the current head without breaking ownership", () => {
        const list = new _CircularDoubleLinkedList<string>();
        list.add("b");
        const inserted = list.insertBefore("a", list.getHead()!);

        expect(inserted).not.toBeNull();
        expect(list.getHead()).toBe(inserted);
        expect(list.getHead()!.next.data).toBe("b");
    });
});
