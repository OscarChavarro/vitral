#ifndef __LIGHTTYPE__
#define __LIGHTTYPE__

class LightType {
public:
    static const int AMBIENT = 0;
    static const int DIRECTIONAL = 1;
    static const int POINT = 2;

private:
    LightType();
};

#endif
