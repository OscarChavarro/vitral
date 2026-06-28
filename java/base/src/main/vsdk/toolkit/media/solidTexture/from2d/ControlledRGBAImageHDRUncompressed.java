package vsdk.toolkit.media.solidTexture.from2d;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.IndexedColorImageHDRUncompressed;
import vsdk.toolkit.media.RGBAImageHDRUncompressed;

public class ControlledRGBAImageHDRUncompressed extends RGBAImageHDRUncompressed
{
    private ImageToSolidTextureProjectionMethods mapType;
    private ImageToSolidTextureInterpolationTypes interpolationType;
    private boolean onceFlag;
    private boolean useColorFlag;
    private Vector3Dd imageGradient;
    private IndexedColorImageHDRUncompressed indexedData;

    public ControlledRGBAImageHDRUncompressed()
    {
        mapType = ImageToSolidTextureProjectionMethods.PLANAR_MAP;
        interpolationType = ImageToSolidTextureInterpolationTypes.NO_INTERPOLATION;
        onceFlag = false;
        useColorFlag = true;
        imageGradient = new Vector3Dd();
        indexedData = null;
    }

    public int getMapType()
    {
        return mapType.value();
    }

    public ImageToSolidTextureProjectionMethods getMapTypeEnum()
    {
        return mapType;
    }

    public void setMapType(ImageToSolidTextureProjectionMethods v)
    {
        mapType = v;
    }

    public void setMapType(int v)
    {
        mapType = ImageToSolidTextureProjectionMethods.fromInt(v);
    }

    public int getInterpolationType()
    {
        return interpolationType.value();
    }

    public ImageToSolidTextureInterpolationTypes getInterpolationTypeEnum()
    {
        return interpolationType;
    }

    public void setInterpolationType(ImageToSolidTextureInterpolationTypes v)
    {
        interpolationType = v;
    }

    public void setInterpolationType(int v)
    {
        interpolationType = ImageToSolidTextureInterpolationTypes.fromInt(v);
    }

    public boolean getOnceFlag()
    {
        return onceFlag;
    }

    public void setOnceFlag(boolean v)
    {
        onceFlag = v;
    }

    public boolean getUseColorFlag()
    {
        return useColorFlag;
    }

    public void setUseColorFlag(boolean v)
    {
        useColorFlag = v;
    }

    public Vector3Dd getImageGradient()
    {
        return imageGradient;
    }

    public void setImageGradient(Vector3Dd v)
    {
        imageGradient = v;
    }

    public IndexedColorImageHDRUncompressed getIndexedData()
    {
        return indexedData;
    }

    public void setIndexedData(IndexedColorImageHDRUncompressed v)
    {
        indexedData = v;
    }
}
