import { describe, expect, it } from "vitest";
import { ArrayListOfLongs } from "vsdk/toolkit/common/dataStructures/ArrayListOfLongs.js";
describe("ArrayListOfLongs", () =>
    it("sorts exact bigint values", () => {
        const v = new ArrayListOfLongs(1);
        [9007199254740993n, -4n, 2n].forEach((x) => v.add(x));
        v.sort();
        expect([v.get(0), v.get(1), v.get(2)]).toEqual([-4n, 2n, 9007199254740993n]);
    }));
