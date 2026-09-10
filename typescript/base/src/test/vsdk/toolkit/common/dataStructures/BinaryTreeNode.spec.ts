import { describe, expect, it } from "vitest";
import { BinaryTreeNode } from "vsdk/toolkit/common/dataStructures/BinaryTreeNode.js";
import { NAryTreeTraverser } from "vsdk/toolkit/common/dataStructures/NAryTreeTraverser.js";
describe("BinaryTreeNode", () =>
    it("keeps child and sibling links", () => {
        const parent = new BinaryTreeNode("parent"),
            child = new BinaryTreeNode("child"),
            sibling = new BinaryTreeNode("sibling");
        parent.setChild(child);
        child.setSibling(sibling);
        expect(parent.getChild()?.getSibling()?.getData()).toBe("sibling");
    }));
describe("NAryTreeTraverser", () =>
    it("formats traversal headers", () => {
        class T extends NAryTreeTraverser {
            public start() {}
            public end() {}
            public visit() {}
            public header(level: number) {
                return this.formatHeader(level);
            }
        }
        const t = new T();
        expect([t.header(1), t.header(2), t.header(3), t.header(5)]).toEqual(["", "  - ", "      . ", "          "]);
    }));
