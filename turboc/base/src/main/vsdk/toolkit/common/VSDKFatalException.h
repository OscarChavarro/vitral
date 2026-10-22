#ifndef __VSDK_FATAL_EXCEPTION__
#define __VSDK_FATAL_EXCEPTION__


#include "java/lang/RuntimeException.h"
#include "java/lang/String.h"

class VSDKFatalException : public RuntimeException {
public:
    explicit VSDKFatalException(const String& message) :
        RuntimeException(message) {}

    virtual ~VSDKFatalException() {}
};


#endif
