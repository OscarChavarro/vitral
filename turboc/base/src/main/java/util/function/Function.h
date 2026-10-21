#ifndef __JAVA_FUNCTION__
#define __JAVA_FUNCTION__


/**
Emulation of `java.util.function.Function`: C++98 has neither lambdas nor
`std::function`, so callbacks are objects implementing this interface.
*/
template<typename T, typename R>
class Function {
  public:
    virtual ~Function() {}
    virtual R apply(T value) = 0;
};


#endif
