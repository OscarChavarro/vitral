#ifndef __POLYGON_SURFACE_TESSELLATION_MODE__
#define __POLYGON_SURFACE_TESSELLATION_MODE__

class PolygonSurfaceTessellationMode {
  public:
    enum Value {
        GLU = 0,
        MONOTONE_DECOMPOSITION = 1
    };
    PolygonSurfaceTessellationMode() : value(GLU) {}
    PolygonSurfaceTessellationMode(Value v) : value(v) {}
    operator Value() const { return value; }
  private:
    Value value;
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
