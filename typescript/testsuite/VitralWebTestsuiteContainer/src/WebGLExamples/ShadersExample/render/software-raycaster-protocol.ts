import type { WorkerTransferValue } from '@vitral/base';

/**
 * What a tile worker needs to rebuild the scene Java's
 * `SoftwareRaycaster.buildSceneSnapshot` builds.
 *
 * Java hands its threads the snapshot itself, because they share its heap. A
 * Web Worker shares nothing, so the scene travels as the structured-clone
 * values below and is rebuilt on the other side. Every field names a value the
 * Java snapshot is built from, and nothing else: the model's camera, sphere,
 * light and quality flags, the active material — the Cook-Torrance name when
 * that shading model is selected and the Phong material otherwise — the two
 * resources, and the tile bounds.
 *
 * `generation` counts resource loads. It has no Java counterpart: it is what
 * lets a worker keep the texture and the normal map it built and rebuild them
 * only when the user points the dialog at other resources.
 */
export interface ShadersTileRequest {
  readonly generation: number;

  readonly width: number;
  readonly height: number;
  readonly x0: number;
  readonly y0: number;
  readonly dx: number;
  readonly dy: number;

  readonly cameraPosition: Float64Array;
  readonly cameraRotation: Float64Array;
  readonly cameraFov: number;

  readonly sphereRadius: number;
  readonly modelRotation: Float64Array;

  readonly lightPosition: Float64Array;
  readonly lightColor: Float64Array;

  readonly qualityTexture: boolean;
  readonly qualityBumpMap: boolean;
  readonly qualityShadingType: number;

  readonly microFacetCsvText: string;
  readonly microFacetCsvName: string;
  /** `null` when the Phong material is the active one. */
  readonly microFacetMaterialName: string | null;

  readonly textureWidth: number;
  readonly textureHeight: number;
  readonly textureBytes: Uint8Array;
  readonly bumpWidth: number;
  readonly bumpHeight: number;
  readonly bumpBytes: Uint8Array;

  readonly [key: string]: WorkerTransferValue;
}

export interface ShadersTileResult {
  readonly y0: number;
  readonly dy: number;
  readonly bytes: Uint8Array;
  readonly [key: string]: WorkerTransferValue;
}
