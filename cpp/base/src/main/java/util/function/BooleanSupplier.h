#ifndef __JAVA_BOOLEAN_SUPPLIER__
#define __JAVA_BOOLEAN_SUPPLIER__

#include "java/util/function/_LambdaHolder.h"

namespace java {

/**
Emulation of `java.util.function.BooleanSupplier`: a function without arguments returning a boolean.
*/
class BooleanSupplier : public _Lambda<bool> {
  public:
    BooleanSupplier() : _Lambda<bool>() {}
    BooleanSupplier(decltype(nullptr)) : _Lambda<bool>() {}
    BooleanSupplier(const BooleanSupplier &other) : _Lambda<bool>(static_cast<const _Lambda<bool> &>(other)) {}

    template <class F>
    BooleanSupplier(const F &function) : _Lambda<bool>(function) {}

    BooleanSupplier &
    operator=(const BooleanSupplier &other)
    {
        _Lambda<bool>::operator=(other);
        return *this;
    }

    bool
    getAsBoolean() const
    {
        return this->invoke();
    }
};

}

#endif
