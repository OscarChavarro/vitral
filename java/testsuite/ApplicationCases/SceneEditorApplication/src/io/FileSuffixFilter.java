package io;

import java.io.File;

/**
Accepts the files with a given suffix (extension), and the folders so the
user can browse them, as file choosers do. It does not depend on any GUI
technology: each one adapts it to its file chooser.
*/
public class FileSuffixFilter
{
    private final String suffix;
    private final String description;

    /**
    @param suffix accepted suffix, without the dot and in lower case
    @param description text describing the kind of files
    */
    public FileSuffixFilter(String suffix, String description)
    {
        this.suffix = suffix;
        this.description = description;
    }

    /**
    @param path path of a file
    @return the suffix of the file name (after the last dot) in lower case,
    or null if it has none
    */
    public static String getSuffix(String path)
    {
        String suffix = null;
        int i = path.lastIndexOf('.');

        if ( i > 0 && i < path.length() - 1 ) {
            suffix = path.substring(i + 1).toLowerCase();
        }
        return suffix;
    }

    /**
    @param file file or folder
    @return true for folders and for files with the suffix of this filter
    */
    public boolean accept(File file)
    {
        if ( file.isDirectory() ) {
            return true;
        }
        return suffix != null && suffix.equals(getSuffix(file.getPath()));
    }

    /**
    @return the description of the kind of files, with the suffix
    */
    public String getDescription()
    {
        return description + " (*." + suffix + ")";
    }
}
