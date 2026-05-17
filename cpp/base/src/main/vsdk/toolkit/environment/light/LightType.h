#ifndef __VSDK_TOOLKIT_ENVIRONMENT_LIGHT_LIGHTTYPE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_LIGHT_LIGHTTYPE_H__

class LightType {
public:
    static const int AMBIENT = 0;
    static const int DIRECTIONAL = 1;
    static const int POINT = 2;

private:
    LightType();
};

#endif
