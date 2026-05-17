#ifndef __VSDK_TOOLKIT_COMMON_VSDK_H__
#define __VSDK_TOOLKIT_COMMON_VSDK_H__


#include <string>

class VSDK {
public:
    static const double EPSILON;

    static std::string formatDouble(double a);
    static std::string formatDouble(double a, int digits);
};


#endif // __VSDK_TOOLKIT_COMMON_VSDK_H__
