#ifndef __SHADERS_GEOMETRY_FIXTURE__
#define __SHADERS_GEOMETRY_FIXTURE__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "ShadersGeometry.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/processing/SolidBoundaryTessellationReport.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class Geometry;
class SimpleBody;
class SimpleMaterial;
class TriangleMesh;

/**
One non-sphere geometry of the example, built once and shared by both
renderers: the software path ray traces a body that references its
geometries, and the OpenGL path draws the drawable mesh that VITRAL
tessellation derives from that same body. The mesh is an approximation of
declared resolution, never a second definition of the geometry.
*/
class ShadersGeometryFixture {
private:
    ShadersGeometry kind;
    java::ArrayList<Geometry*> geometries;
    GeometryBounds region;
    TriangleMesh* drawableMesh;
    SolidBoundaryTessellationReport report;

    void buildQuartic();
    void buildBlob();
    void buildHeightField();
    void buildCsg();
    void buildPatch();
    ShadersGeometryFixture(const ShadersGeometryFixture&) = delete;
    ShadersGeometryFixture& operator=(const ShadersGeometryFixture&) = delete;
public:
    /// Cells along the longest side of the region of a tessellated solid
    static const int SOLID_RESOLUTION = 96;
    /// Parameter steps of the patch mesh along u and v
    static const int PATCH_STEPS = 48;

    /// PRE: kind is not SPHERE
    explicit ShadersGeometryFixture(ShadersGeometry kind);
    ~ShadersGeometryFixture();
    ShadersGeometry getKind() const;

    /**
    A new body for one frame, referencing the fixture's geometries, owning
    the given material (which may be null) and turned by the rotation.
    */
    SimpleBody* createBody(SimpleMaterial* material, const Matrix4x4d& rotation) const;

    /// Mesh drawn by the OpenGL path, built on first use; owned by the fixture
    TriangleMesh* getDrawableMesh();
    /// Resolution and accuracy of the solid meshes (empty for the patch)
    const SolidBoundaryTessellationReport& getTessellationReport() const;

    static const char* nameOf(ShadersGeometry geometry);
    /// @return false for an unknown name
    static bool parse(const java::String& name, ShadersGeometry& result);
    static ShadersGeometry next(ShadersGeometry geometry);
};

#endif
