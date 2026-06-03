#ifndef POLYGONCLIPPING_MODEL_POLYGONSURFACETESSELLATIONMODE_H
#define POLYGONCLIPPING_MODEL_POLYGONSURFACETESSELLATIONMODE_H

enum class PolygonSurfaceTessellationMode {
    GLU = 0,
    MONOTONE_DECOMPOSITION = 1
};

inline PolygonSurfaceTessellationMode nextTessellationMode(PolygonSurfaceTessellationMode m)
{
    return (m == PolygonSurfaceTessellationMode::GLU)
        ? PolygonSurfaceTessellationMode::MONOTONE_DECOMPOSITION
        : PolygonSurfaceTessellationMode::GLU;
}

inline const char* tessellationModeDisplayName(PolygonSurfaceTessellationMode m)
{
    return (m == PolygonSurfaceTessellationMode::GLU) ? "GLU" : "MONOTONE_DECOMPOSITION";
}

#endif
