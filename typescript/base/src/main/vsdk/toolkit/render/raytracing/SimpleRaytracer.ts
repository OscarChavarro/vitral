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

import { ConcurrentLinkedQueue } from "../../../../java/util/concurrent/ConcurrentLinkedQueue.js";
import { Double } from "../../../../java/lang/Double.js";
import { IllegalStateException } from "../../../../java/lang/IllegalStateException.js";
import type { List } from "../../../../java/util/List.js";
import { ThreadLocal } from "../../../../java/lang/ThreadLocal.js";

import { RaytraceStatistics } from "../../common/statistics/RaytraceStatistics.js";
import { VSDK } from "../../common/VSDK.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Ray } from "../../environment/geometry/element/Ray.js";
import { RendererConfiguration } from "../../environment/material/RendererConfiguration.js";
import type { Image } from "../../media/Image.js";
import type { NormalMap } from "../../media/NormalMap.js";
import { RGBPixel } from "../../media/RGBPixel.js";
import type { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import type { ZBuffer } from "../../media/ZBuffer.js";
import { Camera } from "../../environment/camera/Camera.js";
import type { CameraSnapshot } from "../../environment/camera/CameraSnapshot.js";
import type { Light } from "../../environment/light/Light.js";
import type { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import type { Background } from "../../environment/background/Background.js";
import { RayHit } from "../../environment/geometry/element/RayHit.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { SimpleSceneSnapshot } from "../../environment/scene/SimpleSceneSnapshot.js";
import type { ProgressMonitor } from "../../gui/feedback/ProgressMonitor.js";
import { RenderingElement } from "../RenderingElement.js";
import { TraceWorkspace } from "../TraceWorkspace.js";
import type { Shader } from "../shaders/Shader.js";
import { ShaderSelector } from "../shaders/ShaderSelector.js";
import { RasterTileArea } from "./RasterTileArea.js";
import { RasterTileGenerationStrategy } from "./RasterTileGenerationStrategy.js";
import { RasterTileGenerator } from "./RasterTileGenerator.js";
import { RenderContext } from "./RenderContext.js";

/** Port of the Java private static nested class `SimpleRaytracer.SceneObjectRenderData`. */
class _SceneObjectRenderData {
    public constructor(
        public readonly material: SimpleMaterial | null,
        public readonly texture: Image | null,
        public readonly normalMap: NormalMap | null,
        public readonly detailMask: number,
    ) {}
}

/** Port of the Java private static nested class `SimpleRaytracer.SceneRenderCache`. */
class _SceneRenderCache {
    public readonly objects: _SceneObjectRenderData[];

    public constructor(bodies: List<SimpleBody>, renderContext: RenderContext) {
        this.objects = new Array<_SceneObjectRenderData>(bodies.size());
        for (let i = 0; i < bodies.size(); i++) {
            const body: SimpleBody = bodies.get(i);
            const material: SimpleMaterial | null = body.getMaterial();
            const texture: Image | null = renderContext.textureEnabled ? body.getTexture() : null;
            const normalMap: NormalMap | null = renderContext.bumpMappingEnabled ? body.getNormalMap() : null;
            const detailMask: number = SimpleRaytracer.buildSurfaceDetailMask(
                material,
                texture,
                normalMap,
                renderContext,
            );
            this.objects[i] = new _SceneObjectRenderData(material, texture, normalMap, detailMask);
        }
    }

    public objectData(objectIndex: number): _SceneObjectRenderData {
        return this.objects[objectIndex]!;
    }
}

/**
This class provides an encaptulation for a rendering algorithm,
implementing simple recursive raytracing as presented in [WHIT1980].
Includes a normal perturbation for the simulation of wrinkled surfaces,
as described in [BLIN1978b].
This class is appropiate to play a role of "concrete strategy" in
a "Strategy" design pattern.
*/
export class SimpleRaytracer extends RenderingElement {
    private static readonly TINY = 0.0001;
    private static readonly MAX_RECURSION_LEVEL = TraceWorkspace.DEFAULT_MAX_RECURSION_LEVEL;
    private static readonly TILE_STRATEGY = RasterTileGenerationStrategy.SERIAL;
    private static readonly TILE_WORKERS_HINT = 1;

    private readonly traceWorkspace: ThreadLocal<TraceWorkspace> = new ThreadLocal<TraceWorkspace>(
        () => new TraceWorkspace(SimpleRaytracer.MAX_RECURSION_LEVEL),
    );

    public constructor() {
        // No mutable shared temporaries: keep method-level variables for reentrancy.
        super();
    }

    private static hasNonAmbientLights(lights: List<Light>): boolean {
        for (let i = 0; i < lights.size(); i++) {
            if (!lights.get(i).isAmbient()) {
                return true;
            }
        }
        return false;
    }

    private static isReflective(material: SimpleMaterial | null): boolean {
        return material !== null && material.getReflectionCoefficient() > 0;
    }

    private static buildRenderContext(qualitySelection: RendererConfiguration, lights: List<Light>): RenderContext {
        const localShader: Shader = ShaderSelector.select(qualitySelection);
        const localLightingEnabled: boolean =
            qualitySelection.getShadingType() !== RendererConfiguration.SHADING_TYPE_NOLIGHT &&
            SimpleRaytracer.hasNonAmbientLights(lights);

        return new RenderContext(
            localLightingEnabled,
            qualitySelection.isTextureSet(),
            qualitySelection.isBumpMapSet(),
            localShader,
        );
    }

    public static buildSurfaceDetailMask(
        material: SimpleMaterial | null,
        texture: Image | null,
        normalMap: NormalMap | null,
        renderContext: RenderContext,
    ): number {
        const objectReflective: boolean = SimpleRaytracer.isReflective(material);
        const needsPoint: boolean = renderContext.localLightingEnabled || objectReflective;
        const needsNormal: boolean = needsPoint;
        const needsUv: boolean =
            (renderContext.textureEnabled && texture !== null) ||
            ((renderContext.localLightingEnabled || objectReflective) && normalMap !== null);
        const needsTangent: boolean = (renderContext.localLightingEnabled || objectReflective) && normalMap !== null;
        let detailMask: number = RayHit.DETAIL_NONE;

        if (needsPoint) {
            detailMask |= RayHit.DETAIL_POINT;
        }
        if (needsNormal) {
            detailMask |= RayHit.DETAIL_NORMAL;
        }
        if (needsUv) {
            detailMask |= RayHit.DETAIL_UV;
        }
        if (needsTangent) {
            detailMask |= RayHit.DETAIL_TANGENT;
        }
        return detailMask;
    }

    private static captureBodyVersions(bodies: List<SimpleBody>): number[] {
        const versions: number[] = new Array<number>(bodies.size());
        for (let i = 0; i < bodies.size(); i++) {
            versions[i] = bodies.get(i).getModificationVersion();
        }
        return versions;
    }

    private static assertSceneUnmodifiedDuringRender(expectedBodyVersions: number[], bodies: List<SimpleBody>): void {
        if (expectedBodyVersions.length !== bodies.size()) {
            throw new IllegalStateException(
                "Scene bodies list changed while raytracing. " + "Freeze scene edits during SimpleRaytracer.execute.",
            );
        }
        for (let i = 0; i < bodies.size(); i++) {
            if (bodies.get(i).getModificationVersion() !== expectedBodyVersions[i]) {
                throw new IllegalStateException(
                    "SimpleBody at index " +
                        i +
                        " was modified while raytracing. " +
                        "Freeze scene/body edits during SimpleRaytracer.execute.",
                );
            }
        }
    }

    private static generateRay(cameraSnapshot: CameraSnapshot, x: number, y: number): Ray {
        const viewportXSize: number = cameraSnapshot.getViewportXSize();
        const viewportYSize: number = cameraSnapshot.getViewportYSize();
        const pixelCenterX: number = x + 0.5;
        const pixelCenterY: number = y + 0.5;
        const u: number = (pixelCenterX - viewportXSize / 2.0) / viewportXSize;
        const v: number = (viewportYSize - pixelCenterY - viewportYSize / 2.0) / viewportYSize;

        if (cameraSnapshot.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            const left: Vector3Dd = cameraSnapshot.getLeft();
            const up: Vector3Dd = cameraSnapshot.getUp();
            const front: Vector3Dd = cameraSnapshot.getFront();
            const eyePosition: Vector3Dd = cameraSnapshot.getEyePosition();
            const fovFactor: number = viewportXSize / viewportYSize;
            const duScale: number = -fovFactor * ((2 * u) / cameraSnapshot.getOrthogonalZoom());
            const dvScale: number = (2 * v) / cameraSnapshot.getOrthogonalZoom();
            const origin: Vector3Dd = new Vector3Dd(
                eyePosition.x() + left.x() * duScale + up.x() * dvScale,
                eyePosition.y() + left.y() * duScale + up.y() * dvScale,
                eyePosition.z() + left.z() * duScale + up.z() * dvScale,
            );
            return new Ray(origin, front);
        }

        const rightWithScale: Vector3Dd = cameraSnapshot.getRightWithScale();
        const upWithScale: Vector3Dd = cameraSnapshot.getUpWithScale();
        const dir: Vector3Dd = cameraSnapshot.getDir();
        const direction: Vector3Dd = new Vector3Dd(
            rightWithScale.x() * u + upWithScale.x() * v + dir.x(),
            rightWithScale.y() * u + upWithScale.y() * v + dir.y(),
            rightWithScale.z() * u + upWithScale.z() * v + dir.z(),
        );

        return new Ray(cameraSnapshot.getEyePosition(), direction);
    }

    private prepareSurfaceHit(
        nearestObject: SimpleBody,
        objectData: _SceneObjectRenderData,
        hitRay: Ray,
        outHit: RayHit,
    ): void {
        const detailMask: number = objectData.detailMask;

        outHit.reset(detailMask);
        outHit.setRay(hitRay);
        if (detailMask !== RayHit.DETAIL_NONE) {
            nearestObject.doExtraInformation(hitRay, hitRay.getT(), outHit);
            outHit.setRay(hitRay);
        }

        if (!outHit.needsTextureCoordinates()) {
            outHit.texture = null;
        } else if (outHit.texture === null) {
            outHit.texture = objectData.texture;
        }

        if (!outHit.needsTextureCoordinates() || !outHit.needsNormal() || !outHit.needsTangent()) {
            outHit.normalMap = null;
        } else if (outHit.normalMap === null) {
            outHit.normalMap = objectData.normalMap;
        }
    }

    private static resolveMaterial(hit: RayHit, objectData: _SceneObjectRenderData): SimpleMaterial | null {
        if (hit.material !== null) {
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
    private evaluateIlluminationModel(
        info: RayHit,
        viewX: number,
        viewY: number,
        viewZ: number,
        lights: List<Light>,
        objects: List<SimpleBody>,
        sceneRenderCache: _SceneRenderCache,
        background: Background,
        material: SimpleMaterial,
        renderContext: RenderContext,
        recursions: number,
        recursionLevel: number,
        workspace: TraceWorkspace,
    ): ColorRgb {
        const localShading: Shader.LocalShadingResult = renderContext.localShader.shadeLocal(
            info,
            viewX,
            viewY,
            viewZ,
            lights,
            objects,
            material,
            workspace,
        );
        const surfaceNormal: Vector3Dd = localShading.normal();
        const localColor: ColorRgb = localShading.color();
        let outR: number = localColor.r();
        let outG: number = localColor.g();
        let outB: number = localColor.b();
        const surfaceNormalX: number = surfaceNormal.x();
        const surfaceNormalY: number = surfaceNormal.y();
        const surfaceNormalZ: number = surfaceNormal.z();

        // Compute illumination due to reflection
        const kr: number = material.getReflectionCoefficient();
        if (kr > 0 && recursions > 0) {
            const t: number = viewX * surfaceNormalX + viewY * surfaceNormalY + viewZ * surfaceNormalZ;
            if (t > 0) {
                const twoT: number = 2 * t;
                const reflectX: number = twoT * surfaceNormalX - viewX;
                const reflectY: number = twoT * surfaceNormalY - viewY;
                const reflectZ: number = twoT * surfaceNormalZ - viewZ;
                const reflect: Vector3Dd = new Vector3Dd(reflectX, reflectY, reflectZ);
                const poffset: Vector3Dd = new Vector3Dd(
                    info.p.x() + VSDK.EPSILON * reflectX,
                    info.p.y() + VSDK.EPSILON * reflectY,
                    info.p.z() + VSDK.EPSILON * reflectZ,
                );
                RaytraceStatistics.recordReflectionRay();
                const reflected_ray: Ray = new Ray(poffset, reflect);

                //delete reflect;
                //delete poffset;

                const reflectedHit: RayHit = workspace.reflectionHits[recursionLevel]!;
                const nearestObjectIndex: number = SimpleRaytracer.selectNearestThingInRayDirection(
                    reflected_ray,
                    objects,
                    reflectedHit,
                    workspace.traversalCandidateHit,
                );
                if (nearestObjectIndex >= 0) {
                    const nearestObject: SimpleBody = objects.get(nearestObjectIndex);
                    const objectData: _SceneObjectRenderData = sceneRenderCache.objectData(nearestObjectIndex);
                    const subInfo: RayHit = workspace.shadingHits[recursionLevel + 1]!;
                    const reflectedHitRay: Ray = reflected_ray.withT(reflectedHit.hitDistance());

                    this.prepareSurfaceHit(nearestObject, objectData, reflectedHitRay, subInfo);
                    const rcolor: ColorRgb = this.evaluateIlluminationModel(
                        subInfo,
                        -reflected_ray.getDirection().x(),
                        -reflected_ray.getDirection().y(),
                        -reflected_ray.getDirection().z(),
                        lights,
                        objects,
                        sceneRenderCache,
                        background,
                        SimpleRaytracer.resolveMaterial(subInfo, objectData)!,
                        renderContext,
                        recursions - 1,
                        recursionLevel + 1,
                        workspace,
                    );

                    outR += rcolor.r() * kr;
                    outG += rcolor.g() * kr;
                    outB += rcolor.b() * kr;
                } else {
                    // `Background.colorInDireccion` is declared to return a
                    // `ColorRgb` in Java but `FixedBackground` answers null;
                    // Java then throws a NullPointerException right here, and
                    // the non-null assertion reproduces that failure point.
                    const reflectedBackground: ColorRgb = background.colorInDireccion(reflect)!;
                    outR += reflectedBackground.r() * kr;
                    outG += reflectedBackground.g() * kr;
                    outB += reflectedBackground.b() * kr;
                }
            }
        }

        // Add code for refraction here
        // <TODO>

        // Clamp outColor to MAX 1.0 intensity.
        return new ColorRgb(outR > 1 ? 1 : outR, outG > 1 ? 1 : outG, outB > 1 ? 1 : outB);
    }

    /**
    This method intersect the `inOut_Ray` with all of the geometries contained
    in `inSimpleBodiesArray`. If none of the geometries is intersected
    `-1` is returned, otherwise the nearest object index is returned.

    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations.
    */
    private static selectNearestThingInRayDirection(
        inRay: Ray,
        inSimpleBodiesArray: List<SimpleBody>,
        outHit: RayHit | null,
        candidateHit: RayHit,
    ): number {
        let i: number;
        let nearestObjectIndex: number;
        let nearestDistance: number;

        nearestDistance = Double.MAX_VALUE;
        nearestObjectIndex = -1;
        candidateHit.setStoreRay(false);
        RaytraceStatistics.recordSceneTraversal();
        for (i = 0; i < inSimpleBodiesArray.size(); i++) {
            const gi: SimpleBody = inSimpleBodiesArray.get(i);
            candidateHit.resetForDistanceOnly();
            RaytraceStatistics.recordObjectIntersectionTest();
            if (gi.doIntersectionFirstHit(inRay, candidateHit)) {
                const hitDistance: number = candidateHit.hitDistance();
                if (hitDistance < nearestDistance && hitDistance > VSDK.EPSILON) {
                    nearestDistance = hitDistance;
                    nearestObjectIndex = i;
                }
            }
        }
        if (nearestObjectIndex >= 0 && outHit !== null) {
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
    private followRayPath(
        inRay: Ray,
        inSimpleBodiesArray: List<SimpleBody>,
        inLightsArray: List<Light>,
        in_background: Background,
        renderContext: RenderContext,
        sceneRenderCache: _SceneRenderCache,
        workspace: TraceWorkspace,
    ): ColorRgb {
        const hitInfo: RayHit = workspace.nearestHit;

        const nearestObjectIndex: number = SimpleRaytracer.selectNearestThingInRayDirection(
            inRay,
            inSimpleBodiesArray,
            hitInfo,
            workspace.traversalCandidateHit,
        );
        if (nearestObjectIndex >= 0) {
            //------------------------------------------------------------
            const nearestObject: SimpleBody = inSimpleBodiesArray.get(nearestObjectIndex);
            const objectData: _SceneObjectRenderData = sceneRenderCache.objectData(nearestObjectIndex);
            const primaryHitRay: Ray = inRay.withT(hitInfo.hitDistance());
            const shadingInfo: RayHit = workspace.shadingHits[0]!;
            this.prepareSurfaceHit(nearestObject, objectData, primaryHitRay, shadingInfo);

            return this.evaluateIlluminationModel(
                shadingInfo,
                -inRay.getDirection().x(),
                -inRay.getDirection().y(),
                -inRay.getDirection().z(),
                inLightsArray,
                inSimpleBodiesArray,
                sceneRenderCache,
                in_background,
                SimpleRaytracer.resolveMaterial(shadingInfo, objectData)!,
                renderContext,
                SimpleRaytracer.MAX_RECURSION_LEVEL,
                0,
                workspace,
            );
        } else {
            // See the note in evaluateIlluminationModel: a null here is the
            // Java NullPointerException of a FixedBackground scene.
            return in_background.colorInDireccion(inRay.getDirection())!;
        }
    }

    public execute(
        inoutViewport: RGBImageUncompressed,
        inQualitySelection: RendererConfiguration,
        sceneSnapshot: SimpleSceneSnapshot,
        report: ProgressMonitor | null,
    ): void;
    public execute(
        inoutViewport: RGBImageUncompressed,
        inQualitySelection: RendererConfiguration,
        sceneSnapshot: SimpleSceneSnapshot,
        report: ProgressMonitor | null,
        depthmap: ZBuffer | null,
    ): void;
    public execute(
        inoutViewport: RGBImageUncompressed,
        inQualitySelection: RendererConfiguration,
        sceneSnapshot: SimpleSceneSnapshot,
        liveReport: ProgressMonitor | null,
        outDepthmap: ZBuffer | null,
        limx1: number,
        limy1: number,
        limx2: number,
        limy2: number,
    ): void;
    public execute(
        inoutViewport: RGBImageUncompressed,
        inQualitySelection: RendererConfiguration,
        sceneSnapshot: SimpleSceneSnapshot,
        liveReport: ProgressMonitor | null,
        outDepthmap?: ZBuffer | null,
        limx1?: number,
        limy1?: number,
        limx2?: number,
        limy2?: number,
    ): void {
        this.executeInternal(
            inoutViewport,
            inQualitySelection,
            sceneSnapshot.getSimpleBodies(),
            sceneSnapshot.getLights(),
            sceneSnapshot.getBackground(),
            sceneSnapshot.getCameraSnapshot(),
            liveReport,
            outDepthmap === undefined ? null : outDepthmap,
            limx1 === undefined ? 0 : limx1,
            limy1 === undefined ? 0 : limy1,
            limx2 === undefined ? inoutViewport.getXSize() : limx2,
            limy2 === undefined ? inoutViewport.getYSize() : limy2,
        );
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
    private executeInternal(
        inoutViewport: RGBImageUncompressed,
        inQualitySelection: RendererConfiguration,
        inSimpleBodiesArray: List<SimpleBody>,
        inLightsArray: List<Light>,
        inBackground: Background,
        cameraSnapshot: CameraSnapshot,
        liveReport: ProgressMonitor | null,
        outDepthmap: ZBuffer | null,
        limx1: number,
        limy1: number,
        limx2: number,
        limy2: number,
    ): void {
        let x: number, y: number;
        let rayo: Ray;
        let color: ColorRgb;
        const outputPixel: RGBPixel = new RGBPixel();
        const renderContext: RenderContext = SimpleRaytracer.buildRenderContext(inQualitySelection, inLightsArray);
        const sceneRenderCache: _SceneRenderCache = new _SceneRenderCache(inSimpleBodiesArray, renderContext);
        const workspace: TraceWorkspace = this.traceWorkspace.get()!;
        const initialBodyVersions: number[] = SimpleRaytracer.captureBodyVersions(inSimpleBodiesArray);
        const tileGenerator: RasterTileGenerator = new RasterTileGenerator(
            SimpleRaytracer.TILE_STRATEGY,
            inoutViewport,
            limx1,
            limy1,
            limx2 - limx1,
            limy2 - limy1,
            SimpleRaytracer.TILE_WORKERS_HINT,
        );
        const pendingTiles: ConcurrentLinkedQueue<RasterTileArea> = new ConcurrentLinkedQueue<RasterTileArea>(
            tileGenerator.getTiles(),
        );
        let tile: RasterTileArea | undefined;

        if (liveReport !== null) {
            liveReport.begin();
        }
        while ((tile = pendingTiles.poll()) !== undefined) {
            const tileImage: Image = tile.getImage();
            const tileX0: number = tile.getX0();
            const tileY0: number = tile.getY0();
            const tileX1: number = tileX0 + tile.getDx();
            const tileY1: number = tileY0 + tile.getDy();

            for (y = tileY0; y < tileY1; y++) {
                SimpleRaytracer.assertSceneUnmodifiedDuringRender(initialBodyVersions, inSimpleBodiesArray);
                if (liveReport !== null) {
                    liveReport.update(0, inoutViewport.getYSize(), y);
                }
                for (x = tileX0; x < tileX1; x++) {
                    //- Trazado individual de un rayo --------------------------
                    RaytraceStatistics.recordPrimaryRay();
                    rayo = SimpleRaytracer.generateRay(cameraSnapshot, x, y);
                    color = this.followRayPath(
                        rayo,
                        inSimpleBodiesArray,
                        inLightsArray,
                        inBackground,
                        renderContext,
                        sceneRenderCache,
                        workspace,
                    );
                    if (outDepthmap !== null) {
                        outDepthmap.setZ(x, y, Math.fround(rayo.getT()));
                    }
                    //- Exporto el result de color del pixel ----------------
                    // `(byte)` narrowing of a double in Java: truncate towards
                    // zero, then keep the low 8 bits as a signed value.
                    outputPixel.r = (Math.trunc(255 * color.r()) << 24) >> 24;
                    outputPixel.g = (Math.trunc(255 * color.g()) << 24) >> 24;
                    outputPixel.b = (Math.trunc(255 * color.b()) << 24) >> 24;
                    tileImage.putPixelRgb(x, y, outputPixel);
                }
            }
        }
        //delete color;
        //delete ray;

        if (liveReport !== null) {
            liveReport.end();
        }
    }
}
