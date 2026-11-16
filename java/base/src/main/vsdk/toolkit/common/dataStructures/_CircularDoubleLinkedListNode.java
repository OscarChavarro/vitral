package vsdk.toolkit.common.dataStructures;

import java.io.Serial;
import vsdk.toolkit.common.FundamentalEntity;

public class _CircularDoubleLinkedListNode<E> extends FundamentalEntity
{
    @Serial private static final long serialVersionUID = 20070422L;

    public E data;
    public _CircularDoubleLinkedListNode<E> next;
    public _CircularDoubleLinkedListNode<E> previous;

    /// List that currently holds this node, or null once it was removed.
    /// Lets an element that keeps a handle to its node (see
    /// `CircularDoubleLinkedList.Linkable`) find its neighbors in O(1).
    public CircularDoubleLinkedList<E> owner;
}
