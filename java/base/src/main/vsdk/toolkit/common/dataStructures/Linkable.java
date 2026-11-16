package vsdk.toolkit.common.dataStructures;

/**
Optional element-side handle for {@link CircularDoubleLinkedList}: an element
that implements it remembers the node that holds it, so neighbor queries
(`nextOf`, `previousOf`) and `locateWindowAtElem` run in O(1) instead of
scanning the list. This is how the half-edges of a loop recover the
intrusive `nxt`/`prv` links of [MANT1988] Program 10.1 while keeping the
container API used throughout the kernel.

<p>The list keeps the handle up to date on every structural change. When an
element is inserted in a second list, the handle follows the newest node and
the first list falls back to a scan, so correctness never depends on it.</p>
*/
public interface Linkable<E>
{
    _CircularDoubleLinkedListNode<E> getListNode();

    void setListNode(_CircularDoubleLinkedListNode<E> node);
}
