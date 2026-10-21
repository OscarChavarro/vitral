#ifndef __JAVA_SUPPLIER__
#define __JAVA_SUPPLIER__


/**
Emulation of `java.util.function.Supplier`: C++98 has neither lambdas nor
`std::function`, so callbacks are objects implementing this interface.
*/
template<typename T>
class Supplier {
  public:
    virtual ~Supplier() {}
    virtual T get() = 0;
};


#endif
