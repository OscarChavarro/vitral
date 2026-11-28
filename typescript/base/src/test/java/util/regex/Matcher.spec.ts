import { describe, expect, it } from "vitest";
import { IllegalStateException } from "java/lang/IllegalStateException.js";
import { Pattern } from "java/util/regex/Pattern.js";

describe("Matcher", () => {
    it("reports capture counts and UTF-16 offsets, including absent groups", () => {
        const matcher = Pattern.compile("([a-z]+)([0-9]+)?").matcher("!abc de12");
        expect(matcher.groupCount()).toBe(2);
        expect(() => matcher.group()).toThrow(IllegalStateException);
        expect(matcher.find()).toBe(true);
        expect([matcher.group(1), matcher.start(1), matcher.end(1)]).toEqual(["abc", 1, 4]);
        expect([matcher.group(2), matcher.start(2), matcher.end(2)]).toEqual([null, -1, -1]);
        expect(matcher.find(5)).toBe(true);
        expect([matcher.group(2), matcher.start(2), matcher.end(2)]).toEqual(["12", 7, 9]);
        expect(matcher.find()).toBe(false);
        expect(matcher.reset("xy12").matches()).toBe(true);
        expect(matcher.find()).toBe(false);
    });

    it("requires the full input even for a multiline pattern", () => {
        const matcher = Pattern.compile("^abc$", Pattern.MULTILINE).matcher("abc\nxyz");
        expect(matcher.matches()).toBe(false);
        expect(matcher.find()).toBe(true);
        expect(matcher.group()).toBe("abc");
    });
});
