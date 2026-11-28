import { describe, expect, it } from "vitest";
import { NullPointerException } from "java/lang/NullPointerException.js";
import { XmlImporter } from "vsdk/toolkit/io/xml/XmlImporter.js";

describe("XmlImporter", () => {
    it("preserves source byte offsets, non-tag spans and the closed flag", () => {
        const nodes = new XmlImporter().importFromLine("  <item label='niño' flag=\"ok\" /><next>text</next>tail");

        expect(nodes.toArray().map((node) => [node.toString(), node.getSourceStart(), node.getSourceEnd()])).toEqual([
            ["_NOTAG_: content=  ", 0, 1],
            ["item: label=niño, flag=ok", 2, 33],
            ["next", 34, 39],
            ["_NOTAG_: content=text", 40, 43],
            ["/next", 44, 50],
        ]);
        expect(nodes.get(0).isEmpty()).toBe(true);
        expect(nodes.get(1).isClosed()).toBe(true);
        expect(nodes.get(1).value("label")).toBe("niño");
    });

    it("keeps a partially read tag and rejects an empty tag like Java", () => {
        const importer = new XmlImporter();
        const node = importer.importFromLine("<a x='é'").get(0);

        expect(node.toString()).toBe("a: x=é");
        expect(node.getSourceEnd()).toBe(8);
        expect(() => importer.importFromLine("<>")).toThrow(NullPointerException);
    });
});
