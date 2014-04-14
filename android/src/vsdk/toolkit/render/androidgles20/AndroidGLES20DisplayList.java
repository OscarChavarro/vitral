//===========================================================================
package vsdk.toolkit.render.androidgles20;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import vsdk.toolkit.common.RendererConfiguration;

import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

/**
As OpenGL ES 2.0 does not support directly display lists, this class is aimed
to help the creation of a construction similar to display lists, based upon
binded vertex buffer objects (VBOs) and indexed arrays.
*/
public class AndroidGLES20DisplayList extends AndroidGLES20Renderer {
    /// vboMaterials should be the same size as directVertexBufferObjects
    /// and can contain null references. If first material is null, default
    /// material is assumed. If a middle positioned material is null, it is
    /// asumed as no change in material properties for next fragment.
    private ArrayList<Material> vboMaterials;
    private ArrayList<FloatBuffer> directVertexBufferObjects;
    
    /// Gets invalidated if user changes rendering configuration.
    private RendererConfiguration correspondingQuality;

    AndroidGLES20DisplayList(RendererConfiguration q) {
        vboMaterials = new ArrayList<Material>();
        directVertexBufferObjects = new ArrayList<FloatBuffer>();
        correspondingQuality = q.clone();
    }

    /**
    @return the directVertexBufferObjects
    */
    public ArrayList<FloatBuffer> getDirectVertexBufferObjects() {
        return directVertexBufferObjects;
    }

    /**
    @param directVertexBufferObjects the directVertexBufferObjects to set
    */
    public void setDirectVertexBufferObjects(ArrayList<FloatBuffer> directVertexBufferObjects) {
        this.directVertexBufferObjects = directVertexBufferObjects;
    }

    /**
    @return the vboMaterials
    */
    public ArrayList<Material> getVboMaterials() {
        return vboMaterials;
    }

    /**
    @param vboMaterials the vboMaterials to set
    */
    public void setVboMaterials(ArrayList<Material> vboMaterials) {
        this.vboMaterials = vboMaterials;
    }

    /**
    @return the correspondingQuality
    */
    public RendererConfiguration getCorrespondingQuality() {
        return correspondingQuality;
    }

    /**
    @param correspondingQuality the correspondingQuality to set
    */
    public void setCorrespondingQuality(RendererConfiguration correspondingQuality) {
        this.correspondingQuality = correspondingQuality;
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
