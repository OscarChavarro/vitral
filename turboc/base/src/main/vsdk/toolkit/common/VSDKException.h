#ifndef __VSDK_EXCEPTION__
#define __VSDK_EXCEPTION__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Exception.h"
#include "java/lang/String.h"

/**
The VSDKException abstract class provides an interface for *Exception
style classes inside the Vitral SDK. This serves two purposes:
  - To help in design level organization of exceptions (this eases the
    study of the class hierarchy)
  - To provide a place to locate possible future operations, common to
    all exceptions (but none of these as been detected yet)
*/
class VSDKException : public Exception {
protected:
    VSDKException() : Exception() {}
    explicit VSDKException(const String& message) : Exception(message) {}

public:
    virtual ~VSDKException() {}
};

#endif
