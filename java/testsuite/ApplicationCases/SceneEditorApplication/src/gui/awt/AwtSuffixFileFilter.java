package gui.awt;

import java.io.File;

import javax.swing.filechooser.FileFilter;

// Application classes
import io.FileSuffixFilter;

/**
Swing adapter of a `FileSuffixFilter`, for `JFileChooser`.
*/
public class AwtSuffixFileFilter extends FileFilter
{
    private final FileSuffixFilter filter;

    /**
    @param suffix accepted suffix, without the dot and in lower case
    @param description text describing the kind of files
    */
    public AwtSuffixFileFilter(String suffix, String description)
    {
        filter = new FileSuffixFilter(suffix, description);
    }

    @Override
    public boolean accept(File f)
    {
        return filter.accept(f);
    }

    @Override
    public String getDescription()
    {
        return filter.getDescription();
    }
}
