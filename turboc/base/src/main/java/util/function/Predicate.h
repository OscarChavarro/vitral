#ifndef __JAVA_PREDICATE__
#define __JAVA_PREDICATE__

#include "vsdk/toolkit/common/VSDK.h"


/**
Emulation of `java.util.function.Predicate`: C++98 has neither lambdas nor
conditions are objects implementing this interface.
*/
template<class T>
class Predicate {
  public:
    virtual ~Predicate() {}
    virtual BOOLEAN test(T value) = 0;
};


#endif
