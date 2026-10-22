#ifndef __JAVA_SUPPLIER__
#define __JAVA_SUPPLIER__


/**
Emulation of `java.util.function.Supplier`: C++98 has neither lambdas nor
callbacks are objects implementing this interface.
*/
template<class T>
class Supplier {
  public:
    virtual ~Supplier() {}
    virtual T get() = 0;
};


#endif
