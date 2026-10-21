#include "java/util/Collections.h"

template <class T> void
Collections::swap(ArrayList<T> &a, ArrayList<T> &b) {
    // C++98 has no move semantics: the lists are copied
    ArrayList<T> temp(a);
    a = b;
    b = temp;
}

