//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 11 2005 - Gabriel Sarmiento & Lina Rojas: Original base       =
//=                     version                                             =
//= - November 1 2005 - Oscar Chavarro: Quality check - comments added      =
//= - August 6 2006 - Oscar Chavarro: toString method added                 =
//===========================================================================

package vsdk.toolkit.common;

/**
The QualitySelection class is used to indicate some attributes in which a 
geometry is to be displayed in screen. The QualitySelection class is not
responsible of display any data, it is just an a suggestion on how to draw it.
*/
public class QualitySelection extends Entity {

    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    public static final int SHADING_TYPE_NOLIGHT  = 0;
    public static final int SHADING_TYPE_FLAT     = 1;
    public static final int SHADING_TYPE_GOURAUD  = 2;
    public static final int SHADING_TYPE_PHONG    = 3;

    // Indicates the type of shading algorithm to apply in shaders. This must
    // be one of the values defined in the constants SHADING_TYPE_* of this
    // class.
    private int shadingType;

    private boolean surfaces;
    private boolean wires;
    private boolean boundingVolume;
    private boolean texture;
    private boolean bumpMap;
    private boolean points;
    private boolean normals;
    private boolean trianglesNormals;

    // To be used in future: depending of the rendering implementation for
    // each geometry, this can be used to specify the desired amount of
    // primitives to generate in dynamic or progressive geometry rendering
    // algorithms.
    private int lodHint;

    public void setLodHint(int l)
    {
        lodHint = l;
    }

    public int getLodHint()
    {
        return lodHint;
    }

    /**
    Constructs a default QualitySelection object that will display the surfaces
    */
    public QualitySelection() {
        shadingType = SHADING_TYPE_GOURAUD;

        surfaces = true;
        wires = false;
        boundingVolume = false;
        texture = true;
        bumpMap = false;
        points = false;
        normals = false;
        trianglesNormals = false;
    }

    /**
    Contructs a QualitySelection object with the specified parameers
    @param surfaces Indicates wether or not the surface of the object is to be 
           displayed
    @param wires Indicates wether or not the edges of the faces that compose 
           the object are to be displayed
    @param boundingVolume Indicates wether or not the bounding volume of the 
           object is to be displayed
    @param texture Indicates wether or not the object is to be displayed using 
           texture mapping
    @param bumpMap Indicates wether or not the object is to be displayed using 
           bump mapping
    @param points Indicates wether or not the vertexes of the object are to be 
           displayed
    @param normals Indicates wether or not the normals of the object are to be 
           displayed
    @param trianglesNormals Indicates wether or not the face normals of the 
           object are to be displayed
    @param shadingType Indicates the behavior of the lighting calculation for 
           this object
    */
    public QualitySelection(boolean surfaces,
                            boolean wires,
                            boolean boundingVolume,
                            boolean texture,
                            boolean bumpMap,
                            boolean points,
                            boolean normals,
                            boolean trianglesNormals, 
                            int shadingType)
    {
        this.surfaces = surfaces;
        this.wires = wires;
        this.boundingVolume = boundingVolume;
        this.texture = texture;
        this.bumpMap = bumpMap;
        this.points = points;
        this.normals = normals;
        this.shadingType = shadingType;
        this.trianglesNormals = trianglesNormals;
    }

    /**
    Selects wether or not to display the surface of the object
    @param b The selection value, true indicates that the quality should be 
    displayed and false indicates that it shouldn't
    */
    public void setSurfaces(boolean b)
    {
        this.surfaces = b;
    }

    public void setWires(boolean b)
    {
        this.wires = b;
    }

    public void setBoundingVolume(boolean b)
    {
        this.boundingVolume = b;
    }

    public void setTexture(boolean b)
    {
        this.texture = b;
    }

    public void setBumpMap(boolean b)
    {
        this.bumpMap = b;
    }

    public void setPoints(boolean b)
    {
        this.points = b;
    }

    public void setNormals(boolean b)
    {
        this.normals = b;
    }

    public void setTrianglesNormals(boolean b)
    {
        this.trianglesNormals = b;
    }

    public void setShadingType(int shadingType)
    {
        this.shadingType = shadingType;
    }

    public boolean isSurfacesSet()
    {
        return this.surfaces;
    }

    public boolean isWiresSet()
    {
        return this.wires;
    }

    public boolean isBoundingVolumeSet()
    {
        return this.boundingVolume;
    }

    public boolean isTextureSet()
    {
        return this.texture;
    }

    public boolean isBumpMapSet()
    {
        return this.bumpMap;
    }

    public boolean isPointsSet()
    {
        return this.points;
    }

    public boolean isNormalsSet()
    {
        return this.normals;
    }

    public boolean isTrianglesNormalsSet()
    {
        return this.trianglesNormals;
    }

    public int getShadingType()
    {
        return this.shadingType;
    }

    public void changeSurfaces()
    {
        surfaces = !surfaces;
    }

    public void changeWires()
    {
        wires = !wires;
    }

    public void changeBoundingVolume()
    {
        boundingVolume = !boundingVolume;
    }

    public void changeTexture()
    {
        texture = !texture;
    }

    public void changeBumpMap()
    {
        bumpMap = !bumpMap;
    }

    public void changePoints()
    {
        points = !points;
    }

    public void changeNormals()
    {
        normals = !normals;
    }

    public void changeTrianglesNormals()
    {
        this.trianglesNormals = !this.trianglesNormals;
    }

    public void changeShadingType()
    {
        shadingType++;
        if ( shadingType == 4 ) {
            shadingType = 0;
        }
    }

    public String toString()
    {
        String msg;

        msg = "<QualitySelection>:\n";

        msg = msg + "  - Shading type: ";
        switch ( shadingType ) {
          case SHADING_TYPE_NOLIGHT:
            msg = msg + "LIGHTING DISABLED (ONLY AMBIENT COLOR)\n";
            break;
          case SHADING_TYPE_FLAT:
            msg = msg + "FLAT\n";
            break;
          case SHADING_TYPE_GOURAUD:
            msg = msg + "GOURAUD\n";
            break;
          case SHADING_TYPE_PHONG:
            msg = msg + "PHONG\n";
            break;
          default:
            msg = msg + "INVALID!\n";
            break;
        }

        msg = msg + "  - Draw points: " + (points?"ON":"OFF") + "\n";
        msg = msg + "  - Draw wires: " + (wires?"ON":"OFF") + "\n";
        msg = msg + "  - Draw surfaces: " + (surfaces?"ON":"OFF") + "\n";
        msg = msg + "  - Draw bounding volume: " + (boundingVolume?"ON":"OFF") + "\n";
        msg = msg + "  - Draw normals: " + (normals?"ON":"OFF") + "\n";
        msg = msg + "  - Draw triangles normals: " + (trianglesNormals?"ON":"OFF") + "\n";
        msg = msg + "  - With texture: " + (texture?"ON":"OFF") + "\n";
        msg = msg + "  - With bump map: " + (bumpMap?"ON":"OFF") + "\n";

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
