#ifndef __JAVA_TO_DOUBLE_FUNCTION__
#define __JAVA_TO_DOUBLE_FUNCTION__


/**
Emulation of `java.util.function.ToDoubleFunction`: C++98 has neither
lambdas nor method references, so callbacks are objects implementing this
interface.
*/
template<class T>
class ToDoubleFunction {
  public:
    virtual ~ToDoubleFunction() {}
    virtual double applyAsDouble(T value) = 0;
};


#endif
