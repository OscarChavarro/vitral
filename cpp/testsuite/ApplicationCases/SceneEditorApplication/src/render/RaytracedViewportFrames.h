#ifndef __RAYTRACED_VIEWPORT_FRAMES__
#define __RAYTRACED_VIEWPORT_FRAMES__

class Camera;
class CameraState;
class DrawingArea;
class RGBImageUncompressed;
class Scene;
class ZBuffer;

/**
Computes the raytraced images of the viewports in CPU render mode, which are
drawn again in each frame, avoiding work that does not change the result
(port of the Java `render.RaytracedViewportFrames`):

  - The image held in the output buffers is presented again, without
    raytracing, while the camera, the size and the content of the scene (see
    `DrawingArea::getContentGeneration`) do not change: i.e. when the drawing
    area is exposed after closing a menu, or when only overlays (gizmos,
    selection) change. As the buffers are shared by all the viewports, only
    the last one raytraced benefits from it.
  - While a gesture is in course (see
    `DrawingArea::isInteractiveGestureInCourse`) the image is raytraced at a
    lower resolution, chosen from the time the last images took, so each frame
    takes about `INTERACTIVE_FRAME_NANOSECONDS`, and it is enlarged (nearest
    pixel) to the size of the viewport. The exact image is raytraced in the
    first frame after the gesture.
*/
class RaytracedViewportFrames {
public:
    /// Time wanted for the raytracing of a frame during a gesture
    static const double INTERACTIVE_FRAME_NANOSECONDS;

    RaytracedViewportFrames();
    ~RaytracedViewportFrames();

    /**
    The output buffers were changed by other means (i.e. resized, or used to
    raytrace a full image): their content can not be presented again.
    */
    void invalidate();

    /**
    Leaves in the output buffers the raytraced image of the scene seen from
    its active camera, and its depth.
    @param scene scene to raytrace, with the camera of the viewport active
    @param drawingArea gives the content generation and the gesture state
    @param image output image, already of the size of the viewport
    @param depth output depth, of the same size of the image
    */
    void raytrace(Scene* scene, DrawingArea* drawingArea,
                  RGBImageUncompressed* image, ZBuffer* depth);

private:
    //- Description of the image held in the output buffers ---------------
    bool heldImageValid;
    Camera* heldCamera;
    CameraState* heldCameraState;
    int heldWidth;
    int heldHeight;
    int heldDivisor;
    long long heldGeneration;

    //- Cost estimation ---------------------------------------------------
    /// Time to raytrace a pixel in the last exact images, without the fixed
    /// cost of an image, or 0 if unknown
    double nanosecondsPerPixel;
    /// Time to raytrace an image besides its pixels (preparing the scene,
    /// dispatching the work...), estimated from the reduced images
    double fixedNanoseconds;
    /// Time to enlarge the last reduced images, by pixel of the viewport
    double nanosecondsPerEnlargedPixel;

    //- Buffers for the images of reduced resolution ----------------------
    RGBImageUncompressed* reducedImage;
    ZBuffer* reducedDepth;

    int chooseDivisor(int width, int height) const;
    static double smoothedCost(double current, double elapsedNanoseconds,
                               double pixels);
    void ensureReducedBuffers(int width, int height);
    static void prefillBackground(Scene* scene, RGBImageUncompressed* image);
    static void enlarge(RGBImageUncompressed* source, ZBuffer* sourceDepth,
                        RGBImageUncompressed* target, ZBuffer* targetDepth);

    RaytracedViewportFrames(const RaytracedViewportFrames&);
    RaytracedViewportFrames& operator=(const RaytracedViewportFrames&);
};

#endif
