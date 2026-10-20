#include <cctype>
#include <string>

#include "vsdk/toolkit/gui/dialog/components/DialogFileFilter.h"

namespace {
std::string lowerCase(const char* text)
{
    std::string result(text != nullptr ? text : "");
    for ( size_t i = 0; i < result.size(); i++ ) {
        result[i] = static_cast<char>(
            std::tolower(static_cast<unsigned char>(result[i])));
    }
    return result;
}
}

DialogFileFilter::DialogFileFilter(const java::String& suffix,
                                   const java::String& description)
    : suffix(lowerCase(suffix.c_str()).c_str()), text(description)
{
}

bool DialogFileFilter::accepts(const java::String& path, bool directory) const
{
    if ( directory ) {
        return true;
    }
    std::string name(path.c_str());
    size_t dot = name.rfind('.');
    return dot != std::string::npos && dot > 0 && dot < name.size() - 1 &&
        lowerCase(name.substr(dot + 1).c_str()) == suffix.c_str();
}

const java::String& DialogFileFilter::getSuffix() const
{
    return suffix;
}

java::String DialogFileFilter::description() const
{
    return text + " (*." + suffix + ")";
}
