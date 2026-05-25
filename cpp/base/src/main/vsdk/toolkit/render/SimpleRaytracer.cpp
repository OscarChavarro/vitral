#include "vsdk/toolkit/render/SimpleRaytracer.h"

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/background/Background.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/render/TileGenerator.h"
#include "vsdk/toolkit/render/Tile.h"
#include "vsdk/toolkit/render/shaders/Shader.h"
#include "vsdk/toolkit/render/shaders/ShaderSelector.h"

#include <cmath>
#include <stdexcept>

SimpleRaytracer::SimpleRaytracer() : workspace(MAX_RECURSION_LEVEL)
{
}

bool SimpleRaytracer::hasNonAmbientLights(const std::vector<Light*>& lights)
{ for (size_t i=0;i<lights.size();i++) if (lights[i] && lights[i]->tipo_de_luz != LightType::AMBIENT) return true; return false; }

bool SimpleRaytracer::isReflective(SimpleMaterial* material)
{ return material != 0 && material->getReflectionCoefficient() > 0; }

RenderContext SimpleRaytracer::buildRenderContext(const RendererConfiguration* qualitySelection,const std::vector<Light*>& lights)
{
    Shader* localShader = ShaderSelector::select(qualitySelection);
    bool localLightingEnabled =
        qualitySelection->getShadingType() != RendererConfiguration::SHADING_TYPE_NOLIGHT &&
        hasNonAmbientLights(lights);
    return RenderContext(localLightingEnabled, qualitySelection->isTextureSet(), qualitySelection->isBumpMapSet(), localShader);
}

int SimpleRaytracer::buildSurfaceDetailMask(SimpleMaterial* material, Image* texture, NormalMap* normalMap, const RenderContext& renderContext)
{
    bool objectReflective = isReflective(material);
    bool needsPoint = renderContext.localLightingEnabled || objectReflective;
    bool needsNormal = needsPoint;
    bool needsUv = (renderContext.textureEnabled && texture != 0) || ((renderContext.localLightingEnabled || objectReflective) && normalMap != 0);
    bool needsTangent = (renderContext.localLightingEnabled || objectReflective) && normalMap != 0;
    int detailMask = RayHit::DETAIL_NONE;
    if (needsPoint) detailMask |= RayHit::DETAIL_POINT;
    if (needsNormal) detailMask |= RayHit::DETAIL_NORMAL;
    if (needsUv) detailMask |= RayHit::DETAIL_UV;
    if (needsTangent) detailMask |= RayHit::DETAIL_TANGENT;
    return detailMask;
}

std::vector<long long> SimpleRaytracer::captureBodyVersions(const std::vector<SimpleBody*>& bodies)
{ std::vector<long long> v(bodies.size()); for(size_t i=0;i<bodies.size();i++) v[i]=bodies[i]->getModificationVersion(); return v; }

void SimpleRaytracer::assertSceneUnmodifiedDuringRender(const std::vector<long long>& expected,const std::vector<SimpleBody*>& bodies)
{
    if ( expected.size() != bodies.size() ) throw std::runtime_error("Scene bodies list changed while raytracing");
    for ( size_t i = 0; i < bodies.size(); i++ ) {
        if ( bodies[i]->getModificationVersion() != expected[i] ) throw std::runtime_error("SimpleBody modified while raytracing");
    }
}

Ray SimpleRaytracer::generateRay(const CameraSnapshot* c, int x, int y)
{
    double viewportXSize = c->getViewportXSize();
    double viewportYSize = c->getViewportYSize();
    double pixelCenterX = (double)x + 0.5;
    double pixelCenterY = (double)y + 0.5;
    double u = (pixelCenterX - viewportXSize/2.0) / viewportXSize;
    double v = ((viewportYSize - pixelCenterY) - viewportYSize/2.0) / viewportYSize;

    if ( c->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        Vector3Dd left = c->getLeft(); Vector3Dd up = c->getUp(); Vector3Dd front = c->getFront(); Vector3Dd eye = c->getEyePosition();
        double fovFactor = viewportXSize/viewportYSize;
        double duScale = (-fovFactor) * (2*u/c->getOrthogonalZoom());
        double dvScale = 2*v/c->getOrthogonalZoom();
        Vector3Dd origin(eye.x()+left.x()*duScale+up.x()*dvScale, eye.y()+left.y()*duScale+up.y()*dvScale, eye.z()+left.z()*duScale+up.z()*dvScale);
        return Ray(origin, front);
    }

    Vector3Dd rw = c->getRightWithScale();
    Vector3Dd uw = c->getUpWithScale();
    Vector3Dd dir = c->getDir();
    Vector3Dd direction(rw.x()*u + uw.x()*v + dir.x(), rw.y()*u + uw.y()*v + dir.y(), rw.z()*u + uw.z()*v + dir.z());
    return Ray(c->getEyePosition(), direction);
}

void SimpleRaytracer::prepareSurfaceHit(SimpleBody* nearestObject, const SceneObjectRenderData& objectData, const Ray& hitRay, RayHit* outHit)
{
    outHit->reset(objectData.detailMask);
    outHit->setRay(hitRay);
    if ( objectData.detailMask != RayHit::DETAIL_NONE ) {
        nearestObject->doExtraInformation(hitRay, hitRay.t(), outHit);
        outHit->setRay(hitRay);
    }
    if ( !outHit->needsTextureCoordinates() ) outHit->texture = 0;
    else if ( outHit->texture == 0 ) outHit->texture = objectData.texture;
    if ( !outHit->needsTextureCoordinates() || !outHit->needsNormal() || !outHit->needsTangent() ) outHit->normalMap = 0;
    else if ( outHit->normalMap == 0 ) outHit->normalMap = objectData.normalMap;
}

SimpleMaterial* SimpleRaytracer::resolveMaterial(RayHit* hit, const SceneObjectRenderData& objectData)
{ return hit->material != 0 ? hit->material : objectData.material; }

ColorRgb SimpleRaytracer::evaluateIlluminationModel(RayHit* info,double viewX,double viewY,double viewZ,const std::vector<Light*>& lights,const std::vector<SimpleBody*>& objects,const SceneRenderCache& cache,Background* background,SimpleMaterial* material,RenderContext& renderContext,int recursions,int recursionLevel)
{
    Shader::LocalShadingResult localShading = renderContext.localShader->shadeLocal(info, viewX, viewY, viewZ, lights, objects, material, &workspace);
    Vector3Dd surfaceNormal = localShading.normal;
    double outR = localShading.color.r(), outG = localShading.color.g(), outB = localShading.color.b();
    double nx = surfaceNormal.x(), ny = surfaceNormal.y(), nz = surfaceNormal.z();

    double kr = material->getReflectionCoefficient();
    if ( kr > 0 && recursions > 0 ) {
        double t = viewX*nx + viewY*ny + viewZ*nz;
        if ( t > 0 ) {
            double twoT = 2*t;
            double rx = twoT*nx - viewX, ry = twoT*ny - viewY, rz = twoT*nz - viewZ;
            Vector3Dd reflect(rx, ry, rz);
            Vector3Dd poffset(info->p.x() + VSDK::EPSILON*rx, info->p.y() + VSDK::EPSILON*ry, info->p.z() + VSDK::EPSILON*rz);
            Ray reflectedRay(poffset, reflect);

            RayHit* reflectedHit = &workspace.reflectionHits[recursionLevel];
            int nearestObjectIndex = selectNearestThingInRayDirection(reflectedRay, objects, reflectedHit, &workspace.traversalCandidateHit);
            if ( nearestObjectIndex >= 0 ) {
                SimpleBody* nearestObject = objects[nearestObjectIndex];
                const SceneObjectRenderData& objectData = cache.objects[nearestObjectIndex];
                RayHit* subInfo = &workspace.shadingHits[recursionLevel + 1];
                Ray reflectedHitRay = reflectedRay.withT(reflectedHit->hitDistance());
                prepareSurfaceHit(nearestObject, objectData, reflectedHitRay, subInfo);
                ColorRgb rcolor = evaluateIlluminationModel(subInfo, -reflectedRay.direction().x(), -reflectedRay.direction().y(), -reflectedRay.direction().z(), lights, objects, cache, background, resolveMaterial(subInfo, objectData), renderContext, recursions - 1, recursionLevel + 1);
                outR += rcolor.r() * kr; outG += rcolor.g() * kr; outB += rcolor.b() * kr;
            }
            else {
                ColorRgb reflectedBackground = background->colorInDireccion(reflect);
                outR += reflectedBackground.r() * kr; outG += reflectedBackground.g() * kr; outB += reflectedBackground.b() * kr;
            }
        }
    }

    return ColorRgb(outR > 1 ? 1 : outR, outG > 1 ? 1 : outG, outB > 1 ? 1 : outB);
}

int SimpleRaytracer::selectNearestThingInRayDirection(const Ray& inRay,const std::vector<SimpleBody*>& bodies,RayHit* outHit,RayHit* candidateHit)
{
    double nearestDistance = 1e308; int nearestIndex = -1;
    candidateHit->setStoreRay(false);
    RaytraceStatistics::recordSceneTraversal();
    for ( size_t i=0; i<bodies.size(); i++ ) {
        candidateHit->resetForDistanceOnly();
        RaytraceStatistics::recordObjectIntersectionTest();
        if ( bodies[i]->doIntersection(inRay, candidateHit) ) {
            double d = candidateHit->hitDistance();
            if ( d < nearestDistance && d > VSDK::EPSILON ) { nearestDistance = d; nearestIndex = (int)i; }
        }
    }
    if ( nearestIndex >= 0 && outHit != 0 ) { outHit->resetForDistanceOnly(); outHit->setHitDistance(nearestDistance); }
    return nearestIndex;
}

ColorRgb SimpleRaytracer::followRayPath(const Ray& inRay,const std::vector<SimpleBody*>& bodies,const std::vector<Light*>& lights,Background* background,RenderContext& renderContext,const SceneRenderCache& cache)
{
    RayHit* hitInfo = &workspace.nearestHit;
    int nearestIndex = selectNearestThingInRayDirection(inRay, bodies, hitInfo, &workspace.traversalCandidateHit);
    if ( nearestIndex >= 0 ) {
        SimpleBody* nearestObject = bodies[nearestIndex];
        const SceneObjectRenderData& objectData = cache.objects[nearestIndex];
        Ray primaryHitRay = inRay.withT(hitInfo->hitDistance());
        RayHit* shadingInfo = &workspace.shadingHits[0];
        prepareSurfaceHit(nearestObject, objectData, primaryHitRay, shadingInfo);
        return evaluateIlluminationModel(shadingInfo, -inRay.direction().x(), -inRay.direction().y(), -inRay.direction().z(), lights, bodies, cache, background, resolveMaterial(shadingInfo, objectData), renderContext, MAX_RECURSION_LEVEL, 0);
    }
    return background->colorInDireccion(inRay.direction());
}

void SimpleRaytracer::execute(RGBImageUncompressed* inoutViewport,const RendererConfiguration* q,SimpleSceneSnapshot* sceneSnapshot,ProgressMonitor* report)
{ execute(inoutViewport, q, sceneSnapshot, report, 0, 0, 0, inoutViewport->getXSize(), inoutViewport->getYSize()); }

void SimpleRaytracer::execute(RGBImageUncompressed* inoutViewport,const RendererConfiguration* q,SimpleSceneSnapshot* sceneSnapshot,ProgressMonitor* report,ZBuffer* depthmap)
{ execute(inoutViewport, q, sceneSnapshot, report, depthmap, 0, 0, inoutViewport->getXSize(), inoutViewport->getYSize()); }

void SimpleRaytracer::execute(RGBImageUncompressed* inoutViewport,const RendererConfiguration* q,SimpleSceneSnapshot* sceneSnapshot,ProgressMonitor* liveReport,ZBuffer* outDepthmap,int limx1,int limy1,int limx2,int limy2)
{ execute(inoutViewport, q, sceneSnapshot->getSimpleBodies(), sceneSnapshot->getLights(), sceneSnapshot->getBackground(), sceneSnapshot->getCameraSnapshot(), liveReport, outDepthmap, limx1, limy1, limx2, limy2); }

void SimpleRaytracer::execute(RGBImageUncompressed* inoutViewport,const RendererConfiguration* q,const std::vector<SimpleBody*>& bodies,const std::vector<Light*>& lights,Background* bg,const CameraSnapshot* cameraSnapshot,ProgressMonitor* liveReport,ZBuffer* outDepthmap,int limx1,int limy1,int limx2,int limy2)
{
    RenderContext renderContext = buildRenderContext(q, lights);
    SceneRenderCache cache;
    cache.objects.resize(bodies.size());
    for ( size_t i=0; i<bodies.size(); i++ ) {
        cache.objects[i].material = bodies[i]->getMaterial();
        cache.objects[i].texture = renderContext.textureEnabled ? bodies[i]->getTexture() : 0;
        cache.objects[i].normalMap = renderContext.bumpMappingEnabled ? bodies[i]->getNormalMap() : 0;
        cache.objects[i].detailMask = buildSurfaceDetailMask(cache.objects[i].material, cache.objects[i].texture, cache.objects[i].normalMap, renderContext);
    }

    std::vector<long long> versions = captureBodyVersions(bodies);
    TileGenerator tileGenerator(TILE_STRATEGY, inoutViewport, limx1, limy1, limx2 - limx1, limy2 - limy1, TILE_WORKERS_HINT);
    const std::vector<Tile>& tiles = tileGenerator.getTiles();
    RGBPixel outputPixel;

    if ( liveReport ) liveReport->begin();
    for ( size_t ti=0; ti<tiles.size(); ti++ ) {
        const Tile& tile = tiles[ti];
        Image* tileImage = tile.getImage();
        for ( int y = tile.getY0(); y < tile.getY1(); y++ ) {
            assertSceneUnmodifiedDuringRender(versions, bodies);
            if ( liveReport ) liveReport->update(0, inoutViewport->getYSize(), y);
            for ( int x = tile.getX0(); x < tile.getX1(); x++ ) {
                RaytraceStatistics::recordPrimaryRay();
                Ray ray = generateRay(cameraSnapshot, x, y);
                ColorRgb color = followRayPath(ray, bodies, lights, bg, renderContext, cache);
                if ( outDepthmap ) outDepthmap->setDepth(x, y, (float)ray.t());
                outputPixel.r = (char)(255 * color.r()); outputPixel.g = (char)(255 * color.g()); outputPixel.b = (char)(255 * color.b());
                tileImage->putPixelRgb(x, y, &outputPixel);
            }
        }
    }
    if ( liveReport ) liveReport->end();
    delete renderContext.localShader;
}
