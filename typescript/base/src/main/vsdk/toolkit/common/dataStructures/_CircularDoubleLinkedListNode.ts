import { FundamentalEntity } from "../FundamentalEntity.js";

/** Link node used by CircularDoubleLinkedList. */
export class _CircularDoubleLinkedListNode<E> extends FundamentalEntity {
  public data: E | null = null;
  public next: _CircularDoubleLinkedListNode<E> | null = null;
  public previous: _CircularDoubleLinkedListNode<E> | null = null;
}
