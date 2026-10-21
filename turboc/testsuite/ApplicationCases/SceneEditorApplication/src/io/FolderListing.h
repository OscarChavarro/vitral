#ifndef __FOLDER_LISTING__
#define __FOLDER_LISTING__

#include <string>
#include <vector>

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
    static bool isFolder(const std::string& path);

    /**
    @return the folder without "." and ".." components, as
    `File.getCanonicalPath`, or the folder if it does not exist
    */
    static std::string normalizeFolder(const std::string& folder);

    /**
    @param filters accepted kinds of files, or none for all of them
    @param path path of a file or folder
    @return true if the path is a folder or a file accepted by any filter
    */
    static bool accepts(const std::vector<FileSuffixFilter>& filters,
                        const std::string& path);

    /**
    Lists a folder, skipping hidden entries (whose names start with a dot).
    @param folder folder to list
    @param filters accepted kinds of files, or none for all of them
    @param folders names of its subfolders, sorted
    @param files names of its accepted files, sorted
    */
    static void list(const std::string& folder,
                     const std::vector<FileSuffixFilter>& filters,
                     std::vector<std::string>& folders,
                     std::vector<std::string>& files);

private:
    FolderListing();
};

#endif
