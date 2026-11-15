#ifndef __ALGBRC_EXPRSSN_EXCPTN__
#define __ALGBRC_EXPRSSN_EXCPTN__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/VSDKException.h"

class AlgebraicExpressionException : public VSDKException {
public:
    AlgebraicExpressionException() : VSDKException() {}
    explicit AlgebraicExpressionException(const String& message)
        : VSDKException(message) {}
};

#endif
