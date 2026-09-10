import { describe, expect, it } from "vitest";
import { ArrayListOfDoubles } from "vsdk/toolkit/common/dataStructures/ArrayListOfDoubles.js";
describe("ArrayListOfDoubles", () =>
    it("sorts floating values", () => {
        const v = new ArrayListOfDoubles(1);
        [2.5, -1.25, 0].forEach((x) => v.add(x));
        v.sort();
        expect([v.get(0), v.get(1), v.get(2)]).toEqual([-1.25, 0, 2.5]);
    }));
