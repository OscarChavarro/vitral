#ifndef __VSDK_TOOLKIT_COMMON_VSDKFATALEXCEPTION_H__
#define __VSDK_TOOLKIT_COMMON_VSDKFATALEXCEPTION_H__


#include <stdexcept>
#include "java/lang/String.h"
#include "java/lang/String.h"
#include "java/lang/String.h"

class VSDKFatalException : public std::runtime_error {
public:
    explicit VSDKFatalException(const java::String& message) : std::runtime_error(message) {}
};


#endif // __VSDK_TOOLKIT_COMMON_VSDKFATALEXCEPTION_H__
