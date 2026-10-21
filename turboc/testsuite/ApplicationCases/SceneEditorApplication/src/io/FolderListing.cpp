#include <algorithm>
#include <limits.h>
#include <stdlib.h>
#include <dirent.h>
#include <sys/stat.h>

#include "io/FolderListing.h"

bool FolderListing::isFolder(const std::string& path)
{
    struct stat status;
    return stat(path.c_str(), &status) == 0 && S_ISDIR(status.st_mode);
}

std::string FolderListing::normalizeFolder(const std::string& folder)
{
    char resolved[PATH_MAX];
    if ( realpath(folder.c_str(), resolved) != NULL ) {
        return resolved;
    }
    return folder;
}

bool FolderListing::accepts(const std::vector<FileSuffixFilter>& filters,
                            const std::string& path)
{
    if ( filters.empty() || isFolder(path) ) {
        return true;
    }
    File file(path.c_str());
    for ( size_t i = 0; i < filters.size(); i++ ) {
        if ( filters[i].accept(file) ) {
            return true;
        }
    }
    return false;
}

void FolderListing::list(const std::string& folder,
                         const std::vector<FileSuffixFilter>& filters,
                         std::vector<std::string>& folders,
                         std::vector<std::string>& files)
{
    folders.clear();
    files.clear();
    DIR* directory = opendir(folder.c_str());
    if ( directory == NULL ) {
        return;
    }
    struct dirent* entry;
    while ( (entry = readdir(directory)) != NULL ) {
        std::string name = entry->d_name;
        if ( name.empty() || name[0] == '.' ) {
            continue;
        }
        std::string path = folder + "/" + name;
        if ( isFolder(path) ) {
            folders.push_back(name);
        }
        else if ( accepts(filters, path) ) {
            files.push_back(name);
        }
    }
    closedir(directory);
    std::sort(folders.begin(), folders.end());
    std::sort(files.begin(), files.end());
}
