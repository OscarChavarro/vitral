#ifndef __JAVA_CONSUMER__
#define __JAVA_CONSUMER__


/**
Emulation of `java.util.function.Consumer`: C++98 has neither lambdas nor
`std::function`, so callbacks are objects implementing this interface.
*/
template<typename T>
class Consumer {
  public:
    virtual ~Consumer() {}
    virtual void accept(T value) = 0;
};


#endif
