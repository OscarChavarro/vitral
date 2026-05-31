#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_POLYGONCLIPPER_DOUBLELINKEDLISTNODE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_POLYGONCLIPPER_DOUBLELINKEDLISTNODE_H__

template <class E>
class _DoubleLinkedListNode {
public:
    E data;
    _DoubleLinkedListNode<E>* next;
    _DoubleLinkedListNode<E>* previous;
    bool isHead;

    _DoubleLinkedListNode() : next(0), previous(0), isHead(false) {}
};

#endif
