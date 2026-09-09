import { Entity } from "../Entity.js";

/** Base composite node for an N-ary tree. */
export abstract class _NAryTreeNode<T> extends Entity {
  private data: T;
  public constructor(inInfo: T) { super(); this.data = inInfo; }
  public getData(): T { return this.data; }
  public setData(data: T): void { this.data = data; }
}
