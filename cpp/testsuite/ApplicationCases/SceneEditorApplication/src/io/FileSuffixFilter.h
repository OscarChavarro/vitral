#ifndef __FILE_SUFFIX_FILTER__
#define __FILE_SUFFIX_FILTER__

#include "java/io/File.h"
#include "java/lang/String.h"

/**
Accepts the files with a given suffix (extension), and the folders so the
user can browse them, as file choosers do. It does not depend on any GUI
technology: each one adapts it to its file chooser.
*/
class FileSuffixFilter {
private:
    java::String suffix;
    java::String description;

public:
    /**
    @param suffix accepted suffix, without the dot and in lower case
    @param description text describing the kind of files
    */
    FileSuffixFilter(const java::String& suffix,
                     const java::String& description);

    /**
    @param path path of a file
    @return the suffix of the file name (after the last dot) in lower case,
    or an empty string if it has none
    */
    static java::String getSuffix(const java::String& path);

    /**
    @param file file or folder
    @return true for folders and for files with the suffix of this filter
    */
    bool accept(const java::File& file) const;

    /**
    @return the description of the kind of files, with the suffix
    */
    java::String getDescription() const;
};

#endif
