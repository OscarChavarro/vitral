import { _NAryTreeNode } from "./_NAryTreeNode.js";

export class _NAryTreeIntermediateNode<T> extends _NAryTreeNode<T> {
    private readonly children: _NAryTreeNode<T>[] = [];
    public getChildren(): _NAryTreeNode<T>[] {
        return this.children;
    }
    public replaceChild(oldNode: _NAryTreeNode<T>, newNode: _NAryTreeNode<T>): boolean {
        const index = this.children.indexOf(oldNode);
        if (index < 0) return false;
        const subtree = new _NAryTreeIntermediateNode(oldNode.getData());
        subtree.getChildren().push(newNode);
        this.children[index] = subtree;
        return true;
    }
}
