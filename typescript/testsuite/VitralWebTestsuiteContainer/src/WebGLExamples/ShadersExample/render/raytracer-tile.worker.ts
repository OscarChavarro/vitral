/*
Deep module specifiers rather than the `@vitral/base` barrel, for the reason
the Node tile worker of `RaytracingOfflineExample` records: a worker that
pulls the whole barrel pays for compiling every module in it, once per worker.
*/
import { createWorkerMessageHandler } from '@vitral/base/java/concurrent/WorkerProtocol';
import { ArrayList } from '@vitral/base/java/util/ArrayList';
import { AmbientLight } from '@vitral/base/vsdk/toolkit/environment/light/AmbientLight';
import { Camera } from '@vitral/base/vsdk/toolkit/environment/camera/Camera';
import { ColorRgb } from '@vitral/base/vsdk/toolkit/common/color/ColorRgb';
import { IndexedColorImageUncompressed } from '@vitral/base/vsdk/toolkit/media/IndexedColorImageUncompressed';
import { GrayScalePalette } from '@vitral/base/vsdk/toolkit/media/GrayScalePalette';
import { Matrix4x4d } from '@vitral/base/vsdk/toolkit/common/linealAlgebra/Matrix4x4d';
import { MicroFacetedMaterial } from '@vitral/base/vsdk/toolkit/environment/material/MicroFacetedMaterial';
import { NormalMap } from '@vitral/base/vsdk/toolkit/media/NormalMap';
import { PointLight } from '@vitral/base/vsdk/toolkit/environment/light/PointLight';
import { RendererConfiguration } from '@vitral/base/vsdk/toolkit/environment/material/RendererConfiguration';
import { RGBImageUncompressed } from '@vitral/base/vsdk/toolkit/media/RGBImageUncompressed';
import { SimpleBackground } from '@vitral/base/vsdk/toolkit/environment/background/SimpleBackground';
import { SimpleBody } from '@vitral/base/vsdk/toolkit/environment/scene/SimpleBody';
import { SimpleMaterial } from '@vitral/base/vsdk/toolkit/environment/material/SimpleMaterial';
import { SimpleRaytracer } from '@vitral/base/vsdk/toolkit/render/raytracing/SimpleRaytracer';
import { SimpleSceneSnapshot } from '@vitral/base/vsdk/toolkit/environment/scene/SimpleSceneSnapshot';
import { Sphere } from '@vitral/base/vsdk/toolkit/environment/geometry/volume/Sphere';
import { Vector3Dd } from '@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd';
import type { Light } from '@vitral/base/vsdk/toolkit/environment/light/Light';
// The barrel installs these `RendererConfiguration` methods as a side effect;
// a worker that does not load the barrel must ask for them explicitly.
import '@vitral/base/vsdk/toolkit/environment/material/RendererConfigurationBehavior';
import type { ShadersTileRequest, ShadersTileResult } from './software-raycaster-protocol';

/**
 * Worker-side half of `render.SoftwareRaycaster`.
 *
 * Java's `SoftwareRaycaster.TileWorker` is a `Callable<Void>` on a thread of
 * the same JVM: every worker shares the one scene snapshot and writes straight
 * into the one `RGBImageUncompressed`. A Web Worker shares no heap, so this
 * module rebuilds the snapshot from a description of it and returns the bytes
 * of the band it rendered, which the owner splices into the result. The pixels
 * are the same either way, because a tile only ever reads the scene and only
 * ever writes its own rows, which is exactly why Java can let its threads share
 * the image in the first place.
 *
 * The scene description is rebuilt into the very objects Java's
 * `buildSceneSnapshot` creates, in its order: the sphere body carrying a copy
 * of the active material, the texture and the bump normal map and the model
 * rotation; the ambient light with id 0 and a copy of the point light with id
 * 1; and a black `SimpleBackground`. The heavy parts — the texture, the bump
 * map and the normal map built from it — are rebuilt only when the resources
 * change, since they are the same on every frame.
 */
interface LoadedResources {
  readonly generation: number;
  readonly textureMap: RGBImageUncompressed;
  readonly normalMap: NormalMap;
}

let loaded: LoadedResources | undefined;

function loadResources(request: ShadersTileRequest): LoadedResources {
  if (loaded !== undefined && loaded.generation === request.generation) {
    return loaded;
  }

  const textureMap = new RGBImageUncompressed();
  textureMap.init(request.textureWidth, request.textureHeight);
  textureMap.getRawImageDirectBuffer().set(request.textureBytes);

  const bumpMap = new IndexedColorImageUncompressed(new GrayScalePalette());
  bumpMap.init(request.bumpWidth, request.bumpHeight);
  bumpMap
    .getRawImage()
    .set(
      new Int8Array(
        request.bumpBytes.buffer,
        request.bumpBytes.byteOffset,
        request.bumpBytes.length,
      ),
    );

  const normalMap = new NormalMap();
  normalMap.importBumpMap(bumpMap, new Vector3Dd(1.0, 1.0, 1.0));

  loaded = { generation: request.generation, textureMap, normalMap };
  return loaded;
}

function buildSceneSnapshot(
  request: ShadersTileRequest,
  resources: LoadedResources,
): SimpleSceneSnapshot {
  const camera = new Camera();
  camera.setPosition(
    new Vector3Dd(
      request.cameraPosition[0]!,
      request.cameraPosition[1]!,
      request.cameraPosition[2]!,
    ),
  );
  camera.setRotation(matrixFromRowOrder(request.cameraRotation));
  camera.setFov(request.cameraFov);
  camera.updateViewportResize(request.width, request.height);
  const cameraSnapshot = camera.exportToCameraSnapshot(request.width, request.height);

  const sphereBody = new SimpleBody();
  sphereBody.setGeometry(new Sphere(request.sphereRadius));
  const activeMaterial: SimpleMaterial = buildActiveMaterial(request);
  if (activeMaterial instanceof MicroFacetedMaterial) {
    sphereBody.setMaterial(new MicroFacetedMaterial(activeMaterial));
  } else {
    sphereBody.setMaterial(new SimpleMaterial(activeMaterial));
  }
  sphereBody.setTexture(resources.textureMap);
  sphereBody.setNormalMap(resources.normalMap);
  sphereBody.setRotation(matrixFromRowOrder(request.modelRotation));

  const bodies = new ArrayList<SimpleBody>(1);
  bodies.add(sphereBody);

  const lights = new ArrayList<Light>(2);
  const ambientLight: Light = new AmbientLight(new ColorRgb(1, 1, 1));
  ambientLight.setId(0);
  lights.add(ambientLight);
  const pointLight: Light = new PointLight(
    new Vector3Dd(request.lightPosition[0]!, request.lightPosition[1]!, request.lightPosition[2]!),
    new ColorRgb(request.lightColor[0]!, request.lightColor[1]!, request.lightColor[2]!),
  ).copy();
  pointLight.setId(1);
  lights.add(pointLight);

  const background = new SimpleBackground();
  background.setColor(0, 0, 0);

  return new SimpleSceneSnapshot(bodies, lights, background, cameraSnapshot);
}

/** `ShadersModel.getActiveMaterialForCurrentShading`, on the worker's side. */
function buildActiveMaterial(request: ShadersTileRequest): SimpleMaterial {
  if (request.microFacetMaterialName !== null) {
    return MicroFacetedMaterial.fromCsvText(
      request.microFacetCsvText,
      request.microFacetMaterialName,
      request.microFacetCsvName,
    );
  }
  let material = new SimpleMaterial();
  material = material.withAmbient(new ColorRgb(0.1, 0.1, 0.1));
  material = material.withDiffuse(new ColorRgb(1, 1, 1));
  material = material.withSpecular(new ColorRgb(1, 1, 1));
  material = material.withPhongExponent(40);
  return material;
}

function matrixFromRowOrder(values: Float64Array): Matrix4x4d {
  const rows: number[][] = [];
  for (let row = 0; row < 4; row++) {
    const r: number[] = [];
    for (let column = 0; column < 4; column++) {
      r.push(values[row * 4 + column]!);
    }
    rows.push(r);
  }
  return new Matrix4x4d(rows);
}

const handler = createWorkerMessageHandler<ShadersTileRequest, ShadersTileResult>(
  (request) => {
    const resources: LoadedResources = loadResources(request);
    const snapshot: SimpleSceneSnapshot = buildSceneSnapshot(request, resources);

    const quality = new RendererConfiguration();
    quality.setTexture(request.qualityTexture);
    quality.setBumpMap(request.qualityBumpMap);
    quality.setShadingType(request.qualityShadingType);

    const image = new RGBImageUncompressed();
    image.init(request.width, request.height);

    new SimpleRaytracer().execute(
      image,
      quality,
      snapshot,
      null,
      null,
      request.x0,
      request.y0,
      request.x0 + request.dx,
      request.y0 + request.dy,
    );

    // `RGBImageUncompressed` stores row `y` at `(ySize - 1 - y) * rowStride`,
    // so a full-width band is one contiguous byte range. `RasterTileGenerator`
    // in LINEAR mode only ever produces full-width bands, which the owner
    // checks before handing a tile over.
    const rowStride: number = request.width * 3;
    const raw: Uint8Array = image.getRawImageDirectBuffer();
    const start: number = (request.height - (request.y0 + request.dy)) * rowStride;
    const end: number = (request.height - request.y0) * rowStride;

    return { y0: request.y0, dy: request.dy, bytes: raw.slice(start, end) };
  },
  (response) => {
    self.postMessage(response);
  },
);

self.addEventListener('message', handler);
