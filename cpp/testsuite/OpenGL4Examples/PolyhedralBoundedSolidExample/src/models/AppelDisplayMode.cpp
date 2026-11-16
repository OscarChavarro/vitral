#include <cstring>

#include "models/AppelDisplayMode.h"

static const char* const NAMES[] = {
    "OFF", "EDGES_VISIBLE_HIDDEN", "EDGES_VISIBLE", "EDGES_ONLY"
};

static const char* const LABELS[] = {
    "OFF", "edges + visible + hidden", "edges + visible", "edges only"
};

static const int VALUES_LENGTH = 4;

const char* AppelDisplayModeUtil::name(AppelDisplayMode value)
{
    return NAMES[(int)value];
}

const char* AppelDisplayModeUtil::getLabel(AppelDisplayMode value)
{
    return LABELS[(int)value];
}

AppelDisplayMode AppelDisplayModeUtil::nextCircular(AppelDisplayMode value)
{
    return (AppelDisplayMode)(((int)value + 1) % VALUES_LENGTH);
}

bool AppelDisplayModeUtil::valueOf(const char* name, AppelDisplayMode* out)
{
    int i;
    for ( i = 0; i < VALUES_LENGTH; i++ ) {
        if ( std::strcmp(NAMES[i], name) == 0 ) {
            *out = (AppelDisplayMode)i;
            return true;
        }
    }
    return false;
}
