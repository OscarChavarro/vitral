#ifndef __XML_EXCEPTION__
#define __XML_EXCEPTION__

#include "vsdk/toolkit/common/VSDKException.h"

class XmlException : public VSDKException {
public:
    XmlException() : VSDKException() {}
    explicit XmlException(const String& message) : VSDKException(message) {}
};

#endif
