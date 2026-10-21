#ifndef __VSDK_FATAL_EXCEPTION__
#define __VSDK_FATAL_EXCEPTION__


#include "java/lang/String.h"
#include <exception>
class VSDKFatalException : public std::exception {
private:
    String message;

public:
    explicit VSDKFatalException(const String& message) : message(message) {}

    virtual ~VSDKFatalException() throw() {}

    const char* what() const throw()
    {
        return message.c_str();
    }
};


#endif
