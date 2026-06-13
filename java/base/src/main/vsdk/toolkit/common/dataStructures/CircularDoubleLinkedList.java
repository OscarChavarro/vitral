package vsdk.toolkit.common.dataStructures;

import java.io.Serial;
import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

public class CircularDoubleLinkedList<E> extends FundamentalEntity
{
    @Serial private static final long serialVersionUID = 20070422L;

    private _CircularDoubleLinkedListNode<E> head;
    private _CircularDoubleLinkedListNode<E> window;
    private int currentSize;

    // Sequential-access memo for get(int): the dominant access pattern in the
    // CSG kernel is `for (i = 0; i < list.size(); i++) list.get(i)`, which on a
    // plain head-relative scan is O(n^2) per loop. Caching the last accessed
    // (index, node) lets an ascending walk continue from where it left off,
    // making such loops O(n) amortized. Reset on any structural change. Marked
    // transient so it never participates in serialization/equality.
    private transient int accessMemoIndex;
    private transient _CircularDoubleLinkedListNode<E> accessMemoNode;

    public CircularDoubleLinkedList()
    {
        head = null;
        window = null;
        currentSize = 0;
        invalidateAccessMemo();
    }

    private void invalidateAccessMemo()
    {
        accessMemoIndex = -1;
        accessMemoNode = null;
    }

    public int size()
    {
        return currentSize;
    }

    public void add(E e)
    {
        _CircularDoubleLinkedListNode<E> newContainer;
        newContainer = new _CircularDoubleLinkedListNode<E>();
        newContainer.data = e;
        if ( head == null ) {
            head = newContainer;
            newContainer.next = newContainer;
            newContainer.previous = newContainer;
        }
        else {
            newContainer.previous = head.previous;
            newContainer.next = head;
            head.previous.next = newContainer;
            head.previous = newContainer;
        }
        currentSize++;
        invalidateAccessMemo();
    }

    public void insertBefore(E newElem, E pivot)
    {
        locateWindowAtElem(pivot);
        _CircularDoubleLinkedListNode<E> newContainer;

        newContainer = new _CircularDoubleLinkedListNode<E>();
        newContainer.data = newElem;

        if ( head == null ) {
            head = newContainer;
            newContainer.next = newContainer;
            newContainer.previous = newContainer;
        }
        else if ( window == null || window == head ) {
            window = head;
            head = newContainer;
            newContainer.previous = window.previous;
            newContainer.next = window;
            window.previous.next = newContainer;
            window.previous = newContainer;
        }
        else {
            newContainer.previous = window.previous;
            newContainer.next = window;
            window.previous.next = newContainer;
            window.previous = newContainer;
        }
        currentSize++;
        invalidateAccessMemo();
    }

    public void locateWindowAtIndex(int index)
    {
        if ( index < 0 || index >= currentSize ) {
            return;
        }
        int i;
        for ( i = 0, window = head;
              i < currentSize && i < index;
              i++, window = window.next ) {
            // Note this updates the window pointer
	}
    }

    public boolean locateWindowAtElem(E e)
    {
        int i;

        for ( i = 0, window = head;
              i < currentSize;
              i++, window = window.next ) {
            if ( window.data == e ) {
                return true;
            }
        }
        window = null;
        return false;
    }

    public void swapElements(E e1, E e2)
    {
        locateWindowAtElem(e1);
        _CircularDoubleLinkedListNode<E> window1 = window;
        locateWindowAtElem(e2);
        _CircularDoubleLinkedListNode<E> window2 = window;

        if ( window1 == null || window2 == null ) return;
        E temp = window1.data;

        window1.data = window2.data;
        window2.data = temp;
        invalidateAccessMemo();
    }

    public E next()
    {
        if ( window == null ) {
            window = head;
        }
        E elem = window.data;
        window = window.next;
        return elem;
    }

    public E getWindow()
    {
        if ( head == null ) return null;
        if ( window == null ) {
            window = head;
        }
        return window.data;
    }

    public E previous()
    {
        if ( window == null ) {
            window = head;
        }
        E elem = window.data;
        window = window.previous;
        return elem;
    }

    public E nextOf(E e)
    {
        _CircularDoubleLinkedListNode<E> current;
        int i;

        for ( i = 0, current = head;
              i < currentSize; i++, current = current.next ) {
            if ( current.data == e ) {
                return current.next.data;
            }
        }
        return null;
    }

    public E previousOf(E e)
    {
        _CircularDoubleLinkedListNode<E> current;
        int i;

        for ( i = 0, current = head;
              i < currentSize; i++, current = current.next ) {
            if ( current.data == e ) {
                return current.previous.data;
            }
        }
        return null;
    }

    public E get(int index)
    {
        _CircularDoubleLinkedListNode<E> current;

        if ( index < 0 || index >= currentSize ) {
            // Report index out of bounds exception!
            String msg;
            msg = "IndexOutOfBounds Exception! - Trying to `get` with index " + index + " in a list with " + currentSize + " elements.";

            Logger.reportMessage(this, VSDK.FATAL_ERROR, "get", msg);
            return null;
        }
        int i;
        // Start from the access memo when it is at or before the requested
        // index, so ascending get(i) walks continue forward instead of
        // restarting from head; otherwise scan from head.
        if ( accessMemoNode != null && accessMemoIndex >= 0 &&
             accessMemoIndex <= index ) {
            i = accessMemoIndex;
            current = accessMemoNode;
        }
        else {
            i = 0;
            current = head;
        }
        for ( ; i < currentSize && i < index; i++, current = current.next ) {
            // Local traversal keeps indexed reads independent from window.
	}
        accessMemoIndex = index;
        accessMemoNode = current;
        return current.data;
    }

    public void remove(int pos)
    {
        locateWindowAtIndex(pos);
        removeElemAtWindow();
    }

    public void removeElemAtWindow()
    {
        if ( window == null ) return;
        if ( window == head ) head = window.next;
        window.previous.next = window.next;
        window.next.previous = window.previous;
        window = null;
        currentSize--;
        invalidateAccessMemo();
    }

    public void push(E newElem)
    {
        window = head;
        _CircularDoubleLinkedListNode<E> newContainer;

        newContainer = new _CircularDoubleLinkedListNode<E>();
        newContainer.data = newElem;

        if ( head == null ) {
            head = newContainer;
            newContainer.next = newContainer;
            newContainer.previous = newContainer;
        }
        else if ( window == null || window == head ) {
            window = head;
            head = newContainer;
            newContainer.previous = window.previous;
            newContainer.next = window;
            window.previous.next = newContainer;
            window.previous = newContainer;
        }
        else {
            newContainer.previous = window.previous;
            newContainer.next = window;
            window.previous.next = newContainer;
            window.previous = newContainer;
        }
        currentSize++;
        invalidateAccessMemo();
    }

    public void reverse()
    {
        _CircularDoubleLinkedListNode<E> ptr, qtr;
        E tmp;
        int i = 0;

        ptr = head;
        qtr = head.previous;
        do {
            tmp = ptr.data;
            ptr.data = qtr.data;
            qtr.data = tmp;

            ptr = ptr.next;
            qtr = qtr.previous;
            i++;
        } while ( ptr != head && i < currentSize/2 );
        invalidateAccessMemo();
    }

}
