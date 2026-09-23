#include <cctype>

#include "io/FileSuffixFilter.h"

FileSuffixFilter::FileSuffixFilter(const java::String& suffix,
                                   const java::String& description)
    : suffix(suffix), description(description)
{
}

java::String FileSuffixFilter::getSuffix(const java::String& path)
{
    java::String suffix = "";
    int i = path.rfind('.');

    if ( i > 0 && i < path.length() - 1 ) {
        suffix = path.substring(i + 1);
        int j;
        for ( j = 0; j < suffix.length(); j++ ) {
            suffix[j] = (char)std::tolower((unsigned char)suffix[j]);
        }
    }
    return suffix;
}

bool FileSuffixFilter::accept(const java::File& file) const
{
    if ( file.isDirectory() ) {
        return true;
    }
    return !suffix.isEmpty() && suffix.equals(getSuffix(file.getPath()));
}

java::String FileSuffixFilter::getDescription() const
{
    return description + " (*." + suffix + ")";
}
