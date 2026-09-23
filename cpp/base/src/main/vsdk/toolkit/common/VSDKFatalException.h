#ifndef __VSDK_FATAL_EXCEPTION__
#define __VSDK_FATAL_EXCEPTION__


#include "java/lang/String.h"
#include <exception>
class VSDKFatalException : public std::exception {
private:
    java::String message;

public:
    explicit VSDKFatalException(const java::String& message) : message(message) {}

    const char* what() const noexcept override
    {
        return message.c_str();
    }
};


#endif
