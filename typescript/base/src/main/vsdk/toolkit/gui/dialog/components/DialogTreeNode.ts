import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Collections } from "../../../../../java/util/Collections.js";

/** A hierarchy row with identity separate from its display label. */
export class DialogTreeNode {
    private readonly label: string;
    private readonly identity: unknown;
    private readonly children: ArrayList<DialogTreeNode> = new ArrayList<DialogTreeNode>();

    public constructor(label: string, identity: unknown) {
        this.label = label;
        this.identity = identity;
    }
    public getLabel(): string {
        return this.label;
    }
    public getIdentity(): unknown {
        return this.identity;
    }
    public getChildren(): ArrayList<DialogTreeNode> {
        return Collections.unmodifiableList(this.children);
    }
    public add(child: DialogTreeNode): void {
        this.children.add(child);
    }
    public toString(): string {
        return this.label;
    }
}
