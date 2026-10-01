/*
Deep module specifiers rather than the `@vitral/base` barrel: a worker that
pulls the whole barrel pays for compiling every module in it, once per worker
(see the tile worker of `ShadersExample`).
*/
import { createWorkerMessageHandler } from '@vitral/base/java/concurrent/WorkerProtocol';
import type { WorkerTransferValue } from '@vitral/base/java/concurrent/WorkerProtocol';
import type { SimpleSceneSnapshot } from '@vitral/base/vsdk/toolkit/environment/scene/SimpleSceneSnapshot';
import type {
  ParallelRaytracerTileRequest,
  ParallelRaytracerTileResult,
} from '@vitral/base/vsdk/toolkit/render/raytracing/ParallelRaytracer';
import { ParallelRaytracerTileRenderer } from '@vitral/base/vsdk/toolkit/render/raytracing/ParallelRaytracerTileRenderer';
import { decodeSimpleSceneSnapshot } from '@vitral/base/vsdk/toolkit/render/raytracing/SimpleSceneSnapshotTransfer';
// The barrel installs these `RendererConfiguration` methods as a side effect;
// a worker that does not load the barrel must ask for them explicitly.
import '@vitral/base/vsdk/toolkit/environment/material/RendererConfigurationBehavior';

/**
 * Worker entry module of the `ParallelRaytracer` of the scene editor (see
 * `WebSceneRaytracingBackend`).
 *
 * Java's threads share the scene snapshot of `Scene.raytrace`. A Web Worker
 * shares no heap, so the snapshot travels encoded (see
 * `SimpleSceneSnapshotTransfer`), and this module rebuilds it, once per frame,
 * for the `ParallelRaytracerTileRenderer` that renders its bands. The snapshot
 * already carries the camera exported for the size of the image.
 */
const renderer = new ParallelRaytracerTileRenderer(
  (sceneDescriptor: WorkerTransferValue, _width: number, _height: number): SimpleSceneSnapshot =>
    decodeSimpleSceneSnapshot(sceneDescriptor),
);

const handler = createWorkerMessageHandler<ParallelRaytracerTileRequest, ParallelRaytracerTileResult>(
  (request, _signal, notify) => renderer.renderTile(request, notify),
  (response) => {
    self.postMessage(response);
  },
);

self.addEventListener('message', handler);
