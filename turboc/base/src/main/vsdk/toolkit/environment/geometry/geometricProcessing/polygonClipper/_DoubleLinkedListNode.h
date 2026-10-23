#ifndef __DOUBLE_LINKED_LIST_NODE__
#define __DOUBLE_LINKED_LIST_NODE__

#include "vsdk/toolkit/common/VSDK.h"

template <class E>
class _DoubleLinkedListNode {
public:
    E data;
    _DoubleLinkedListNode<E>* next;
    _DoubleLinkedListNode<E>* previous;
    BOOLEAN isHead;

    _DoubleLinkedListNode() : next(0), previous(0), isHead(FALSE) {}
};

#endif
