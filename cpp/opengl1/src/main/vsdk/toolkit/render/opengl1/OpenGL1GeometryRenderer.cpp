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
#include "vsdk/toolkit/environment/geometry/volume/Prism.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl1/curve/OpenGL1ParametricCurveRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1FunctionalExplicitSurfaceRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1InfinitePlaneRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1ParametricBiCubicPatchRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1QuadMeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1SubdividedBezierPatchRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshGroupRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleStripMeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1ArrowRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1BoxRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1ConeRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1HeightFieldSolidRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1ImplicitSolidRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1PlanarHalfSpaceRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1PrismRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1QuadricSurfaceRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1SphereRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1TorusRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1TubeRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1VolumeRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1VoxelVolumeRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/polyhedralBoundedSolid/OpenGL1PolyhedralBoundedSolidRenderer.h"

namespace {

const long MAX_CACHED_MESHES = 256;

java::TreeMap<java::String, OpenGL1MeshRenderer::Mesh*> meshes;
java::TreeSet<java::String> reportedUnsupported;

void releaseMeshes()
{
    for ( long i = 0; i < meshes.size(); i++ ) {
        OpenGL1MeshRenderer::release(meshes.getValue(i));
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
        return OpenGL1ConeRenderer::meshKey(*c);
    }
    if ( Arrow* a = dynamic_cast<Arrow*>(geometry) ) {
        return OpenGL1ArrowRenderer::meshKey(*a);
    }
    if ( Box* b = dynamic_cast<Box*>(geometry) ) {
        return OpenGL1BoxRenderer::meshKey(*b);
    }
    if ( Torus* t = dynamic_cast<Torus*>(geometry) ) {
        return OpenGL1TorusRenderer::meshKey(*t);
    }
    if ( Tube* t = dynamic_cast<Tube*>(geometry) ) {
        return OpenGL1TubeRenderer::meshKey(*t);
    }
    if ( Prism* p = dynamic_cast<Prism*>(geometry) ) {
        return OpenGL1PrismRenderer::meshKey(*p);
    }
    if ( TriangleMesh* m = dynamic_cast<TriangleMesh*>(geometry) ) {
        return OpenGL1TriangleMeshRenderer::meshKey(*m);
    }
    if ( QuadMesh* m = dynamic_cast<QuadMesh*>(geometry) ) {
        return OpenGL1QuadMeshRenderer::meshKey(*m);
    }
    if ( TriangleStripMesh* m = dynamic_cast<TriangleStripMesh*>(geometry) ) {
        return OpenGL1TriangleStripMeshRenderer::meshKey(*m);
    }
    if ( FunctionalExplicitSurface* f = dynamic_cast<FunctionalExplicitSurface*>(geometry) ) {
        return OpenGL1FunctionalExplicitSurfaceRenderer::meshKey(*f);
    }
    if ( ParametricBiCubicPatch* p = dynamic_cast<ParametricBiCubicPatch*>(geometry) ) {
        return OpenGL1ParametricBiCubicPatchRenderer::meshKey(*p);
    }
    if ( SubdividedBezierPatch* p = dynamic_cast<SubdividedBezierPatch*>(geometry) ) {
        return OpenGL1SubdividedBezierPatchRenderer::meshKey(*p);
    }
    if ( InfinitePlane* p = dynamic_cast<InfinitePlane*>(geometry) ) {
        return OpenGL1InfinitePlaneRenderer::meshKey(*p);
    }
    if ( VoxelVolume* v = dynamic_cast<VoxelVolume*>(geometry) ) {
        return OpenGL1VoxelVolumeRenderer::meshKey(*v);
    }
    if ( QuadricSurface* q = dynamic_cast<QuadricSurface*>(geometry) ) {
        return OpenGL1QuadricSurfaceRenderer::meshKey(*q);
    }
    if ( ImplicitSolid* s = dynamic_cast<ImplicitSolid*>(geometry) ) {
        return OpenGL1ImplicitSolidRenderer::meshKey(*s);
    }
    if ( HeightFieldSolid* h = dynamic_cast<HeightFieldSolid*>(geometry) ) {
        return OpenGL1HeightFieldSolidRenderer::meshKey(*h);
    }
    return java::String();
}

/**
@return the mesh of the geometry, built by its renderer, or null if it
can not be tessellated
*/
OpenGL1MeshRenderer::Mesh* buildMesh(Geometry* geometry)
{
    if ( Cone* c = dynamic_cast<Cone*>(geometry) ) {
        return OpenGL1ConeRenderer::buildMesh(*c);
    }
    if ( Arrow* a = dynamic_cast<Arrow*>(geometry) ) {
        return OpenGL1ArrowRenderer::buildMesh(*a);
    }
    if ( Box* b = dynamic_cast<Box*>(geometry) ) {
        return OpenGL1BoxRenderer::buildMesh(*b);
    }
    if ( Torus* t = dynamic_cast<Torus*>(geometry) ) {
        return OpenGL1TorusRenderer::buildMesh(*t);
    }
    if ( Tube* t = dynamic_cast<Tube*>(geometry) ) {
        return OpenGL1TubeRenderer::buildMesh(*t);
    }
    if ( Prism* p = dynamic_cast<Prism*>(geometry) ) {
        return OpenGL1PrismRenderer::buildMesh(*p);
    }
    if ( TriangleMesh* m = dynamic_cast<TriangleMesh*>(geometry) ) {
        return OpenGL1TriangleMeshRenderer::buildMesh(*m);
    }
    if ( QuadMesh* m = dynamic_cast<QuadMesh*>(geometry) ) {
        return OpenGL1QuadMeshRenderer::buildMesh(*m);
    }
    if ( TriangleStripMesh* m = dynamic_cast<TriangleStripMesh*>(geometry) ) {
        return OpenGL1TriangleStripMeshRenderer::buildMesh(*m);
    }
    if ( FunctionalExplicitSurface* f = dynamic_cast<FunctionalExplicitSurface*>(geometry) ) {
        return OpenGL1FunctionalExplicitSurfaceRenderer::buildMesh(*f);
    }
    if ( ParametricBiCubicPatch* p = dynamic_cast<ParametricBiCubicPatch*>(geometry) ) {
        return OpenGL1ParametricBiCubicPatchRenderer::buildMesh(*p);
    }
    if ( SubdividedBezierPatch* p = dynamic_cast<SubdividedBezierPatch*>(geometry) ) {
        return OpenGL1SubdividedBezierPatchRenderer::buildMesh(*p);
    }
    if ( InfinitePlane* p = dynamic_cast<InfinitePlane*>(geometry) ) {
        return OpenGL1InfinitePlaneRenderer::buildMesh(*p);
    }
    if ( VoxelVolume* v = dynamic_cast<VoxelVolume*>(geometry) ) {
        return OpenGL1VoxelVolumeRenderer::buildMesh(*v);
    }
    if ( QuadricSurface* q = dynamic_cast<QuadricSurface*>(geometry) ) {
        return OpenGL1QuadricSurfaceRenderer::buildMesh(*q);
    }
    if ( ImplicitSolid* s = dynamic_cast<ImplicitSolid*>(geometry) ) {
        return OpenGL1ImplicitSolidRenderer::buildMesh(*s);
    }
    if ( HeightFieldSolid* h = dynamic_cast<HeightFieldSolid*>(geometry) ) {
        return OpenGL1HeightFieldSolidRenderer::buildMesh(*h);
    }
    return nullptr;
}

OpenGL1MeshRenderer::Mesh* obtainMesh(Geometry* geometry)
{
    java::String key = keyOf(geometry);

    if ( key.isEmpty() ) {
        const char* name = typeid(*geometry).name();
        if ( reportedUnsupported.add(name) ) {
            fprintf(stderr, "OpenGL1GeometryRenderer: Geometry not supported "
                    "by the OpenGL 1.2 pipeline yet: %s\n", name);
        }
        return nullptr;
    }
    OpenGL1MeshRenderer::Mesh** known = meshes.get(key);
    OpenGL1MeshRenderer::Mesh* mesh = known != nullptr ? *known : nullptr;

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

bool OpenGL1GeometryRenderer::isSupported(Geometry* geometry)
{
    Volume* volume = dynamic_cast<Volume*>(geometry);
    return dynamic_cast<Sphere*>(geometry) != nullptr ||
        dynamic_cast<ParametricCurve*>(geometry) != nullptr ||
        dynamic_cast<TriangleMeshGroup*>(geometry) != nullptr ||
        dynamic_cast<PlanarHalfSpace*>(geometry) != nullptr ||
        dynamic_cast<PolyhedralBoundedSolid*>(geometry) != nullptr ||
        !keyOf(geometry).isEmpty() ||
        (volume != nullptr &&
         OpenGL1VolumeRenderer::exportedSolid(volume) != nullptr);
}

void OpenGL1GeometryRenderer::draw(Geometry* geometry, Camera* camera,
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

void OpenGL1GeometryRenderer::draw(Geometry* geometry, Camera* camera,
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
        // C++ port note: `OpenGL1SphereRenderer` takes a single light; as in
        // Java, without lights the scene is lit from the camera
        PointLight cameraLight(camera->getPosition(), ColorRgb(1, 1, 1));
        const Light* light = lights != nullptr && lights->size() > 0 ?
            (*lights)[0] : &cameraLight;
        const SimpleMaterial defaultMaterial;
        OpenGL1SphereRenderer::draw(sphere, camera, light,
            material != nullptr ? material : &defaultMaterial, quality,
            textureMap, normalMap, localTransform,
            OpenGL1SphereRenderer::SLICES, OpenGL1SphereRenderer::SLICES / 2);
        if ( quality->isBoundingVolumeSet() ) {
            OpenGL1MinMaxRenderer::draw(geometry, camera, localTransform);
        }
        if ( quality->isSelectionCornersSet() ) {
            OpenGL1SelectionCornersRenderer::draw(geometry, camera, localTransform);
        }
        return;
    }
    if ( ParametricCurve* curve = dynamic_cast<ParametricCurve*>(geometry) ) {
        OpenGL1ParametricCurveRenderer::draw(curve, camera, quality,
            localTransform);
        return;
    }
    if ( TriangleMeshGroup* group = dynamic_cast<TriangleMeshGroup*>(geometry) ) {
        OpenGL1TriangleMeshGroupRenderer::draw(group, camera, lights, material,
            quality, textureMap, normalMap, localTransform);
        return;
    }
    if ( PlanarHalfSpace* halfSpace = dynamic_cast<PlanarHalfSpace*>(geometry) ) {
        OpenGL1PlanarHalfSpaceRenderer::draw(halfSpace, camera, lights,
            material, quality, textureMap, normalMap, localTransform);
        return;
    }
    if ( PolyhedralBoundedSolid* solid = dynamic_cast<PolyhedralBoundedSolid*>(geometry) ) {
        OpenGL1PolyhedralBoundedSolidRenderer::draw(solid, camera, lights,
            material, quality, localTransform);
        return;
    }
    Volume* volume = dynamic_cast<Volume*>(geometry);
    if ( keyOf(geometry).isEmpty() && volume != nullptr &&
         OpenGL1VolumeRenderer::draw(volume, camera, lights, material,
             quality, localTransform) ) {
        return;
    }
    OpenGL1MeshRenderer::Mesh* mesh = obtainMesh(geometry);
    if ( mesh == nullptr ) {
        return;
    }
    InfinitePlane* plane = dynamic_cast<InfinitePlane*>(geometry);
    if ( plane != nullptr ) {
        OpenGL1InfinitePlaneRenderer::drawMesh(mesh, plane, camera, lights,
            material, quality, textureMap, normalMap, localTransform);
        return;
    }
    OpenGL1MeshRenderer::draw(mesh, geometry, camera, lights, material,
        quality, textureMap, normalMap, localTransform);
}

void OpenGL1GeometryRenderer::dispose()
{
    releaseMeshes();
    OpenGL1VolumeRenderer::dispose();
    OpenGL1SphereRenderer::dispose();
    OpenGL1MeshRenderer::dispose();
    OpenGL1ColoredPrimitiveRenderer::release();
    OpenGL1LineRenderer::release();
}
