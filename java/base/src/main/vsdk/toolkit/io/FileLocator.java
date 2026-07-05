package vsdk.toolkit.io;

import java.io.File;
import java.util.ArrayList;

public class FileLocator {
    private ArrayList<String> searchPaths;

    public FileLocator() {
        searchPaths = new ArrayList<String>();
    }

    public void clearSearchPaths() {
        searchPaths.clear();
    }

    public void addSearchPath(String path) {
        searchPaths.add(path);
    }

    public ArrayList<String> getSearchPaths() {
        return searchPaths;
    }

    public File locate(String filename) {
        File candidate = new File(filename);
        if (candidate.canRead()) {
            return candidate;
        }

        for (int i = 0; i < searchPaths.size(); i++) {
            candidate = new File(searchPaths.get(i) + File.separator + filename);
            if (candidate.canRead()) {
                return candidate;
            }
        }
        return null;
    }
}
