#ifndef __IMG_T_SLD_TXTR_INTRP_TYPS__
#define __IMG_T_SLD_TXTR_INTRP_TYPS__

enum ImageToSolidTextureInterpolationTypes {
    NO_INTERPOLATION = 0,
    NEAREST_NEIGHBOR = 1,
    BI_LINEAR = 2,
    CUBIC_SPLINE = 3,
    NORMALIZED_DIST = 4
};

#endif
