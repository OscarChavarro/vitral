#ifndef __EXECUTION_EXCEPTION__
#define __EXECUTION_EXCEPTION__

#include "java/lang/Exception.h"
#include "java/lang/String.h"
#include "java/lang/System.h"

class ExecutionException : public Exception {
public:
    explicit ExecutionException(const String& msg) : Exception(msg)
    {
        System::out.println(msg.c_str());
    }
    virtual ~ExecutionException() {}
};

#endif
