#ifndef __POLYGON_SURFACE_TESSELLATION_MODE__
#define __POLYGON_SURFACE_TESSELLATION_MODE__

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
