import { describe, expect, it } from "vitest";
import { ArrayListOfBytes } from "vsdk/toolkit/common/dataStructures/ArrayListOfBytes.js";
describe("ArrayListOfBytes", () =>
    it("preserves signed storage and ordering", () => {
        const v = new ArrayListOfBytes(2);
        v.add(255);
        v.add(-2);
        v.add(4);
        expect([...v.getRawArray()]).toEqual([-1, -2, 4, 0]);
        v.sort();
        expect([v.get(0), v.get(1), v.get(2)]).toEqual([-2, -1, 4]);
        v.clean();
        expect(v.size()).toBe(0);
    }));
