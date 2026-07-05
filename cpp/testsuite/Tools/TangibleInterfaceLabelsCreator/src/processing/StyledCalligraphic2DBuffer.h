#ifndef __STYLEDCALLIGRAPHIC2DBUFFER__
#define __STYLEDCALLIGRAPHIC2DBUFFER__

#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
class StyledCalligraphic2DBuffer : public Calligraphic2DBuffer {
  public:
    StyledCalligraphic2DBuffer() = default;
    virtual ~StyledCalligraphic2DBuffer() = default;

    Calligraphic2DBuffer& visibleContourLines();
    const Calligraphic2DBuffer& visibleContourLines() const;

    Calligraphic2DBuffer& visibleInternalLines();
    const Calligraphic2DBuffer& visibleInternalLines() const;

  private:
    Calligraphic2DBuffer contourLines;
    Calligraphic2DBuffer internalLines;
};

#endif
