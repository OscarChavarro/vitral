#ifndef __EXECUTION_EXCEPTION__
#define __EXECUTION_EXCEPTION__

#include "java/lang/String.h"
#include "java/lang/System.h"
#include <exception>

class ExecutionException : public std::exception {
private:
    String message_;

public:
    explicit ExecutionException(const String& msg) : message_(msg)
    {
        System::out.println(message_.c_str());
    }

    virtual ~ExecutionException() throw() {}

    const char* what() const throw()
    {
        return message_.c_str();
    }
};


#endif
