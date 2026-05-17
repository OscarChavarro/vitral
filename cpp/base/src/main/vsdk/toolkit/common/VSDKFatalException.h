#ifndef __VSDK_TOOLKIT_COMMON_VSDKFATALEXCEPTION_H__
#define __VSDK_TOOLKIT_COMMON_VSDKFATALEXCEPTION_H__


#include <stdexcept>
#include <string>

class VSDKFatalException : public std::runtime_error {
public:
    explicit VSDKFatalException(const std::string& message) : std::runtime_error(message) {}
};


#endif // __VSDK_TOOLKIT_COMMON_VSDKFATALEXCEPTION_H__
