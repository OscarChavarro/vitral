package render;

// VSDK classes
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.processing.ImageProcessing;

// Application classes
import model.DrawingArea;
import model.Scene;
import model.history.CameraState;

/**
Computes the raytraced images of the viewports in CPU render mode, which are
drawn again in each frame, avoiding work that does not change the result:

  - The image held in the output buffers is presented again, without
    raytracing, while the camera, the size and the content of the scene (see
    `DrawingArea.getContentGeneration`) do not change: i.e. when the drawing
    area is exposed after closing a menu, or when only overlays (gizmos,
    selection) change. As the buffers are shared by all the viewports, only
    the last one raytraced benefits from it.
  - While a gesture is in course (see
    `DrawingArea.isInteractiveGestureInCourse`) the image is raytraced at a
    lower resolution, chosen from the time the last images took, so each frame
    takes about `INTERACTIVE_FRAME_NANOSECONDS`, and it is enlarged (nearest
    pixel) to the size of the viewport. The exact image is raytraced in the
    first frame after the gesture.
*/
public class RaytracedViewportFrames
{
    /// Time wanted for the raytracing of a frame during a gesture
    public static final double INTERACTIVE_FRAME_NANOSECONDS = 66.0e6;

    /// Resolution divisors (in each axis) usable during a gesture
    private static final int[] DIVISORS = {1, 2, 3, 4, 6, 8, 12, 16};

    /// Part of `INTERACTIVE_FRAME_NANOSECONDS` the pixels of the image get at
    /// least: when the fixed costs of an image take most of the frame, a
    /// smaller image would barely make it faster
    private static final double MINIMUM_PIXEL_BUDGET = 0.25;

    //- Description of the image held in the output buffers ---------------
    private boolean heldImageValid;
    private Camera heldCamera;
    private CameraState heldCameraState;
    private int heldWidth;
    private int heldHeight;
    private int heldDivisor;
    private long heldGeneration;

    //- Cost estimation ---------------------------------------------------
    /// Time to raytrace a pixel in the last exact images, without the fixed
    /// cost of an image, or 0 if unknown
    private double nanosecondsPerPixel;
    /// Time to raytrace an image besides its pixels (preparing the scene,
    /// dispatching the work...), estimated from the reduced images
    private double fixedNanoseconds;
    /// Time to enlarge the last reduced images, by pixel of the viewport
    private double nanosecondsPerEnlargedPixel;

    //- Buffers for the images of reduced resolution ----------------------
    private RGBImageUncompressed reducedImage;
    private ZBuffer reducedDepth;

    public RaytracedViewportFrames()
    {
        heldImageValid = false;
        nanosecondsPerPixel = 0.0;
        fixedNanoseconds = 0.0;
        nanosecondsPerEnlargedPixel = 0.0;
        reducedImage = null;
        reducedDepth = null;
    }

    /**
    The output buffers were changed by other means (i.e. resized, or used to
    raytrace a full image): their content can not be presented again.
    */
    public void invalidate()
    {
        heldImageValid = false;
        heldCamera = null;
        heldCameraState = null;
    }

    /**
    Leaves in the output buffers the raytraced image of the scene seen from
    its active camera, and its depth.
    @param scene scene to raytrace, with the camera of the viewport active
    @param drawingArea gives the content generation and the gesture state
    @param image output image, already of the size of the viewport
    @param depth output depth, of the same size of the image
    */
    public void raytrace(Scene scene, DrawingArea drawingArea,
                         RGBImageUncompressed image, ZBuffer depth)
    {
        Camera camera = scene.activeCamera;
        int width = image.getXSize();
        int height = image.getYSize();
        long generation = drawingArea.getContentGeneration();
        int divisor = 1;

        if ( drawingArea.isInteractiveGestureInCourse() ) {
            divisor = chooseDivisor(width, height);
        }

        CameraState cameraState = CameraState.capture(camera);
        if ( heldImageValid && heldCamera == camera &&
             heldCameraState.isSameState(cameraState) &&
             heldWidth == width && heldHeight == height &&
             heldDivisor <= divisor && heldGeneration == generation ) {
            // The held image is as detailed as the one asked, or more
            return;
        }

        long start = System.nanoTime();
        if ( divisor == 1 ) {
            prefillBackground(scene, image);
            scene.raytraceViewport(image, depth);
            double elapsed = System.nanoTime() - start;
            // The fixed cost of the image is not a cost of its pixels
            nanosecondsPerPixel = smoothedCost(nanosecondsPerPixel,
                Math.max(elapsed - fixedNanoseconds, 0.5 * elapsed),
                width * (double)height);
        }
        else {
            int reducedWidth = Math.max(1, width / divisor);
            int reducedHeight = Math.max(1, height / divisor);
            ensureReducedBuffers(reducedWidth, reducedHeight);
            prefillBackground(scene, reducedImage);
            scene.raytraceViewport(reducedImage, reducedDepth);
            long raytraced = System.nanoTime();
            enlarge(reducedImage, reducedDepth, image, depth);
            // What the pixels do not explain is the fixed cost of the image
            fixedNanoseconds = smoothedCost(fixedNanoseconds,
                Math.max(0.0, (raytraced - start) - nanosecondsPerPixel *
                    reducedWidth * (double)reducedHeight), 1.0);
            nanosecondsPerEnlargedPixel = smoothedCost(
                nanosecondsPerEnlargedPixel, System.nanoTime() - raytraced,
                width * (double)height);
        }

        heldImageValid = true;
        heldCamera = camera;
        heldCameraState = cameraState;
        heldWidth = width;
        heldHeight = height;
        heldDivisor = divisor;
        heldGeneration = generation;
    }

    /**
    @return the smallest divisor of the resolution whose image is expected
    to be raytraced and enlarged in `INTERACTIVE_FRAME_NANOSECONDS`, or
    whose pixels are expected to take `MINIMUM_PIXEL_BUDGET` of that time if
    the fixed costs leave less than that to them
    */
    private int chooseDivisor(int width, int height)
    {
        if ( nanosecondsPerPixel <= 0.0 ) {
            return 1;
        }
        double pixels = (double)width * (double)height;
        for ( int divisor : DIVISORS ) {
            double fixed = fixedNanoseconds;
            if ( divisor > 1 ) {
                fixed += nanosecondsPerEnlargedPixel * pixels;
            }
            double budget = Math.max(INTERACTIVE_FRAME_NANOSECONDS - fixed,
                MINIMUM_PIXEL_BUDGET * INTERACTIVE_FRAME_NANOSECONDS);
            if ( nanosecondsPerPixel * pixels / (divisor * (double)divisor) <=
                 budget ) {
                return divisor;
            }
        }
        return DIVISORS[DIVISORS.length - 1];
    }

    /**
    @param current cost known before, or 0 if unknown
    @param elapsedNanoseconds time measured for `pixels` pixels
    @param pixels number of pixels measured (1 for a cost by image)
    @return the cost of a pixel, smoothed: it depends on what the pixels show
    */
    private static double smoothedCost(double current, double elapsedNanoseconds,
                                       double pixels)
    {
        if ( pixels <= 0.0 ) {
            return current;
        }
        double sample = elapsedNanoseconds / pixels;
        if ( current <= 0.0 ) {
            return sample;
        }
        return 0.5 * current + 0.5 * sample;
    }

    private void ensureReducedBuffers(int width, int height)
    {
        if ( reducedImage == null || reducedImage.getXSize() != width ||
             reducedImage.getYSize() != height ) {
            reducedImage = new RGBImageUncompressed();
            reducedImage.init(width, height);
            reducedDepth = new ZBuffer(width, height);
        }
    }

    /**
    The fixed background is copied to the image before raytracing it, as the
    application has always done.
    */
    private static void prefillBackground(Scene scene, RGBImageUncompressed image)
    {
        if ( scene.selectedBackground == 1 && scene.fixedBackground != null ) {
            ImageProcessing.resize(scene.fixedBackground.getImage(), image);
        }
    }

    /**
    Copies a small image and its depth over larger ones, repeating each pixel
    (nearest pixel), so overlays are still depth tested against it.
    */
    private static void enlarge(RGBImageUncompressed source, ZBuffer sourceDepth,
                                RGBImageUncompressed target, ZBuffer targetDepth)
    {
        int sw = source.getXSize();
        int sh = source.getYSize();
        int tw = target.getXSize();
        int th = target.getYSize();
        byte[] sourcePixels = source.getRawImage();
        byte[] row = new byte[tw * 3];
        int[] columns = new int[tw];
        float[] sourceZ = sourceDepth.getZBuffer();
        float[] targetZ = targetDepth.getZBuffer();
        java.nio.ByteBuffer targetPixels = target.getRawImageDirectBuffer();

        for ( int x = 0; x < tw; x++ ) {
            columns[x] = Math.min(sw - 1, x * sw / tw);
        }
        int lastSourceY = -1;
        for ( int y = 0; y < th; y++ ) {
            int sy = Math.min(sh - 1, y * sh / th);
            if ( sy != lastSourceY ) {
                int base = sy * sw * 3;
                for ( int x = 0; x < tw; x++ ) {
                    int s = base + columns[x] * 3;
                    row[3*x] = sourcePixels[s];
                    row[3*x+1] = sourcePixels[s+1];
                    row[3*x+2] = sourcePixels[s+2];
                }
                lastSourceY = sy;
            }
            targetPixels.position(y * tw * 3);
            targetPixels.put(row);
            int zBase = sy * sw;
            int zTarget = y * tw;
            for ( int x = 0; x < tw; x++ ) {
                targetZ[zTarget + x] = sourceZ[zBase + columns[x]];
            }
        }
        targetPixels.rewind();
    }
}
