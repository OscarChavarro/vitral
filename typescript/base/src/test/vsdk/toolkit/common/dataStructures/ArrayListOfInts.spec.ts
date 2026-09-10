import { describe, expect, it } from "vitest";
import { ArrayListOfInts } from "vsdk/toolkit/common/dataStructures/ArrayListOfInts.js";
describe("ArrayListOfInts", () =>
    it("keeps integer semantics", () => {
        const v = new ArrayListOfInts(1);
        [4, -3, 2].forEach((x) => v.add(x));
        v.set(0, 2 ** 32 + 1);
        v.sort();
        expect([v.get(0), v.get(1), v.get(2)]).toEqual([-3, 1, 2]);
    }));
