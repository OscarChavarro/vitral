#ifndef __FILE_SUFFIX_FILTER__
#define __FILE_SUFFIX_FILTER__

#include <vector>

#include "java/io/File.h"
#include "java/lang/String.h"

class WidgetDialog;

/**
Accepts the files with a given suffix (extension), and the folders so the
user can browse them, as file choosers do. It does not depend on any GUI
technology: each one adapts it to its file chooser.
*/
class FileSuffixFilter {
private:
    String suffix;
    String description;

public:
    /**
    @param suffix accepted suffix, without the dot and in lower case
    @param description text describing the kind of files
    */
    FileSuffixFilter(const String& suffix,
                     const String& description);

    /**
    @param path path of a file
    @return the suffix of the file name (after the last dot) in lower case,
    or an empty string if it has none
    */
    static String getSuffix(const String& path);

    /**
    @param definition dialog definition with a `fileChooser` element (i.e.
    `scene.import` of `etc/gui/dialogs.json`), or null
    @return the filters of its `fileFilter` elements, in order (none for a
    null definition)
    */
    static std::vector<FileSuffixFilter> fromDialog(
        const WidgetDialog* definition);

    /**
    @param file file or folder
    @return true for folders and for files with the suffix of this filter
    */
    bool accept(const File& file) const;

    /**
    @return the description of the kind of files, with the suffix
    */
    String getDescription() const;
};

#endif
