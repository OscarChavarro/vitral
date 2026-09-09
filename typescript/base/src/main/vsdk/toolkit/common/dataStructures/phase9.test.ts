import { describe, expect, it } from "vitest";
import {
  ArrayListOfBytes, ArrayListOfDoubles, ArrayListOfInts, ArrayListOfLongs,
  BinaryTreeNode, CircularDoubleLinkedList, NAryTree, NAryTreeTraverser,
  _CircularDoubleLinkedListNode, _NAryTreeIntermediateNode, _NAryTreeLeafNode
} from "../../../../index.js";

describe("Phase 9 data-structure contracts", () => {
  it("preserves signed byte storage, capacity exposure, growth and ordering", () => {
    const values = new ArrayListOfBytes(2);
    values.add(255); values.add(-2); values.add(4);
    expect([...values.getRawArray()]).toEqual([-1, -2, 4, 0]);
    values.sort();
    expect([values.get(0), values.get(1), values.get(2)]).toEqual([-2, -1, 4]);
    values.clean(); expect(values.size()).toBe(0);
    expect(() => values.get(-1)).toThrow(RangeError);
  });

  it("preserves int, double and exact long numeric semantics", () => {
    const ints = new ArrayListOfInts(1); [4, -3, 2].forEach((value) => ints.add(value)); ints.set(0, 2 ** 32 + 1); ints.sort();
    expect([ints.get(0), ints.get(1), ints.get(2)]).toEqual([-3, 1, 2]);
    const doubles = new ArrayListOfDoubles(1); [2.5, -1.25, 0].forEach((value) => doubles.add(value)); doubles.sort();
    expect([doubles.get(0), doubles.get(1), doubles.get(2)]).toEqual([-1.25, 0, 2.5]);
    const longs = new ArrayListOfLongs(1); [9_007_199_254_740_993n, -4n, 2n].forEach((value) => longs.add(value)); longs.sort();
    expect([longs.get(0), longs.get(1), longs.get(2)]).toEqual([-4n, 2n, 9_007_199_254_740_993n]);
  });

  it("implements circular-list cursor, insertion, swapping, removal and reverse", () => {
    const list = new CircularDoubleLinkedList<string>();
    list.add("b"); list.add("c"); list.push("a"); list.insertBefore("x", "c");
    expect([list.get(0), list.get(1), list.get(2), list.get(3)]).toEqual(["a", "b", "x", "c"]);
    expect(list.nextOf("c")).toBe("a"); expect(list.previousOf("a")).toBe("c");
    list.swapElements("a", "c"); list.remove(1); list.reverse();
    expect([list.get(0), list.get(1), list.get(2)]).toEqual(["a", "x", "c"]);
  });

  it("exposes circular linked-list nodes", () => {
    const node = new _CircularDoubleLinkedListNode<number>(); node.data = 1; node.next = node; node.previous = node;
    expect(node.next?.data).toBe(1);
  });

  it("promotes leaf nodes, finds contents and preserves child replacement topology", () => {
    const tree = new NAryTree<string>("root");
    expect(tree.addChild("root", "a")).toBe(false);
    expect(tree.addChild("root", "b")).toBe(true);
    expect(tree.addChild("a", "a1")).toBe(false);
    expect(tree.searchNodeByContent("a1")?.getData()).toBe("a1");
    const root = tree.getRoot() as _NAryTreeIntermediateNode<string>;
    expect(root.getChildren()).toHaveLength(2);
    const leaf = new _NAryTreeLeafNode("old"); root.getChildren().push(leaf);
    expect(root.replaceChild(leaf, new _NAryTreeLeafNode("new"))).toBe(true);
    expect((root.getChildren()[2] as _NAryTreeIntermediateNode<string>).getChildren()[0]?.getData()).toBe("new");
  });

  it("retains binary node links and traverser formatting", () => {
    const parent = new BinaryTreeNode("parent"); const child = new BinaryTreeNode("child"); const sibling = new BinaryTreeNode("sibling");
    parent.setChild(child); child.setSibling(sibling);
    expect(parent.getChild()?.getSibling()?.getData()).toBe("sibling");
    class Traverser extends NAryTreeTraverser { public start(): void {} public end(): void {} public visit(): void {} public header(level: number): string { return this.formatHeader(level); } }
    const traverser = new Traverser();
    expect([traverser.header(1), traverser.header(2), traverser.header(3), traverser.header(5)]).toEqual(["", "  - ", "      . ", "          "]);
  });
});
