#ifndef __ICON_GENERATOR__
#define __ICON_GENERATOR__

#include <java/lang/String.h>
class Calligraphic2DBuffer;
class Camera;
class SimpleScene;

class IconGenerator {
  public:
    IconGenerator();
    ~IconGenerator();

    Calligraphic2DBuffer* generate(const java::String& title) const;

  private:
    Calligraphic2DBuffer* buildVisibleIcon(SimpleScene& scene, const Camera& camera) const;
    Calligraphic2DBuffer* generateRayIcon() const;
    Calligraphic2DBuffer* generateObjectManipulatorIcon() const;
    Calligraphic2DBuffer* generateOmniLightIcon() const;
    Calligraphic2DBuffer* generateCubeFallbackIcon() const;
};

#endif
