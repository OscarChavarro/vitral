package vsdk.toolkit.render.androidgles10;

// Java basic classes
import java.util.ArrayList;

// VSDK classes
import vsdk.toolkit.common.ArrayListOfInts;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Material;

/**
As OpenGL ES 2.0 does not support directly display lists, this class is aimed
to help the creation of a construction similar to display lists, based upon
binded vertex buffer objects (VBOs) and indexed arrays.
*/
public class AndroidGLES10DisplayList extends AndroidGLES10Renderer {
    /// vboMaterials should be the same size as vboIds
    /// and can contain null references. If first material is null, default
    /// material is assumed. If a middle positioned material is null, it is
    /// asumed as no change in material properties for next fragment.
    private ArrayList<Material> vboMaterials;
    
    /// Open GL ES 2.0 vertex buffer object ids list.
    private ArrayListOfInts vboIds; 
    
    /// Open GL ES 2.0 index buffer object ids list.
    private ArrayListOfInts iboIds; 
    
    /// Size in vertices
    private ArrayListOfInts vboSizes;

    /// Size in primitives
    private ArrayListOfInts iboSizes;

    /// Primitive to be used on vertex buffered objects
    private ArrayListOfInts vboPrimitives;
    
    /// Gets invalidated if user changes rendering configuration.
    private RendererConfiguration correspondingQuality;
    
    /// One of the several encaptulation for vertex data
    private int vertexMode;
    
    public AndroidGLES10DisplayList(RendererConfiguration q) {
        vboMaterials = new ArrayList<Material>();
        vboIds = new ArrayListOfInts(10);
        vboSizes = new ArrayListOfInts(10);
        vboPrimitives = new ArrayListOfInts(10);
        iboIds = new ArrayListOfInts(10);
        iboSizes = new ArrayListOfInts(10);
        vertexMode = MODE_3POSITION_3COLOR_3NORMAL_2UV;
        if ( q!= null ) {
            correspondingQuality = q.clone();
        }
        else {
            correspondingQuality = null;
        }
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

    /**
    @return the vboIds
    */
    public ArrayListOfInts getVboIds() {
        return vboIds;
    }

    public void addVbo(Material material, int id, int primitive, int size) {
        vboMaterials.add(material);
        vboIds.add(id);
        vboPrimitives.add(primitive);
        getVboSizes().add(size);
    }
    
    public void addIbos(int ibo, int size) {
        iboIds.add(ibo);
        getIboSizes().add(size);
    }

    
    /**
    @return the vboSizes
    */
    public ArrayListOfInts getVboSizes() {
        return vboSizes;
    }

    /**
    @return the vboPrimitives
    */
    public ArrayListOfInts getVboPrimitives() {
        return vboPrimitives;
    }

    /**
    @return the vertexMode
    */
    public int getVertexMode() {
        return vertexMode;
    }

    /**
    @param vertexMode the vertexMode to set
    */
    public void setVertexMode(int vertexMode) {
        this.vertexMode = vertexMode;
    }

    /**
    @return the iboIds
    */
    public ArrayListOfInts getIndexIds() {
        return iboIds;
    }

    public ArrayListOfInts getIboSizes() {
        return iboSizes;
    }

}
