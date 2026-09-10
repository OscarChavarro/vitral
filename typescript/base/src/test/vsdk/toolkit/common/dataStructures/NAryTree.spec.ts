import { describe, expect, it } from "vitest";
import { NAryTree } from "vsdk/toolkit/common/dataStructures/NAryTree.js";
import { _NAryTreeIntermediateNode } from "vsdk/toolkit/common/dataStructures/_NAryTreeIntermediateNode.js";
import { _NAryTreeLeafNode } from "vsdk/toolkit/common/dataStructures/_NAryTreeLeafNode.js";
describe("NAryTree", () =>
    it("replaces children", () => {
        const t = new NAryTree<string>("root");
        t.addChild("root", "a");
        t.addChild("root", "b");
        const r = t.getRoot() as _NAryTreeIntermediateNode<string>,
            leaf = new _NAryTreeLeafNode("old");
        r.getChildren().push(leaf);
        expect(r.replaceChild(leaf, new _NAryTreeLeafNode("new"))).toBe(true);
        expect((r.getChildren()[2] as _NAryTreeIntermediateNode<string>).getChildren()[0]?.getData()).toBe("new");
    }));
