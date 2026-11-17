#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include "java/util/TreeSet.txx"
#include <cstdio>
#include <typeinfo>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/QuadMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/ImplicitSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl4/curve/OpenGL4ParametricCurveRenderer.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4FunctionalExplicitSurfaceRenderer.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4InfinitePlaneRenderer.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4ParametricBiCubicPatchRenderer.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4QuadMeshRenderer.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4SubdividedBezierPatchRenderer.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4TriangleMeshGroupRenderer.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4TriangleMeshRenderer.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4TriangleStripMeshRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4ArrowRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4BoxRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4ConeRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4HeightFieldSolidRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4ImplicitSolidRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4PlanarHalfSpaceRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4QuadricSurfaceRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4SphereRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4TorusRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4VolumeRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4VoxelVolumeRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidRenderer.h"

namespace {

const long MAX_CACHED_MESHES = 256;

java::TreeMap<java::String, OpenGL4MeshRenderer::Mesh*> meshes;
java::TreeSet<java::String> reportedUnsupported;

void releaseMeshes()
{
    for ( long i = 0; i < meshes.size(); i++ ) {
        OpenGL4MeshRenderer::release(meshes.getValue(i));
        delete meshes.getValue(i);
    }
    meshes.clear();
}

/**
@return a string that identifies the mesh of the geometry (given by the
`meshKey` of its renderer), or an empty one (Java's null) if the geometry is
not tessellated to a mesh
*/
java::String keyOf(Geometry* geometry)
{
    if ( Cone* c = dynamic_cast<Cone*>(geometry) ) {
        return OpenGL4ConeRenderer::meshKey(*c);
    }
    if ( Arrow* a = dynamic_cast<Arrow*>(geometry) ) {
        return OpenGL4ArrowRenderer::meshKey(*a);
    }
    if ( Box* b = dynamic_cast<Box*>(geometry) ) {
        return OpenGL4BoxRenderer::meshKey(*b);
    }
    if ( Torus* t = dynamic_cast<Torus*>(geometry) ) {
        return OpenGL4TorusRenderer::meshKey(*t);
    }
    if ( TriangleMesh* m = dynamic_cast<TriangleMesh*>(geometry) ) {
        return OpenGL4TriangleMeshRenderer::meshKey(*m);
    }
    if ( QuadMesh* m = dynamic_cast<QuadMesh*>(geometry) ) {
        return OpenGL4QuadMeshRenderer::meshKey(*m);
    }
    if ( TriangleStripMesh* m = dynamic_cast<TriangleStripMesh*>(geometry) ) {
        return OpenGL4TriangleStripMeshRenderer::meshKey(*m);
    }
    if ( FunctionalExplicitSurface* f = dynamic_cast<FunctionalExplicitSurface*>(geometry) ) {
        return OpenGL4FunctionalExplicitSurfaceRenderer::meshKey(*f);
    }
    if ( ParametricBiCubicPatch* p = dynamic_cast<ParametricBiCubicPatch*>(geometry) ) {
        return OpenGL4ParametricBiCubicPatchRenderer::meshKey(*p);
    }
    if ( SubdividedBezierPatch* p = dynamic_cast<SubdividedBezierPatch*>(geometry) ) {
        return OpenGL4SubdividedBezierPatchRenderer::meshKey(*p);
    }
    if ( InfinitePlane* p = dynamic_cast<InfinitePlane*>(geometry) ) {
        return OpenGL4InfinitePlaneRenderer::meshKey(*p);
    }
    if ( VoxelVolume* v = dynamic_cast<VoxelVolume*>(geometry) ) {
        return OpenGL4VoxelVolumeRenderer::meshKey(*v);
    }
    if ( QuadricSurface* q = dynamic_cast<QuadricSurface*>(geometry) ) {
        return OpenGL4QuadricSurfaceRenderer::meshKey(*q);
    }
    if ( ImplicitSolid* s = dynamic_cast<ImplicitSolid*>(geometry) ) {
        return OpenGL4ImplicitSolidRenderer::meshKey(*s);
    }
    if ( HeightFieldSolid* h = dynamic_cast<HeightFieldSolid*>(geometry) ) {
        return OpenGL4HeightFieldSolidRenderer::meshKey(*h);
    }
    return java::String();
}

/**
@return the mesh of the geometry, built by its renderer, or null if it
can not be tessellated
*/
OpenGL4MeshRenderer::Mesh* buildMesh(Geometry* geometry)
{
    if ( Cone* c = dynamic_cast<Cone*>(geometry) ) {
        return OpenGL4ConeRenderer::buildMesh(*c);
    }
    if ( Arrow* a = dynamic_cast<Arrow*>(geometry) ) {
        return OpenGL4ArrowRenderer::buildMesh(*a);
    }
    if ( Box* b = dynamic_cast<Box*>(geometry) ) {
        return OpenGL4BoxRenderer::buildMesh(*b);
    }
    if ( Torus* t = dynamic_cast<Torus*>(geometry) ) {
        return OpenGL4TorusRenderer::buildMesh(*t);
    }
    if ( TriangleMesh* m = dynamic_cast<TriangleMesh*>(geometry) ) {
        return OpenGL4TriangleMeshRenderer::buildMesh(*m);
    }
    if ( QuadMesh* m = dynamic_cast<QuadMesh*>(geometry) ) {
        return OpenGL4QuadMeshRenderer::buildMesh(*m);
    }
    if ( TriangleStripMesh* m = dynamic_cast<TriangleStripMesh*>(geometry) ) {
        return OpenGL4TriangleStripMeshRenderer::buildMesh(*m);
    }
    if ( FunctionalExplicitSurface* f = dynamic_cast<FunctionalExplicitSurface*>(geometry) ) {
        return OpenGL4FunctionalExplicitSurfaceRenderer::buildMesh(*f);
    }
    if ( ParametricBiCubicPatch* p = dynamic_cast<ParametricBiCubicPatch*>(geometry) ) {
        return OpenGL4ParametricBiCubicPatchRenderer::buildMesh(*p);
    }
    if ( SubdividedBezierPatch* p = dynamic_cast<SubdividedBezierPatch*>(geometry) ) {
        return OpenGL4SubdividedBezierPatchRenderer::buildMesh(*p);
    }
    if ( InfinitePlane* p = dynamic_cast<InfinitePlane*>(geometry) ) {
        return OpenGL4InfinitePlaneRenderer::buildMesh(*p);
    }
    if ( VoxelVolume* v = dynamic_cast<VoxelVolume*>(geometry) ) {
        return OpenGL4VoxelVolumeRenderer::buildMesh(*v);
    }
    if ( QuadricSurface* q = dynamic_cast<QuadricSurface*>(geometry) ) {
        return OpenGL4QuadricSurfaceRenderer::buildMesh(*q);
    }
    if ( ImplicitSolid* s = dynamic_cast<ImplicitSolid*>(geometry) ) {
        return OpenGL4ImplicitSolidRenderer::buildMesh(*s);
    }
    if ( HeightFieldSolid* h = dynamic_cast<HeightFieldSolid*>(geometry) ) {
        return OpenGL4HeightFieldSolidRenderer::buildMesh(*h);
    }
    return nullptr;
}

OpenGL4MeshRenderer::Mesh* obtainMesh(Geometry* geometry)
{
    java::String key = keyOf(geometry);

    if ( key.isEmpty() ) {
        const char* name = typeid(*geometry).name();
        if ( reportedUnsupported.add(name) ) {
            fprintf(stderr, "OpenGL4GeometryRenderer: Geometry not supported "
                    "by the GL4 pipeline yet: %s\n", name);
        }
        return nullptr;
    }
    OpenGL4MeshRenderer::Mesh** known = meshes.get(key);
    OpenGL4MeshRenderer::Mesh* mesh = known != nullptr ? *known : nullptr;

    if ( mesh == nullptr ) {
        mesh = buildMesh(geometry);
        if ( mesh == nullptr ) {
            return nullptr;
        }
        if ( meshes.size() >= MAX_CACHED_MESHES ) {
            // Stale meshes (i.e. from geometries edited many times) are
            // released, and recreated on demand
            releaseMeshes();
        }
        meshes.put(key, mesh);
    }
    return mesh;
}

}

bool OpenGL4GeometryRenderer::isSupported(Geometry* geometry)
{
    Volume* volume = dynamic_cast<Volume*>(geometry);
    return dynamic_cast<Sphere*>(geometry) != nullptr ||
        dynamic_cast<ParametricCurve*>(geometry) != nullptr ||
        dynamic_cast<TriangleMeshGroup*>(geometry) != nullptr ||
        dynamic_cast<PlanarHalfSpace*>(geometry) != nullptr ||
        dynamic_cast<PolyhedralBoundedSolid*>(geometry) != nullptr ||
        !keyOf(geometry).isEmpty() ||
        (volume != nullptr &&
         OpenGL4VolumeRenderer::exportedSolid(volume) != nullptr);
}

void OpenGL4GeometryRenderer::draw(Geometry* geometry, Camera* camera,
    Light* light, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    java::ArrayList<Light*> lights;

    if ( light != nullptr ) {
        lights.add(light);
    }
    draw(geometry, camera, light != nullptr ? &lights : nullptr, material,
        quality, textureMap, normalMap, localTransform);
}

void OpenGL4GeometryRenderer::draw(Geometry* geometry, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    if ( geometry == nullptr ) {
        return;
    }
    // C++ port note: as before the split, nothing is drawn without a camera
    // or a configuration (the Java renderers check them later)
    if ( camera == nullptr || quality == nullptr ) {
        return;
    }
    if ( Sphere* sphere = dynamic_cast<Sphere*>(geometry) ) {
        // C++ port note: `OpenGL4SphereRenderer` takes a single light; as in
        // Java, without lights the scene is lit from the camera
        PointLight cameraLight(camera->getPosition(), ColorRgb(1, 1, 1));
        const Light* light = lights != nullptr && lights->size() > 0 ?
            (*lights)[0] : &cameraLight;
        const SimpleMaterial defaultMaterial;
        OpenGL4SphereRenderer::draw(sphere, camera, light,
            material != nullptr ? material : &defaultMaterial, quality,
            textureMap, normalMap, localTransform,
            OpenGL4SphereRenderer::SLICES, OpenGL4SphereRenderer::SLICES / 2);
        if ( quality->isBoundingVolumeSet() ) {
            OpenGL4MinMaxRenderer::draw(geometry, camera, localTransform);
        }
        if ( quality->isSelectionCornersSet() ) {
            OpenGL4SelectionCornersRenderer::draw(geometry, camera, localTransform);
        }
        return;
    }
    if ( ParametricCurve* curve = dynamic_cast<ParametricCurve*>(geometry) ) {
        OpenGL4ParametricCurveRenderer::draw(curve, camera, quality,
            localTransform);
        return;
    }
    if ( TriangleMeshGroup* group = dynamic_cast<TriangleMeshGroup*>(geometry) ) {
        OpenGL4TriangleMeshGroupRenderer::draw(group, camera, lights, material,
            quality, textureMap, normalMap, localTransform);
        return;
    }
    if ( PlanarHalfSpace* halfSpace = dynamic_cast<PlanarHalfSpace*>(geometry) ) {
        OpenGL4PlanarHalfSpaceRenderer::draw(halfSpace, camera, lights,
            material, quality, textureMap, normalMap, localTransform);
        return;
    }
    if ( PolyhedralBoundedSolid* solid = dynamic_cast<PolyhedralBoundedSolid*>(geometry) ) {
        OpenGL4PolyhedralBoundedSolidRenderer::draw(solid, camera, lights,
            material, quality, localTransform);
        return;
    }
    Volume* volume = dynamic_cast<Volume*>(geometry);
    if ( keyOf(geometry).isEmpty() && volume != nullptr &&
         OpenGL4VolumeRenderer::draw(volume, camera, lights, material,
             quality, localTransform) ) {
        return;
    }
    OpenGL4MeshRenderer::Mesh* mesh = obtainMesh(geometry);
    if ( mesh == nullptr ) {
        return;
    }
    InfinitePlane* plane = dynamic_cast<InfinitePlane*>(geometry);
    if ( plane != nullptr ) {
        OpenGL4InfinitePlaneRenderer::drawMesh(mesh, plane, camera, lights,
            material, quality, textureMap, normalMap, localTransform);
        return;
    }
    OpenGL4MeshRenderer::draw(mesh, geometry, camera, lights, material,
        quality, textureMap, normalMap, localTransform);
}

void OpenGL4GeometryRenderer::dispose()
{
    releaseMeshes();
    OpenGL4VolumeRenderer::dispose();
    OpenGL4SphereRenderer::dispose();
    OpenGL4MeshRenderer::dispose();
    OpenGL4ColoredPrimitiveRenderer::release();
    OpenGL4LineRenderer::release();
}
