package vsdk.toolkit.render.jogl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

final class Jogl4ShaderLoader {
    private Jogl4ShaderLoader() {
    }

    static String readShaderSource(String shaderFileName)
    {
        List<Path> candidates = new ArrayList<>();
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

        candidates.add(Paths.get("../../../etc/glslShaders", shaderFileName));
        candidates.add(Paths.get("etc", "glslShaders", shaderFileName));

        Path cursor = cwd;
        for ( int i = 0; i < 6; i++ ) {
            candidates.add(cursor.resolve("etc").resolve("glslShaders").resolve(shaderFileName));
            if ( cursor.getParent() == null ) {
                break;
            }
            cursor = cursor.getParent();
        }

        for ( Path path : candidates ) {
            if ( Files.exists(path) ) {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                }
                catch (IOException e) {
                    throw new IllegalStateException("Failed to read shader: " + path, e);
                }
            }
        }

        throw new IllegalStateException(
            "Shader not found: " + shaderFileName + " (searched " + candidates + ")");
    }
}
