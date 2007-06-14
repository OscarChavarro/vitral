package util.filters;

import java.io.File;

public abstract class SuffixAwareFilter
    extends javax.swing.filechooser.FileFilter {

  public String getSuffix(File f) {
    String s = f.getPath(), suffix = null;
    int i = s.lastIndexOf('.');

    if (i > 0 && i < s.length() - 1) {
      suffix = s.substring(i + 1).toLowerCase();
    }

    return suffix;
  }

  public boolean accept(File f) {
    return f.isDirectory();
  }

}
