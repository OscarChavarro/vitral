//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 11 2005 - Gabriel Sarmiento & Lina Rojas: Original base       =
//=                     version                                             =
//= - November 1 2005 - Oscar Chavarro: Quality check - comments added      =
//===========================================================================

package vsdk.toolkit.common;

public class QualitySelection {

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

    public QualitySelection() {
        shadingType = SHADING_TYPE_FLAT;

        surfaces = true;
        wires = false;
        boundingVolume = false;
        texture = false;
        bumpMap = false;
        points = false;
        normals = false;
        trianglesNormals = false;
    }

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

}
