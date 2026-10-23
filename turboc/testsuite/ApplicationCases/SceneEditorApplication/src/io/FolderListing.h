#ifndef __FOLDER_LISTING__
#define __FOLDER_LISTING__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

#include "io/FileSuffixFilter.h"

/**
Contents of a folder as the file choosers of the application present them
(as `JFileChooser` does): its visible subfolders and the files accepted by
a set of filters, sorted by name. It does not depend on any GUI
technology: the file dialogs of each widget set use it.
*/
class FolderListing {
public:
    /**
    @return true if the path exists and is a folder
    */
    static BOOLEAN isFolder(const String& path);

    /**
    @return the folder without "." and ".." components, as
    `File.getCanonicalPath`, or the folder if it does not exist
    */
    static String normalizeFolder(const String& folder);

    /**
    @param filters accepted kinds of files, or none for all of them
    @param path path of a file or folder
    @return true if the path is a folder or a file accepted by any filter
    */
    static BOOLEAN accepts(const ArrayList<FileSuffixFilter>& filters,
                        const String& path);

    /**
    Lists a folder, skipping hidden entries (whose names start with a dot).
    @param folder folder to list
    @param filters accepted kinds of files, or none for all of them
    @param folders names of its subfolders, sorted
    @param files names of its accepted files, sorted
    */
    static void list(const String& folder,
                     const ArrayList<FileSuffixFilter>& filters,
                     ArrayList<String>& folders,
                     ArrayList<String>& files);

private:
    FolderListing();
};

#endif
