#ifndef __JAVA_BOOLEAN_SUPPLIER__
#define __JAVA_BOOLEAN_SUPPLIER__

#include "vsdk/toolkit/common/VSDK.h"

/**
Emulation of `java.util.function.BooleanSupplier`: C++98 has neither
lambdas nor method references, so callbacks are objects implementing this
interface.
*/
class BooleanSupplier {
  public:
    virtual ~BooleanSupplier() {}
    virtual BOOLEAN getAsBoolean() = 0;
};


#endif
