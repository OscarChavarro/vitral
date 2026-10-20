#include <cctype>

#include "vsdk/toolkit/gui/dialog/components/DialogFileFilter.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
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

std::vector<FileSuffixFilter> FileSuffixFilter::fromDialog(
    const WidgetDialog* definition)
{
    std::vector<FileSuffixFilter> filters;
    if ( definition == nullptr ) {
        return filters;
    }
    const std::vector<WidgetDialogElement>& elements = definition->getElements();
    for ( size_t i = 0; i < elements.size(); i++ ) {
        if ( !elements[i].getKind().equals("fileChooser") ) {
            continue;
        }
        const std::vector<WidgetDialogElement>& children =
            elements[i].getChildren();
        for ( size_t j = 0; j < children.size(); j++ ) {
            if ( !children[j].getKind().equals("fileFilter") ) {
                continue;
            }
            // The label of a filter is its description, as in Java
            DialogFileFilter policy(children[j].getBinding(),
                                    children[j].getLabelKey());
            filters.push_back(FileSuffixFilter(policy.getSuffix(),
                                               children[j].getLabelKey()));
        }
    }
    return filters;
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
