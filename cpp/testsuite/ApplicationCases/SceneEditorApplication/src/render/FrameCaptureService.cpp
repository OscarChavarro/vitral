#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "render/DrawingAreaHost.h"
#include "render/FrameBufferSource.h"
#include "render/FrameCaptureService.h"

FrameCaptureService::FrameCaptureService(ApplicationModel* model,
                                         DrawingAreaHost* host)
    : model(model), drawingArea(model->getDrawingArea()),
      viewportSet(drawingArea->getViewportSet()), host(host)
{
}

void FrameCaptureService::copyColorBufferIfNeeded(FrameBufferSource* source,
                                                  bool selectedView)
{
    if ( drawingArea->isColorCaptureRequested() && selectedView ) {
        model->setZbufferImage(source->readColor());
        host->showImage(model->getZbufferImage());
        host->showStatusMessage("ZBuffer Color Image obtained!");
        drawingArea->setColorCaptureRequested(false);
    }
}

void FrameCaptureService::copyZBufferIfNeeded(FrameBufferSource* source)
{
    if ( !drawingArea->isDepthCaptureRequested() ) {
        return;
    }

    ZBuffer* depth = source->readDepth();
    if ( drawingArea->isContoursRequested() ) {
        IndexedColorImageUncompressed* zbuffer;
        NormalMap nm;
        zbuffer = depth->exportIndexedColorImage();
        nm.importBumpMap(zbuffer, Vector3Dd(1, 1, 0.1));
        delete zbuffer;
        model->setZbufferImage(nm.exportToRgbImageGradient());
    }
    else {
        model->setZbufferImage(depth->exportRGBImage(model->getPalette()));
    }
    delete depth;

    host->showImage(model->getZbufferImage());
    host->showStatusMessage("ZBuffer depth map obtained!");
    drawingArea->setDepthCaptureRequested(false);
    drawingArea->setContoursRequested(false);
}

bool FrameCaptureService::isFrameExportPending() const
{
    return drawingArea->getPendingViewportExportFile() != nullptr ||
        drawingArea->getPendingWorkspaceExportFile() != nullptr;
}

void FrameCaptureService::exportPendingFrame(FrameBufferSource* source)
{
    if ( !isFrameExportPending() ) {
        return;
    }

    RGBImageUncompressed* workspace = source->readColor();

    if ( drawingArea->getPendingViewportExportFile() != nullptr ) {
        Viewport* selected = viewportSet->getSelectedViewport();
        if ( selected != nullptr ) {
            RGBImageUncompressed* viewport = cropImage(workspace,
                selected->getPixelStartX(), selected->getPixelStartY(),
                selected->getPixelSizeX(), selected->getPixelSizeY());
            if ( drawingArea->isPendingViewportExportJpg() ) {
                ImagePersistence::exportJPEG(
                    *drawingArea->getPendingViewportExportFile(), viewport);
            }
            else {
                ImagePersistence::exportPNG(
                    *drawingArea->getPendingViewportExportFile(), viewport);
            }
            delete viewport;
        }
        else {
            host->showStatusMessage(
                "ERROR: there is no selected viewport to export");
        }
        drawingArea->clearPendingViewportExport();
    }

    if ( drawingArea->getPendingWorkspaceExportFile() != nullptr ) {
        ImagePersistence::exportJPEG(
            *drawingArea->getPendingWorkspaceExportFile(), workspace);
        drawingArea->clearPendingWorkspaceExport();
    }
    delete workspace;
}

RGBImageUncompressed* FrameCaptureService::cropImage(
    const RGBImageUncompressed* source, int startX, int startY, int width,
    int height)
{
    int sourceWidth = source->getXSize();
    int sourceHeight = source->getYSize();
    int x0 = startX > 0 ? startX : 0;
    int y0 = startY > 0 ? startY : 0;
    int x1 = startX + width < sourceWidth ? startX + width : sourceWidth;
    int y1 = startY + height < sourceHeight ? startY + height : sourceHeight;
    RGBImageUncompressed* result = new RGBImageUncompressed();
    RGBPixel pixel;
    result->init(x1 - x0 > 0 ? x1 - x0 : 0, y1 - y0 > 0 ? y1 - y0 : 0);
    for ( int y = y0; y < y1; y++ ) {
        for ( int x = x0; x < x1; x++ ) {
            source->getPixelRgb(x, y, &pixel);
            result->putPixel(x - x0, y - y0, &pixel);
        }
    }
    return result;
}
