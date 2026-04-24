//= References:                                                             =
//= [BLIN1978b] Blinn, James F. "Simulation of wrinkled surfaces", SIGGRAPH =
//=          proceedings, 1978.                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =
//= [WHIT1980] Whitted, Turner. "An Improved Illumination Model for Shaded  =
//=            Display", 1980.                                              =
//=   rayable objects, with geometries implementing intersection operation  =
//=   in an object-coordinate basis.                                        =
//=   for inclusion of sub-viewport spec.                                   =
//=   sub-materials inside geometry.                                         =

package vsdk.toolkit.render;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;

import vsdk.toolkit.common.RaytraceStatistics;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.CameraSnapshot;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.ProgressMonitor;

/**
This class provides an encaptulation for a rendering algorithm, 
implementing simple recursive raytracing as presented in [WHIT1980].
Includes a normal perturbation for the simulation of wrinkled surfaces,
as described in [BLIN1978b].
This class is appropiate to play a role of "concrete strategy" in
a "Strategy" design pattern.

\todo  Upgrade ArrayList management to Java 1.5 code style (typed templates)
*/
public class SimpleRaytracer extends RenderingElement {
    private static final double TINY = 0.0001;
    private static final int MAX_RECURSION_LEVEL =
        TraceWorkspace.DEFAULT_MAX_RECURSION_LEVEL;
    private static final TileGenerationStrategy TILE_STRATEGY =
        TileGenerationStrategy.SERIAL;
    private static final int TILE_WORKERS_HINT = 1;

    private final ThreadLocal<TraceWorkspace> traceWorkspace =
        ThreadLocal.withInitial(() -> new TraceWorkspace(MAX_RECURSION_LEVEL));

    public SimpleRaytracer()
    {
        // No mutable shared temporaries: keep method-level variables for reentrancy.
    }

    private static final class SceneObjectRenderData {
        final Material material;
        final Image texture;
        final NormalMap normalMap;
        final int detailMask;

        private SceneObjectRenderData(
            Material material,
            Image texture,
            NormalMap normalMap,
            int detailMask)
        {
            this.material = material;
            this.texture = texture;
            this.normalMap = normalMap;
            this.detailMask = detailMask;
        }
    }

    private static final class SceneRenderCache {
        final SceneObjectRenderData[] objects;

        private SceneRenderCache(
            List<SimpleBody> bodies,
            RenderContext renderContext)
        {
            objects = new SceneObjectRenderData[bodies.size()];
            for ( int i = 0; i < bodies.size(); i++ ) {
                SimpleBody body = bodies.get(i);
                Material material = body.getMaterial();
                Image texture =
                    renderContext.textureEnabled ? body.getTexture() : null;
                NormalMap normalMap =
                    renderContext.bumpMappingEnabled ? body.getNormalMap() : null;
                int detailMask = buildSurfaceDetailMask(
                    material, texture, normalMap, renderContext);
                objects[i] = new SceneObjectRenderData(
                    material,
                    texture,
                    normalMap,
                    detailMask);
            }
        }

        private SceneObjectRenderData objectData(int objectIndex)
        {
            return objects[objectIndex];
        }
    }

    private static boolean hasNonAmbientLights(List<Light> lights)
    {
        for ( Light light : lights ) {
            if ( light.tipo_de_luz != vsdk.toolkit.environment.LightType.AMBIENT ) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReflective(Material material)
    {
        return material != null && material.getReflectionCoefficient() > 0;
    }

    private static RenderContext buildRenderContext(
        RendererConfiguration qualitySelection,
        List<Light> lights)
    {
        boolean localLightingEnabled =
            qualitySelection.getShadingType() != RendererConfiguration.SHADING_TYPE_NOLIGHT &&
            hasNonAmbientLights(lights);

        return new RenderContext(
            localLightingEnabled,
            qualitySelection.isTextureSet(),
            qualitySelection.isBumpMapSet());
    }

    private static int buildSurfaceDetailMask(
        Material material,
        Image texture,
        NormalMap normalMap,
        RenderContext renderContext)
    {
        boolean objectReflective = isReflective(material);
        boolean needsPoint = renderContext.localLightingEnabled || objectReflective;
        boolean needsNormal = needsPoint;
        boolean needsUv =
            (renderContext.localLightingEnabled && texture != null) ||
            ((renderContext.localLightingEnabled || objectReflective) &&
             normalMap != null);
        boolean needsTangent =
            (renderContext.localLightingEnabled || objectReflective) &&
            normalMap != null;
        int detailMask = RayHit.DETAIL_NONE;

        if ( needsPoint ) {
            detailMask |= RayHit.DETAIL_POINT;
        }
        if ( needsNormal ) {
            detailMask |= RayHit.DETAIL_NORMAL;
        }
        if ( needsUv ) {
            detailMask |= RayHit.DETAIL_UV;
        }
        if ( needsTangent ) {
            detailMask |= RayHit.DETAIL_TANGENT;
        }
        return detailMask;
    }

    private static long[] captureBodyVersions(List<SimpleBody> bodies)
    {
        long[] versions = new long[bodies.size()];
        for ( int i = 0; i < bodies.size(); i++ ) {
            versions[i] = bodies.get(i).getModificationVersion();
        }
        return versions;
    }

    private static void assertSceneUnmodifiedDuringRender(
        long[] expectedBodyVersions,
        List<SimpleBody> bodies)
    {
        if ( expectedBodyVersions.length != bodies.size() ) {
            throw new IllegalStateException(
                "Scene bodies list changed while raytracing. " +
                "Freeze scene edits during SimpleRaytracer.execute.");
        }
        for ( int i = 0; i < bodies.size(); i++ ) {
            if ( bodies.get(i).getModificationVersion() != expectedBodyVersions[i] ) {
                throw new IllegalStateException(
                    "SimpleBody at index " + i +
                    " was modified while raytracing. " +
                    "Freeze scene/body edits during SimpleRaytracer.execute.");
            }
        }
    }

    private static Ray generateRay(CameraSnapshot cameraSnapshot, int x, int y)
    {
        double viewportXSize = cameraSnapshot.getViewportXSize();
        double viewportYSize = cameraSnapshot.getViewportYSize();
        double u = ((double)x - viewportXSize/2.0) / viewportXSize;
        double v =
            ((viewportYSize - (double)y - 1) - viewportYSize/2.0) / viewportYSize;

        if ( cameraSnapshot.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            Vector3D left = cameraSnapshot.getLeft();
            Vector3D up = cameraSnapshot.getUp();
            Vector3D front = cameraSnapshot.getFront();
            Vector3D eyePosition = cameraSnapshot.getEyePosition();
            double fovFactor = viewportXSize/viewportYSize;
            double duScale =
                (-fovFactor) * (2*u/cameraSnapshot.getOrthogonalZoom());
            double dvScale = 2*v/cameraSnapshot.getOrthogonalZoom();
            Vector3D origin = new Vector3D(
                eyePosition.x() + left.x()*duScale + up.x()*dvScale,
                eyePosition.y() + left.y()*duScale + up.y()*dvScale,
                eyePosition.z() + left.z()*duScale + up.z()*dvScale);
            return new Ray(origin, front);
        }

        Vector3D rightWithScale = cameraSnapshot.getRightWithScale();
        Vector3D upWithScale = cameraSnapshot.getUpWithScale();
        Vector3D dir = cameraSnapshot.getDir();
        Vector3D direction = new Vector3D(
            rightWithScale.x()*u + upWithScale.x()*v + dir.x(),
            rightWithScale.y()*u + upWithScale.y()*v + dir.y(),
            rightWithScale.z()*u + upWithScale.z()*v + dir.z());

        return new Ray(cameraSnapshot.getEyePosition(), direction);
    }

    private void prepareSurfaceHit(
        SimpleBody nearestObject,
        SceneObjectRenderData objectData,
        Ray hitRay,
        RayHit outHit)
    {
        int detailMask = objectData.detailMask;

        outHit.reset(detailMask);
        outHit.setRay(hitRay);
        if ( detailMask != RayHit.DETAIL_NONE ) {
            nearestObject.doExtraInformation(hitRay, hitRay.t(), outHit);
            outHit.setRay(hitRay);
        }

        if ( !outHit.needsTextureCoordinates() ) {
            outHit.texture = null;
        }
        else if ( outHit.texture == null ) {
            outHit.texture = objectData.texture;
        }

        if ( !outHit.needsTextureCoordinates() ||
             !outHit.needsNormal() ||
             !outHit.needsTangent() ) {
            outHit.normalMap = null;
        }
        else if ( outHit.normalMap == null ) {
            outHit.normalMap = objectData.normalMap;
        }
    }

    private static Material resolveMaterial(
        RayHit hit,
        SceneObjectRenderData objectData)
    {
        if ( hit.material != null ) {
            return hit.material;
        }
        return objectData.material;
    }

    /*
    @param info.p the point of intersection
    @param info.n unit-length surface normal
    @param viewVector unit-length vector towards the ray's origin

    Note: The info datastructure must contain point and normal in world
    coordinates.

    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations. (This must be taken
    into account in the reflection and refraction calculations)

    \todo  Check the inconsistent use of tangent vector in bump mapping
    calculation... it is non sense to always be <0, 1, 0>.
    */
    private void evaluateIlluminationModel(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        SceneRenderCache sceneRenderCache,
        Background background,
        Material material,
        RenderContext renderContext,
        int recursions,
        int recursionLevel,
        TraceWorkspace workspace,
        ColorRgb outColor)
    {
        Vector3D surfaceNormal = Shader.shadeLocal(
            info,
            viewX,
            viewY,
            viewZ,
            lights,
            objects,
            material,
            renderContext,
            workspace,
            outColor);
        double surfaceNormalX = surfaceNormal.x();
        double surfaceNormalY = surfaceNormal.y();
        double surfaceNormalZ = surfaceNormal.z();

        // Compute illumination due to reflection
        double kr = material.getReflectionCoefficient();
        if ( kr > 0 && recursions > 0 ) {
            double t =
                viewX*surfaceNormalX +
                viewY*surfaceNormalY +
                viewZ*surfaceNormalZ;
            if ( t > 0 ) {
                double twoT = 2*t;
                double reflectX = twoT*surfaceNormalX - viewX;
                double reflectY = twoT*surfaceNormalY - viewY;
                double reflectZ = twoT*surfaceNormalZ - viewZ;
                Vector3D reflect = new Vector3D(reflectX, reflectY, reflectZ);
                Vector3D poffset = new Vector3D(
                    info.p.x() + VSDK.EPSILON*reflectX,
                    info.p.y() + VSDK.EPSILON*reflectY,
                    info.p.z() + VSDK.EPSILON*reflectZ);
                RaytraceStatistics.recordReflectionRay();
                Ray reflected_ray = new Ray(poffset, reflect);

                //delete reflect;
                //delete poffset;

                RayHit reflectedHit = workspace.reflectionHits[recursionLevel];
                int nearestObjectIndex =
                    selectNearestThingInRayDirection(
                        reflected_ray,
                        objects,
                        reflectedHit,
                        workspace.traversalCandidateHit);
                if ( nearestObjectIndex >= 0 ) {
                    SimpleBody nearestObject = objects.get(nearestObjectIndex);
                    SceneObjectRenderData objectData =
                        sceneRenderCache.objectData(nearestObjectIndex);
                    RayHit subInfo = workspace.shadingHits[recursionLevel + 1];
                    ColorRgb rcolor = workspace.reflectionColors[recursionLevel];
                    rcolor.r = 0;
                    rcolor.g = 0;
                    rcolor.b = 0;
                    Ray reflectedHitRay =
                        reflected_ray.withT(reflectedHit.hitDistance());

                    prepareSurfaceHit(
                        nearestObject,
                        objectData,
                        reflectedHitRay,
                        subInfo);
                    evaluateIlluminationModel(
                        subInfo,
                        -reflected_ray.direction().x(),
                        -reflected_ray.direction().y(),
                        -reflected_ray.direction().z(),
                        lights,
                        objects,
                        sceneRenderCache,
                        background,
                        resolveMaterial(subInfo, objectData),
                        renderContext,
                        recursions - 1,
                        recursionLevel + 1,
                        workspace,
                        rcolor);

                    outColor.r += kr*rcolor.r;
                    outColor.g += kr*rcolor.g;
                    outColor.b += kr*rcolor.b;
                  }
                  else {
                    ColorRgb reflectedBackground =
                        background.colorInDireccion(reflect);
                    outColor.r += kr*reflectedBackground.r;
                    outColor.g += kr*reflectedBackground.g;
                    outColor.b += kr*reflectedBackground.b;
                }
            }
        }

        // Add code for refraction here
        // <TODO>

        // Clamp outColor to MAX 1.0 intensity.
        outColor.r = (outColor.r > 1) ? 1 : outColor.r;
        outColor.g = (outColor.g > 1) ? 1 : outColor.g;
        outColor.b = (outColor.b > 1) ? 1 : outColor.b;

        //delete backgroundColor;
    }

    /**
    This method intersect the `inOut_Ray` with all of the geometries contained
    in `inSimpleBodiesArray`. If none of the geometries is intersected
    `-1` is returned, otherwise the nearest object index is returned.

    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations.
    */
    private int 
    selectNearestThingInRayDirection(
        Ray inRay,
        List<SimpleBody> inSimpleBodiesArray,
        RayHit outHit,
        RayHit candidateHit)
    {
        int i;
        int nearestObjectIndex;
        double nearestDistance;

        nearestDistance = Double.MAX_VALUE;
        nearestObjectIndex = -1;
        candidateHit.setStoreRay(false);
        RaytraceStatistics.recordSceneTraversal();
        for ( i = 0; i < inSimpleBodiesArray.size(); i++ ) {
            SimpleBody gi = inSimpleBodiesArray.get(i);
            candidateHit.resetForDistanceOnly();
            RaytraceStatistics.recordObjectIntersectionTest();
            if ( gi.doIntersection(inRay, candidateHit) ) {
                double hitDistance = candidateHit.hitDistance();
                if ( hitDistance < nearestDistance && hitDistance > VSDK.EPSILON ) {
                    nearestDistance = hitDistance;
                    nearestObjectIndex = i;
                }
            }
        }
        if ( nearestObjectIndex >= 0 && outHit != null ) {
            outHit.resetForDistanceOnly();
            outHit.setHitDistance(nearestDistance);
        }
        return nearestObjectIndex;
    }

    /**
    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations.

    Note that this method can return null, that means a transparent pixel
    should be used.
    */
    private void followRayPath(Ray inRay,
                               List<SimpleBody> inSimpleBodiesArray,
                               List<Light> inLightsArray,
                               Background in_background,
                               RenderContext renderContext,
                               SceneRenderCache sceneRenderCache,
                               TraceWorkspace workspace,
                               ColorRgb outColor)
    {
        RayHit hitInfo = workspace.nearestHit;

        int nearestObjectIndex =
            selectNearestThingInRayDirection(
                inRay,
                inSimpleBodiesArray,
                hitInfo,
                workspace.traversalCandidateHit);
        if ( nearestObjectIndex >= 0 ) {
            //------------------------------------------------------------
            SimpleBody nearestObject =
                inSimpleBodiesArray.get(nearestObjectIndex);
            SceneObjectRenderData objectData =
                sceneRenderCache.objectData(nearestObjectIndex);
            Ray primaryHitRay = inRay.withT(hitInfo.hitDistance());
            RayHit shadingInfo = workspace.shadingHits[0];
            prepareSurfaceHit(
                nearestObject,
                objectData,
                primaryHitRay,
                shadingInfo);

            evaluateIlluminationModel(
                shadingInfo,
                -inRay.direction().x(),
                -inRay.direction().y(),
                -inRay.direction().z(),
                inLightsArray,
                inSimpleBodiesArray,
                sceneRenderCache,
                in_background,
                resolveMaterial(shadingInfo, objectData),
                renderContext,
                MAX_RECURSION_LEVEL,
                0,
                workspace,
                outColor);
          }
          else {
            ColorRgb c;
            c = in_background.colorInDireccion(inRay.direction());
            outColor.r = c.r;
            outColor.g = c.g;
            outColor.b = c.b;
        }
    }

    public void execute(RGBImage inoutViewport,
                        RendererConfiguration inQualitySelection,
                        SimpleSceneSnapshot sceneSnapshot,
                        ProgressMonitor report)
    {
        execute(inoutViewport, inQualitySelection,
                sceneSnapshot, report, null, 0, 0,
                inoutViewport.getXSize(), inoutViewport.getYSize());
    }

    public void execute(RGBImage inoutViewport,
                        RendererConfiguration inQualitySelection,
                        SimpleSceneSnapshot sceneSnapshot,
                        ProgressMonitor report,
                        ZBuffer depthmap)
    {
        execute(inoutViewport, inQualitySelection, sceneSnapshot,
                report, depthmap, 0, 0,
                inoutViewport.getXSize(), inoutViewport.getYSize());
    }

    public void execute(RGBImage inoutViewport,
                        RendererConfiguration inQualitySelection,
                        SimpleSceneSnapshot sceneSnapshot,
                        ProgressMonitor liveReport,
                        ZBuffer outDepthmap,
                        int limx1, int limy1,
                        int limx2, int limy2)
    {
        execute(inoutViewport, inQualitySelection,
                sceneSnapshot.getSimpleBodies(),
                sceneSnapshot.getLights(),
                sceneSnapshot.getBackground(),
                sceneSnapshot.getCameraSnapshot(),
                liveReport,
                outDepthmap,
                limx1, limy1,
                limx2, limy2);
    }

    /**
    Macroalgoritmo de control para raytracing. Este m&eacute;todo recibe
    el modelo de una escena 3D previamente construida en memoria y una
    imagen, y modifica la imagen de tal forma que contiene una visualizacion
    de la escena, result de aplicar la t&eacute;cnica de raytracing.

    PARAMETERS
    - `inout_viewport`: imagen RGB en donde el algoritmo calcular&aacute; su
       result.
    - `inSimpleBodiesArray`: arreglo din&aacute;mico de SimpleBodys que constituyen los
       objetos visibles de la escena.
    - `inLightsArray`: arreglo din&aacute;mico de Light'es (luces puntuales)
    - `in_background`: especificaci&oacute;n de un color de fondo para la escena
      (i.e. el color que se ve si no se ve ning&uacute;n objeto!)
    - `cameraSnapshot`: especificaci&oacute;n de la transformaci&oacute;n de
      proyecci&oacute;n 3D a 2D que se lleva a cabo en el proceso de 
      visualizaci&oacute;n.
    - `depthmap`: can be null or a reference to a ZBuffer. If it is null,
      nothing is done with this parameter. If it is not null, the associated
      ZBuffer is filled with depth values corresponding to distances 
      calculated in world space coordinates from ray intersections.
      Note that depth values are not scaled neither clamped to any specific
      range, so post-processing should be done if wanting to combine that
      with other depth maps, as those generated from OpenGL's ZBuffer.
    - `liveReport` can be null. In that case no report is updated.


    PRE:
    - Todas las referencias estan creadas, asi sea que apunten a estructuras
      vac&iacute;as.
    - La imagen `inout_viewport` esta creada, y es de el tama&ntilde;o que
      el usuario desea para su visualizaci&oacute;n.
    - In the case the ZBuffer depthmap is not null, the ZBuffer must be
      initialized to the same size of the image inoutViewport.

    POST:
    - `inout_viewport` contiene una representaci&oacute;n visual de la
       escena 3D (`inSimpleBodiesArray`, `inLightsArray`, `in_background`), tal que corresponde a
       una proyecci&oacute;n 3D a 2D controlada por la c&aacute;mara
       virtual congelada en `cameraSnapshot`.

    NOTA: Este algoritmo se inici&oacute; como una modificaci&oacute;n del 
          raytracer del curso 6.837 (computaci&oacute;n gr&aacute;fica) de MIT,
          original de Leonard McMillan y Tomas Lozano Perez, pero puede 
          considerarse que es una re-escritura y re-estructuraci&oacute;n 
          completa de Oscar Chavarro.
    */
    private void execute(RGBImage inoutViewport,
                         RendererConfiguration inQualitySelection,
                         List<SimpleBody> inSimpleBodiesArray,
                         List<Light> inLightsArray,
                         Background inBackground,
                         CameraSnapshot cameraSnapshot,
                         ProgressMonitor liveReport,
                         ZBuffer outDepthmap,
                         int limx1, int limy1,
                         int limx2, int limy2)
    {
        int x, y;
        Ray rayo;
        ColorRgb color = new ColorRgb();
        RGBPixel outputPixel = new RGBPixel();
        RenderContext renderContext =
            buildRenderContext(inQualitySelection, inLightsArray);
        SceneRenderCache sceneRenderCache =
            new SceneRenderCache(inSimpleBodiesArray, renderContext);
        TraceWorkspace workspace = traceWorkspace.get();
        long[] initialBodyVersions = captureBodyVersions(inSimpleBodiesArray);
        TileGenerator tileGenerator = new TileGenerator(
            TILE_STRATEGY,
            inoutViewport,
            limx1, limy1,
            limx2 - limx1, limy2 - limy1,
            TILE_WORKERS_HINT);
        ConcurrentLinkedQueue<Tile> pendingTiles =
            new ConcurrentLinkedQueue<Tile>(tileGenerator.getTiles());
        Tile tile;

        if ( liveReport != null ) {
            liveReport.begin();
        }
        while ( (tile = pendingTiles.poll()) != null ) {
            Image tileImage = tile.getImage();
            int tileX0 = tile.getX0();
            int tileY0 = tile.getY0();
            int tileX1 = tileX0 + tile.getDx();
            int tileY1 = tileY0 + tile.getDy();

            for ( y = tileY0; y < tileY1; y++ ) {
                assertSceneUnmodifiedDuringRender(
                    initialBodyVersions,
                    inSimpleBodiesArray);
                if ( liveReport != null ) {
                    liveReport.update(0, inoutViewport.getYSize(), y);
                }
                for ( x = tileX0; x < tileX1; x++ ) {
                    //- Trazado individual de un rayo --------------------------
                    RaytraceStatistics.recordPrimaryRay();
                    rayo = generateRay(cameraSnapshot, x, y);
                    color.r = 0;
                    color.g = 0;
                    color.b = 0;
                    followRayPath(rayo, inSimpleBodiesArray,
                                  inLightsArray, inBackground, 
                                  renderContext, sceneRenderCache, workspace, color);
                    if ( outDepthmap != null ) {
                        outDepthmap.setZ(x, y, (float)rayo.t());
                    }
                    //- Exporto el result de color del pixel ----------------
                    outputPixel.r = (byte)(255 * color.r);
                    outputPixel.g = (byte)(255 * color.g);
                    outputPixel.b = (byte)(255 * color.b);
                    tileImage.putPixelRgb(x, y, outputPixel);
                }
            }
        }
        //delete color;
        //delete ray;

        if ( liveReport != null ) {
            liveReport.end();
        }
    }

}
