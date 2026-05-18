#ifndef __VSDK_TOOLKIT_RENDER_SIMPLERAYTRACER_H__
#define __VSDK_TOOLKIT_RENDER_SIMPLERAYTRACER_H__

#include "RenderingElement.h"
#include "TraceWorkspace.h"
#include "RenderContext.h"
#include "TileGenerationStrategy.h"
#include <vector>

class Ray;
class RayHit;
class ColorRgb;
class RGBImageUncompressed;
class RGBPixel;
class ZBuffer;
class RendererConfiguration;
class ProgressMonitor;
class SimpleSceneSnapshot;
class SimpleBody;
class Light;
class Background;
class SimpleMaterial;
class Shader;
class Image;
class NormalMap;
class CameraSnapshot;

class SimpleRaytracer : public RenderingElement {
private:
    static const int MAX_RECURSION_LEVEL = TraceWorkspace::DEFAULT_MAX_RECURSION_LEVEL;
    static const TileGenerationStrategy TILE_STRATEGY = TileGenerationStrategy::SERIAL;
    static const int TILE_WORKERS_HINT = 1;

    struct SceneObjectRenderData {
        SimpleMaterial* material;
        Image* texture;
        NormalMap* normalMap;
        int detailMask;
        SceneObjectRenderData() : material(0), texture(0), normalMap(0), detailMask(0) {}
    };

    struct SceneRenderCache {
        std::vector<SceneObjectRenderData> objects;
    };

    TraceWorkspace workspace;

    static bool hasNonAmbientLights(const std::vector<Light*>& lights);
    static bool isReflective(SimpleMaterial* material);
    static RenderContext buildRenderContext(const RendererConfiguration* qualitySelection,const std::vector<Light*>& lights);
    static int buildSurfaceDetailMask(SimpleMaterial* material, Image* texture, NormalMap* normalMap, const RenderContext& renderContext);
    static std::vector<long long> captureBodyVersions(const std::vector<SimpleBody*>& bodies);
    static void assertSceneUnmodifiedDuringRender(const std::vector<long long>& expectedBodyVersions,const std::vector<SimpleBody*>& bodies);
    static Ray generateRay(const CameraSnapshot* cameraSnapshot, int x, int y);

    void prepareSurfaceHit(SimpleBody* nearestObject, const SceneObjectRenderData& objectData, const Ray& hitRay, RayHit* outHit);
    static SimpleMaterial* resolveMaterial(RayHit* hit, const SceneObjectRenderData& objectData);
    ColorRgb evaluateIlluminationModel(RayHit* info,double viewX,double viewY,double viewZ,const std::vector<Light*>& lights,const std::vector<SimpleBody*>& objects,const SceneRenderCache& sceneRenderCache,Background* background,SimpleMaterial* material,RenderContext& renderContext,int recursions,int recursionLevel);
    int selectNearestThingInRayDirection(const Ray& inRay,const std::vector<SimpleBody*>& inSimpleBodiesArray,RayHit* outHit,RayHit* candidateHit);
    ColorRgb followRayPath(const Ray& inRay,const std::vector<SimpleBody*>& inSimpleBodiesArray,const std::vector<Light*>& inLightsArray,Background* in_background,RenderContext& renderContext,const SceneRenderCache& sceneRenderCache);
    void execute(RGBImageUncompressed* inoutViewport,const RendererConfiguration* inQualitySelection,const std::vector<SimpleBody*>& inSimpleBodiesArray,const std::vector<Light*>& inLightsArray,Background* inBackground,const CameraSnapshot* cameraSnapshot,ProgressMonitor* liveReport,ZBuffer* outDepthmap,int limx1,int limy1,int limx2,int limy2);

public:
    SimpleRaytracer();
    virtual ~SimpleRaytracer() {}

    void execute(RGBImageUncompressed* inoutViewport,const RendererConfiguration* inQualitySelection,SimpleSceneSnapshot* sceneSnapshot,ProgressMonitor* report);
    void execute(RGBImageUncompressed* inoutViewport,const RendererConfiguration* inQualitySelection,SimpleSceneSnapshot* sceneSnapshot,ProgressMonitor* report,ZBuffer* depthmap);
    void execute(RGBImageUncompressed* inoutViewport,const RendererConfiguration* inQualitySelection,SimpleSceneSnapshot* sceneSnapshot,ProgressMonitor* liveReport,ZBuffer* outDepthmap,int limx1,int limy1,int limx2,int limy2);
};

#endif
