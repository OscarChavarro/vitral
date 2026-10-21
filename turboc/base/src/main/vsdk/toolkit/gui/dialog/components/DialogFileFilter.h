#ifndef __DIALOG_FILE_FILTER__
#define __DIALOG_FILE_FILTER__

#include "java/lang/String.h"

/**
File suffix policy shared by dialog hosts, independent of file APIs: the
`fileFilter` elements of the `fileChooser` of a dialog definition.
*/
class DialogFileFilter {
private:
    String suffix;
    String text;

public:
    /**
    @param suffix accepted suffix, without the dot (compared ignoring case)
    @param description text of the filter
    */
    DialogFileFilter(const String& suffix,
                     const String& description);

    /**
    @return true for folders and for the files with the suffix
    */
    bool accepts(const String& path, bool directory) const;
    const String& getSuffix() const;

    /**
    @return the description with the pattern, i.e. "obj mesh (*.obj)"
    */
    String description() const;
};

#endif
