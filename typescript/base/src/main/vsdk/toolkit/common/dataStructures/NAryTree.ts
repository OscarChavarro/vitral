import { FundamentalEntity } from "../FundamentalEntity.js";
import { VSDK } from "../VSDK.js";
import { Logger } from "../logging/Logger.js";
import { _NAryTreeIntermediateNode } from "./_NAryTreeIntermediateNode.js";
import { _NAryTreeLeafNode } from "./_NAryTreeLeafNode.js";
import { _NAryTreeNode } from "./_NAryTreeNode.js";

/** Java-compatible N-ary tree. Roots are level one and values are expected unique. */
export class NAryTree<T> extends FundamentalEntity {
    private root: _NAryTreeNode<T> | null;
    public constructor(rootContent?: T) {
        super();
        this.root = arguments.length === 0 ? null : new _NAryTreeLeafNode(rootContent as T);
    }
    public getRoot(): _NAryTreeNode<T> | null {
        return this.root;
    }
    public setRoot(root: _NAryTreeNode<T> | null): void {
        this.root = root;
    }
    public searchNodeByContent(key: T): _NAryTreeNode<T> | null {
        return this.root === null ? null : this.search(this.root, key);
    }

    public addChild(existingNodeData: T, newData: T): boolean {
        const parent = this.searchNodeByContent(existingNodeData);
        if (parent === null) return false;
        if (parent instanceof _NAryTreeIntermediateNode) {
            parent.getChildren().push(new _NAryTreeLeafNode(newData));
            return true;
        }
        if (parent instanceof _NAryTreeLeafNode) {
            if (parent === this.root) {
                const node = new _NAryTreeIntermediateNode(parent.getData());
                node.getChildren().push(new _NAryTreeLeafNode(newData));
                this.root = node;
            } else {
                const grandparent = this.searchParent(this.root!, parent);
                if (grandparent === null) {
                    Logger.reportMessage(this, VSDK.FATAL_ERROR, "addChild", "unexpected structure! ");
                    return false;
                }
                grandparent.replaceChild(parent, new _NAryTreeLeafNode(newData));
            }
            // This intentionally retains the Java method's false return for a leaf promotion.
            return false;
        }
        Logger.reportMessage(this, VSDK.FATAL_ERROR, "addChild", "unexpected node subclass");
        return false;
    }

    private search(node: _NAryTreeNode<T>, key: T): _NAryTreeNode<T> | null {
        if (this.dataEquals(node.getData(), key)) return node;
        if (!(node instanceof _NAryTreeIntermediateNode)) return null;
        for (const child of node.getChildren()) {
            const candidate = this.search(child, key);
            if (candidate !== null) return candidate;
        }
        return null;
    }
    private searchParent(node: _NAryTreeNode<T>, key: _NAryTreeNode<T>): _NAryTreeIntermediateNode<T> | null {
        if (!(node instanceof _NAryTreeIntermediateNode)) return null;
        if (node.getChildren().includes(key)) return node;
        for (const child of node.getChildren()) {
            const candidate = this.searchParent(child, key);
            if (candidate !== null) return candidate;
        }
        return null;
    }
    private dataEquals(left: T, right: T): boolean {
        if (left === right) return true;
        if (
            left !== null &&
            typeof left === "object" &&
            "equals" in left &&
            typeof (left as { equals?: unknown }).equals === "function"
        )
            return (left as { equals(value: unknown): boolean }).equals(right);
        return false;
    }
}
