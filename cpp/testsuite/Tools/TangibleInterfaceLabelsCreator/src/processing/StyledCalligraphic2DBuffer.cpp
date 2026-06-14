#include "processing/StyledCalligraphic2DBuffer.h"

Calligraphic2DBuffer& StyledCalligraphic2DBuffer::visibleContourLines() {
    return contourLines;
}

const Calligraphic2DBuffer& StyledCalligraphic2DBuffer::visibleContourLines() const {
    return contourLines;
}

Calligraphic2DBuffer& StyledCalligraphic2DBuffer::visibleInternalLines() {
    return internalLines;
}

const Calligraphic2DBuffer& StyledCalligraphic2DBuffer::visibleInternalLines() const {
    return internalLines;
}
