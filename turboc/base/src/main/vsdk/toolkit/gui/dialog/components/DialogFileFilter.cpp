#include <stddef.h>
#include <ctype.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/dialog/components/DialogFileFilter.h"

static String lowerCase(const char* text)
{
    String result(text != NULL ? text : "");
    for ( int i = 0; i < result.size(); i++ ) {
        result[i] = ((char)(
            tolower(((unsigned char)(result[i])))));
    }
    return result;
}

DialogFileFilter::DialogFileFilter(const String& suffix,
                                   const String& description)
    : suffix(lowerCase(suffix.c_str())), text(description)
{
}

BOOLEAN DialogFileFilter::accepts(const String& path, BOOLEAN directory) const
{
    if ( directory ) {
        return TRUE;
    }
    int dot = path.rfind('.');
    return dot > 0 && dot < path.size() - 1 &&
        lowerCase(path.substring(dot + 1).c_str()) == suffix;
}

const String& DialogFileFilter::getSuffix() const
{
    return suffix;
}

String DialogFileFilter::description() const
{
    return text + " (*." + suffix + ")";
}
