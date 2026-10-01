import {
  NormalMap,
  RGBImageUncompressed,
  Vector3Dd,
  type IndexedColorImageUncompressed,
  type RGBPixel,
  type Viewport,
  type ViewportSet,
} from '@vitral/base';
import { ImageFiles } from '../io/image-files';
import type { ApplicationModel } from '../model/application-model';
import type { DrawingArea } from '../model/drawing-area';
import type { DrawingAreaHost } from './drawing-area-host';
import type { FrameBufferSource } from './frame-buffer-source';

/**
 * Port of `render.FrameCaptureService`.
 *
 * Captures the content of the frame buffers when the user requested it through
 * the `DrawingArea`: the color buffer, the depth buffer, and the export of the
 * frame (or of the selected viewport) to image files. Captured images are left
 * in the application model and presented through the `DrawingAreaHost`. The
 * buffers are read through a `FrameBufferSource`, so this class does not depend
 * on the rendering technology. Image files are written through the
 * `ImageFiles` the application installs (downloads, in a page).
 */
export class FrameCaptureService {
  private readonly model: ApplicationModel;
  private readonly drawingArea: DrawingArea;
  private readonly viewportSet: ViewportSet;
  private readonly host: DrawingAreaHost;

  constructor(model: ApplicationModel, host: DrawingAreaHost) {
    this.model = model;
    this.drawingArea = model.getDrawingArea();
    this.viewportSet = this.drawingArea.getViewportSet();
    this.host = host;
  }

  /**
   * Captures the color buffer, if it was requested and the view being drawn is
   * the selected one.
   * @param source frame buffer of the view being drawn
   * @param selectedView true if the view being drawn is the selected one
   */
  copyColorBufferIfNeeded(source: FrameBufferSource, selectedView: boolean): void {
    if (this.drawingArea.isColorCaptureRequested() && selectedView) {
      this.model.setZbufferImage(source.readColor());
      this.host.showImage(this.model.getZbufferImage());
      this.host.showStatusMessage('ZBuffer Color Image obtained!');
      this.drawingArea.setColorCaptureRequested(false);
    }
  }

  /**
   * Captures the depth buffer, if it was requested.
   * @param source frame buffer of the view being drawn
   */
  async copyZBufferIfNeeded(source: FrameBufferSource): Promise<void> {
    if (!this.drawingArea.isDepthCaptureRequested()) {
      return;
    }

    if (this.drawingArea.isContoursRequested()) {
      const zbuffer: IndexedColorImageUncompressed = (await source.readDepth()).exportIndexedColorImage();
      const nm: NormalMap = new NormalMap();
      nm.importBumpMap(zbuffer, new Vector3Dd(1, 1, 0.1));
      this.model.setZbufferImage(nm.exportToRgbImageGradient());
    } else {
      this.model.setZbufferImage((await source.readDepth()).exportRGBImage(this.model.getPalette()!));
    }

    this.host.showImage(this.model.getZbufferImage());
    this.host.showStatusMessage('ZBuffer depth map obtained!');
    this.drawingArea.setDepthCaptureRequested(false);
    this.drawingArea.setContoursRequested(false);
  }

  /**
   * @return true if the user requested to export the next frame to files
   */
  isFrameExportPending(): boolean {
    return (
      this.drawingArea.getPendingViewportExportFile() !== null ||
      this.drawingArea.getPendingWorkspaceExportFile() !== null
    );
  }

  /**
   * Exports to files the frame just drawn, if it was requested.
   * @param source frame buffer, set to the whole viewport set area
   */
  async exportPendingFrame(source: FrameBufferSource): Promise<void> {
    if (!this.isFrameExportPending()) {
      return;
    }

    const workspace: RGBImageUncompressed = source.readColor();
    const viewportFile: string | null = this.drawingArea.getPendingViewportExportFile();

    if (viewportFile !== null) {
      const selected: Viewport | null = this.viewportSet.getSelectedViewport();
      if (selected !== null) {
        const viewport: RGBImageUncompressed = this.cropImage(
          workspace,
          selected.getPixelStartX(),
          selected.getPixelStartY(),
          selected.getPixelSizeX(),
          selected.getPixelSizeY(),
        );
        if (this.drawingArea.isPendingViewportExportJpg()) {
          await ImageFiles.get().exportJPG(viewportFile, viewport);
        } else {
          await ImageFiles.get().exportPNG(viewportFile, viewport);
        }
      } else {
        this.host.showStatusMessage('ERROR: there is no selected viewport to export');
      }
      this.drawingArea.clearPendingViewportExport();
    }

    const workspaceFile: string | null = this.drawingArea.getPendingWorkspaceExportFile();
    if (workspaceFile !== null) {
      await ImageFiles.get().exportJPG(workspaceFile, workspace);
      this.drawingArea.clearPendingWorkspaceExport();
    }
  }

  private cropImage(
    source: RGBImageUncompressed,
    startX: number,
    startY: number,
    width: number,
    height: number,
  ): RGBImageUncompressed {
    const sourceWidth: number = source.getXSize();
    const sourceHeight: number = source.getYSize();
    const x0: number = Math.max(0, startX);
    const y0: number = Math.max(0, startY);
    const x1: number = Math.min(sourceWidth, startX + width);
    const y1: number = Math.min(sourceHeight, startY + height);
    const result: RGBImageUncompressed = new RGBImageUncompressed();
    result.init(Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    for (let y = y0; y < y1; y++) {
      for (let x = x0; x < x1; x++) {
        const pixel: RGBPixel = source.getPixel(x, y);
        result.putPixel(x - x0, y - y0, pixel);
      }
    }
    return result;
  }
}
