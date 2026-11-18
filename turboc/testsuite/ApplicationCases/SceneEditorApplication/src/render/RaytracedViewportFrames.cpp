#include <string.h>
#include "java/lang/System.h"
#include "vsdk/toolkit/environment/background/FixedBackground.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/processing/ImageProcessing.h"
#include "model/DrawingArea.h"
#include "model/Scene.h"
#include "model/history/CameraState.h"
#include "render/RaytracedViewportFrames.h"

/// Resolution divisors (in each axis) usable during a gesture
static const int DIVISORS[] = {1, 2, 3, 4, 6, 8, 12, 16};
static const int NUMBER_OF_DIVISORS = sizeof(DIVISORS) / sizeof(DIVISORS[0]);
/// Part of `INTERACTIVE_FRAME_NANOSECONDS` the pixels of the image get at
/// least: when the fixed costs of an image take most of the frame, a smaller
/// image would barely make it faster
static const double MINIMUM_PIXEL_BUDGET = 0.25;

const double RaytracedViewportFrames::INTERACTIVE_FRAME_NANOSECONDS = 66.0e6;

RaytracedViewportFrames::RaytracedViewportFrames()
    : heldImageValid(FALSE), heldCamera(NULL), heldCameraState(NULL),
      heldWidth(0), heldHeight(0), heldDivisor(1), heldGeneration(0),
      nanosecondsPerPixel(0.0), fixedNanoseconds(0.0),
      nanosecondsPerEnlargedPixel(0.0),
      reducedImage(NULL), reducedDepth(NULL)
{
}

RaytracedViewportFrames::~RaytracedViewportFrames()
{
    delete heldCameraState;
    delete reducedImage;
    delete reducedDepth;
}

void RaytracedViewportFrames::invalidate()
{
    heldImageValid = FALSE;
    heldCamera = NULL;
    delete heldCameraState;
    heldCameraState = NULL;
}

void RaytracedViewportFrames::raytrace(Scene* scene, DrawingArea* drawingArea,
                                       RGBImageUncompressed* image,
                                       ZBuffer* depth)
{
    Camera* camera = scene->activeCamera;
    int width = image->getXSize();
    int height = image->getYSize();
    long generation = drawingArea->getContentGeneration();
    int divisor = 1;

    if ( drawingArea->isInteractiveGestureInCourse() ) {
        divisor = chooseDivisor(width, height);
    }

    CameraState* cameraState = CameraState::capture(camera);
    if ( heldImageValid && heldCamera == camera &&
         heldCameraState->isSameState(cameraState) &&
         heldWidth == width && heldHeight == height &&
         heldDivisor <= divisor && heldGeneration == generation ) {
        // The held image is as detailed as the one asked, or more
        delete cameraState;
        return;
    }

    long long start = System::nanoTime();
    if ( divisor == 1 ) {
        prefillBackground(scene, image);
        scene->raytraceViewport(image, depth);
        double elapsed = (double)(System::nanoTime() - start);
        // The fixed cost of the image is not a cost of its pixels
        double variable = elapsed - fixedNanoseconds;
        if ( variable < 0.5 * elapsed ) {
            variable = 0.5 * elapsed;
        }
        nanosecondsPerPixel = smoothedCost(nanosecondsPerPixel, variable,
            (double)width * (double)height);
    }
    else {
        int reducedWidth = width / divisor;
        int reducedHeight = height / divisor;
        if ( reducedWidth < 1 ) reducedWidth = 1;
        if ( reducedHeight < 1 ) reducedHeight = 1;
        ensureReducedBuffers(reducedWidth, reducedHeight);
        prefillBackground(scene, reducedImage);
        scene->raytraceViewport(reducedImage, reducedDepth);
        long long raytraced = System::nanoTime();
        enlarge(reducedImage, reducedDepth, image, depth);
        // What the pixels do not explain is the fixed cost of the image
        double fixed = (double)(raytraced - start) - nanosecondsPerPixel *
            (double)reducedWidth * (double)reducedHeight;
        if ( fixed < 0.0 ) {
            fixed = 0.0;
        }
        fixedNanoseconds = smoothedCost(fixedNanoseconds, fixed, 1.0);
        nanosecondsPerEnlargedPixel = smoothedCost(nanosecondsPerEnlargedPixel,
            (double)(System::nanoTime() - raytraced),
            (double)width * (double)height);
    }

    heldImageValid = TRUE;
    heldCamera = camera;
    delete heldCameraState;
    heldCameraState = cameraState;
    heldWidth = width;
    heldHeight = height;
    heldDivisor = divisor;
    heldGeneration = generation;
}

/**
@return the smallest divisor of the resolution whose image is expected to
be raytraced and enlarged in `INTERACTIVE_FRAME_NANOSECONDS`, or whose
pixels are expected to take `MINIMUM_PIXEL_BUDGET` of that time if the fixed
costs leave less than that to them
*/
int RaytracedViewportFrames::chooseDivisor(int width, int height) const
{
    if ( nanosecondsPerPixel <= 0.0 ) {
        return 1;
    }
    double pixels = (double)width * (double)height;
    int i;

    for ( i = 0; i < NUMBER_OF_DIVISORS; i++ ) {
        int divisor = DIVISORS[i];
        double fixed = fixedNanoseconds;
        if ( divisor > 1 ) {
            fixed += nanosecondsPerEnlargedPixel * pixels;
        }
        double budget = INTERACTIVE_FRAME_NANOSECONDS - fixed;
        if ( budget < MINIMUM_PIXEL_BUDGET * INTERACTIVE_FRAME_NANOSECONDS ) {
            budget = MINIMUM_PIXEL_BUDGET * INTERACTIVE_FRAME_NANOSECONDS;
        }
        if ( nanosecondsPerPixel * pixels / (divisor * (double)divisor) <=
             budget ) {
            return divisor;
        }
    }
    return DIVISORS[NUMBER_OF_DIVISORS - 1];
}

/**
@param current cost known before, or 0 if unknown
@param elapsedNanoseconds time measured for `pixels` pixels
@param pixels number of pixels measured (1 for a cost by image)
@return the cost of a pixel, smoothed: it depends on what the pixels show
*/
double RaytracedViewportFrames::smoothedCost(double current,
                                             double elapsedNanoseconds,
                                             double pixels)
{
    // Turbo C++ keeps `long long` in 32 bits: the clock wraps around
    if ( pixels <= 0.0 || elapsedNanoseconds < 0 ) {
        return current;
    }
    double sample = (double)elapsedNanoseconds / pixels;
    if ( current <= 0.0 ) {
        return sample;
    }
    return 0.5 * current + 0.5 * sample;
}

void RaytracedViewportFrames::ensureReducedBuffers(int width, int height)
{
    if ( reducedImage == NULL || reducedImage->getXSize() != width ||
         reducedImage->getYSize() != height ) {
        delete reducedImage;
        delete reducedDepth;
        reducedImage = new RGBImageUncompressed();
        reducedImage->init(width, height);
        reducedDepth = new ZBuffer(width, height);
    }
}

/**
The fixed background is copied to the image before raytracing it, as the
application has always done.
*/
void RaytracedViewportFrames::prefillBackground(Scene* scene,
                                                RGBImageUncompressed* image)
{
    if ( scene->selectedBackground == 1 && scene->fixedBackground != NULL ) {
        ImageProcessing::resize(scene->fixedBackground->getImage(), image);
    }
}

/**
Copies a small image and its depth over larger ones, repeating each pixel
(nearest pixel), so overlays are still depth tested against it.
*/
void RaytracedViewportFrames::enlarge(RGBImageUncompressed* source,
                                      ZBuffer* sourceDepth,
                                      RGBImageUncompressed* target,
                                      ZBuffer* targetDepth)
{
    int sw = source->getXSize();
    int sh = source->getYSize();
    int tw = target->getXSize();
    int th = target->getYSize();
    // Rows are stored in the order of the image: the last one first
    const char* sourcePixels = source->getRawImageDirectBuffer();
    char* targetPixels = target->getRawImageDirectBuffer();
    const float* sourceZ = sourceDepth->getZBufferDirectBuffer();
    float* targetZ = targetDepth->getZBufferDirectBuffer();
    int* columns = new int[tw];
    int x;
    int y;

    for ( x = 0; x < tw; x++ ) {
        int sx = (int)((long)x * sw / tw);
        columns[x] = sx < sw - 1 ? sx : sw - 1;
    }
    if ( sourcePixels == NULL || targetPixels == NULL ) {
        // The pixels are kept out of the program memory (DOS images)
        RGBPixel pixel;
        for ( y = 0; y < th; y++ ) {
            int sy = (int)((long)y * sh / th);
            if ( sy > sh - 1 ) sy = sh - 1;
            for ( x = 0; x < tw; x++ ) {
                source->getPixelRgb(columns[x], sy, &pixel);
                target->putPixel(x, y, &pixel);
                targetDepth->setDepth(x, y,
                    sourceDepth->getDepth(columns[x], sy));
            }
        }
        delete[] columns;
        return;
    }
    int lastSourceY = -1;
    char* lastRow = NULL;
    for ( y = 0; y < th; y++ ) {
        int sy = (int)((long)y * sh / th);
        if ( sy > sh - 1 ) sy = sh - 1;
        char* row = targetPixels + (long)(th - 1 - y) * tw * 3;
        if ( sy == lastSourceY ) {
            // Same source row: the enlarged one is repeated
            memcpy(row, lastRow, (size_t)tw * 3);
        }
        else {
            const char* base = sourcePixels + (long)(sh - 1 - sy) * sw * 3;
            for ( x = 0; x < tw; x++ ) {
                const char* s = base + columns[x] * 3;
                row[3 * x] = s[0];
                row[3 * x + 1] = s[1];
                row[3 * x + 2] = s[2];
            }
            lastSourceY = sy;
            lastRow = row;
        }
        long zBase = (long)sy * sw;
        long zTarget = (long)y * tw;
        for ( x = 0; x < tw; x++ ) {
            targetZ[zTarget + x] = sourceZ[zBase + columns[x]];
        }
    }
    delete[] columns;
}
