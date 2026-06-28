package vsdk.toolkit.media.solidTexture.from2d;

import vsdk.toolkit.media.RGBAImageHDRUncompressed;

public final class SolidTextureCoordinateMapper
{
    public boolean map(double x, double y, double z, ControlledRGBAImageHDRUncompressed image,
                       double[] xCoordinate, double[] yCoordinate, double smallTolerance)
    {
        boolean ok = switch ( image.getMapTypeEnum() ) {
            case PLANAR_MAP -> planarImageMap(x, y, z, image, xCoordinate, yCoordinate);
            case SPHERICAL_MAP -> sphericalImageMap(x, y, z, image, xCoordinate, yCoordinate);
            case CYLINDRICAL_MAP -> cylindricalImageMap(x, y, z, image, xCoordinate, yCoordinate);
            case TORUS_MAP -> torusImageMap(x, y, z, image, xCoordinate, yCoordinate);
        };
        if ( !ok ) {
            return true;
        }

        yCoordinate[0] += smallTolerance;
        xCoordinate[0] += smallTolerance;
        yCoordinate[0] = image.getYSize() - yCoordinate[0];

        if ( xCoordinate[0] < 0.0 ) {
            xCoordinate[0] += image.getXSize();
        }
        else if ( xCoordinate[0] >= image.getXSize() ) {
            xCoordinate[0] -= image.getXSize();
        }
        if ( yCoordinate[0] < 0.0 ) {
            yCoordinate[0] += image.getYSize();
        }
        else if ( yCoordinate[0] >= image.getYSize() ) {
            yCoordinate[0] -= image.getYSize();
        }

        if ( xCoordinate[0] >= image.getXSize() || yCoordinate[0] >= image.getYSize() ||
             xCoordinate[0] < 0.0 || yCoordinate[0] < 0.0 ) {
            throw new IllegalStateException("Picture index out of range");
        }
        return false;
    }

    private boolean cylindricalImageMap(double x, double y, double z,
                                        ControlledRGBAImageHDRUncompressed image,
                                        double[] u, double[] v)
    {
        if ( image.getOnceFlag() && (y < 0.0 || y > 1.0) ) {
            return false;
        }
        v[0] = (y * image.getYSize()) % image.getYSize();
        double len = Math.sqrt(x * x + y * y + z * z);
        if ( len == 0.0 ) {
            return false;
        }
        x /= len;
        z /= len;

        len = Math.sqrt(x * x + z * z);
        if ( len == 0.0 ) {
            return false;
        }
        double theta;
        if ( z == 0.0 ) {
            theta = x > 0.0 ? 0.0 : Math.PI;
        }
        else {
            theta = Math.acos(x / len);
            if ( z < 0.0 ) {
                theta = 2.0 * Math.PI - theta;
            }
        }
        theta /= 2.0 * Math.PI;
        u[0] = theta * image.getXSize();
        return true;
    }

    private boolean torusImageMap(double x, double y, double z,
                                  ControlledRGBAImageHDRUncompressed image,
                                  double[] u, double[] v)
    {
        double r0 = image.getImageGradient().x();
        double len = Math.sqrt(x * x + z * z);
        if ( len == 0.0 ) {
            return false;
        }
        double theta;
        if ( z == 0.0 ) {
            theta = x > 0.0 ? 0.0 : Math.PI;
        }
        else {
            theta = Math.acos(x / len);
            if ( z < 0.0 ) {
                theta = 2.0 * Math.PI - theta;
            }
        }
        theta = 0.0 - theta;

        x = len - r0;
        len = Math.sqrt(x * x + y * y);
        double phi = Math.acos(-x / len);
        if ( y > 0.0 ) {
            phi = 2.0 * Math.PI - phi;
        }
        theta /= 2.0 * Math.PI;
        phi /= 2.0 * Math.PI;
        u[0] = -theta * image.getXSize();
        v[0] = phi * image.getYSize();
        return true;
    }

    private boolean sphericalImageMap(double x, double y, double z, RGBAImageHDRUncompressed image,
                                      double[] u, double[] v)
    {
        double len = Math.sqrt(x * x + y * y + z * z);
        if ( len == 0.0 ) {
            return false;
        }
        x /= len;
        y /= len;
        z /= len;

        double phi = 0.5 + Math.asin(y) / Math.PI;
        len = Math.sqrt(x * x + z * z);
        double theta;
        if ( len == 0.0 ) {
            theta = 0.0;
        }
        else if ( z == 0.0 ) {
            theta = x > 0.0 ? 0.0 : Math.PI;
        }
        else {
            theta = Math.acos(x / len);
            if ( z < 0.0 ) {
                theta = 2.0 * Math.PI - theta;
            }
            theta /= 2.0 * Math.PI;
        }
        u[0] = theta * image.getXSize();
        v[0] = phi * image.getYSize();
        return true;
    }

    private boolean planarImageMap(double x, double y, double z,
                                   ControlledRGBAImageHDRUncompressed image,
                                   double[] u, double[] v)
    {
        if ( image.getImageGradient().x() != 0.0 ) {
            if ( image.getOnceFlag() && (x < 0.0 || x > 1.0) ) return false;
            if ( image.getImageGradient().x() > 0.0 ) u[0] = (x * image.getXSize()) % image.getXSize();
            else v[0] = (x * image.getYSize()) % image.getYSize();
        }
        if ( image.getImageGradient().y() != 0.0 ) {
            if ( image.getOnceFlag() && (y < 0.0 || y > 1.0) ) return false;
            if ( image.getImageGradient().y() > 0.0 ) u[0] = (y * image.getXSize()) % image.getXSize();
            else v[0] = (y * image.getYSize()) % image.getYSize();
        }
        if ( image.getImageGradient().z() != 0.0 ) {
            if ( image.getOnceFlag() && (z < 0.0 || z > 1.0) ) return false;
            if ( image.getImageGradient().z() > 0.0 ) u[0] = (z * image.getXSize()) % image.getXSize();
            else v[0] = (z * image.getYSize()) % image.getYSize();
        }
        return true;
    }
}
