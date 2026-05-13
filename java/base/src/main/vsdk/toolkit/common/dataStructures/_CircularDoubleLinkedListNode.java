package vsdk.toolkit.common.dataStructures;

import java.io.Serial;
import vsdk.toolkit.common.FundamentalEntity;

public class _CircularDoubleLinkedListNode<E> extends FundamentalEntity
{
    @Serial private static final long serialVersionUID = 20070422L;

    public E data;
    public _CircularDoubleLinkedListNode<E> next;
    public _CircularDoubleLinkedListNode<E> previous;
}
