#ifndef __INDEX_OUT_OF_BOUNDS_EXCEPTION__
#define __INDEX_OUT_OF_BOUNDS_EXCEPTION__

#include "java/lang/RuntimeException.h"

/**
Emulation of `java.lang.IndexOutOfBoundsException`.
*/
class IndexOutOfBoundsException : public RuntimeException {
  public:
    IndexOutOfBoundsException() : RuntimeException() {}
    IndexOutOfBoundsException(const String &message) : RuntimeException(message) {}
    virtual ~IndexOutOfBoundsException() {}
};

#endif
