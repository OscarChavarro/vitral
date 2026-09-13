import {
  IndexedColorImageUncompressed,
  NormalMap,
  RGBImageUncompressed,
  Vector3Dd,
} from '@vitral/base';
import { WebImagePersistence } from '@vitral/webgl';

/**
 * What `model.ShadersModel.initializeDefaults` and
 * `render.SoftwareRaycaster.loadBumpNormalMap` read from disk, read from URLs
 * instead.
 *
 * Java opens three `java.io.File`s — a PNG texture, a `.bw` SGI bump map and
 * the microfacet CSV — inside a constructor, because `ImagePersistence` and
 * `new FileInputStream` are synchronous. A browser fetches, so the reads move
 * out of the model's construction and into this class, and the model is handed
 * what was read. That is the only change: the three resources, the
 * `NormalMap.importBumpMap` with a unit bump scale, and the
 * `exportToRgbImage()` that keeps the GLSL and CPU paths on one precomputed
 * normal field are Java's.
 *
 * Java also builds the normal map twice, once in `ShadersModel` and once in
 * `SoftwareRaycaster`, from the same file with the same scale. One read here
 * serves both, since the two results are equal by construction.
 */
export interface ShadersResources {
  readonly textureMap: RGBImageUncompressed;
  readonly bumpNormalMap: NormalMap;
  readonly bumpMapHeightRgb: RGBImageUncompressed;
  readonly bumpMapFile: IndexedColorImageUncompressed;
  readonly microFacetCsvText: string;
  readonly microFacetCsvName: string;
}

export class ShadersReader {
  private static readonly DEFAULT_BUMP_SCALE = new Vector3Dd(1.0, 1.0, 1.0);

  /**
   * Java's `catch (Exception e) { throw new IllegalStateException("Failed
   * loading textures for ShadersExample", e); }`, which ends the program. A
   * module inside the container cannot end the process, so the rejection
   * reaches the component and is shown in its status overlay.
   */
  async read(
    textureUrl: string,
    bumpMapUrl: string,
    microFacetCsvUrl: string,
  ): Promise<ShadersResources> {
    const textureMap = (await WebImagePersistence.importRGB(textureUrl)) as RGBImageUncompressed;

    const bumpMapFile: IndexedColorImageUncompressed =
      await WebImagePersistence.importIndexedColor(bumpMapUrl);
    const bumpNormalMap = new NormalMap();
    bumpNormalMap.importBumpMap(bumpMapFile, ShadersReader.DEFAULT_BUMP_SCALE);
    // Keep GLSL and CPU raytracer aligned: both consume the same
    // precomputed normal field extracted from the bump map.
    const bumpMapHeightRgb: RGBImageUncompressed = bumpNormalMap.exportToRgbImage();

    const microFacetCsvText: string = await ShadersReader.readText(microFacetCsvUrl);

    return {
      textureMap,
      bumpNormalMap,
      bumpMapHeightRgb,
      bumpMapFile,
      microFacetCsvText,
      microFacetCsvName: microFacetCsvUrl,
    };
  }

  /**
   * Java's `Files.readAllLines` and its `new FileInputStream(csvFile)`, which
   * both begin with `resolveCsvFile`: the name as given, then the same name
   * under `etc/materials`. A URL is already resolved, so there is nothing to
   * search, and a failed fetch is the `!csvFile.exists()` branch.
   */
  private static async readText(url: string): Promise<string> {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error('Microfacet CSV file not found: ' + url);
    }
    return response.text();
  }
}
