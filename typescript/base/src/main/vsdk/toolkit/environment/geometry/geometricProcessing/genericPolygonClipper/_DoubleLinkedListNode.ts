/** Node exposed by the circular list used by the Weiler--Atherton clipper. */
export class _DoubleLinkedListNode<E> {
    public data!: E;
    public next!: _DoubleLinkedListNode<E>;
    public previous!: _DoubleLinkedListNode<E>;
    /** Guards against inserting a head belonging to another list. */
    public isHead = false;
}
