#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "java/lang/Double.h"
#include "java/lang/Float.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

bool
Float::isFinite(float a) {
    return Cpp98Support::isFinite(a);
}

float
Float::parseFloat(const String& text) {
    return ((float)(Double::parseDouble(text)));
}

String
Float::toString(float value) {
    if ( !Cpp98Support::isFinite(value) || value == 0.0f ) {
        return Double::toString(((double)(value)));
    }
    // Shortest precision that reads back as the same float, then written
    // as the double with that decimal text (0.1f is "0.1", not
    // "0.10000000149011612")
    char buffer[64];
    for ( int precision = 1; precision <= 9; precision++ ) {
        snprintf(buffer, sizeof(buffer), "%.*g", precision,
                      ((double)(value)));
        if ( strtof(buffer, NULL) == value ) {
            break;
        }
    }
    return Double::toString(strtod(buffer, NULL));
}


const float Float::MIN_VALUE = 1.40129846e-45F;
const float Float::MAX_VALUE = 3.40282347e+38F;
