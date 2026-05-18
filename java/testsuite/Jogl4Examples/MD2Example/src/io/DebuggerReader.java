package io;

import java.io.IOException;

import vsdk.toolkit.environment.geometry.surface.Md2Mesh;
import vsdk.toolkit.io.geometry.Md2Persistence;

public class DebuggerReader {
    private static final double TEXTURE_GAMMA_CORRECTION = 2.0;
    public void readMd2WithTexture(String md2Path, String texturePath, Md2Mesh md2Mesh)
        throws IOException {
        Md2Persistence md2Persistence = new Md2Persistence();
        md2Persistence.read(md2Path, texturePath, md2Mesh, TEXTURE_GAMMA_CORRECTION);
        md2Mesh.setCurrentAnimationInd((short)0);
    }
}
