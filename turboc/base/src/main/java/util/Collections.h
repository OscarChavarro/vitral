#ifndef __COLLECTIONS__
#define __COLLECTIONS__

#include "java/util/ArrayList.h"

// Mirrors the role of JDK's java.util.Collections: static utility
// operations on a container that don't belong on the container class
// itself. The JDK's swap(List<?>, int, int) exchanges two elements of one
// list; since this codebase has no List, this swap instead exchanges the
// full contents of two ArrayLists (a 3-word pointer/field rotation via the
// already-existing move constructor/assignment, no allocation, no
// per-element copy).
class Collections {
  public:
    template <class T>
    static void swap(ArrayList<T> &a, ArrayList<T> &b);
};


#include "java/util/Collections.txx"

#endif
