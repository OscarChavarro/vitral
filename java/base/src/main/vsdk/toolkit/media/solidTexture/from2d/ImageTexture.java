package vsdk.toolkit.media.solidTexture.from2d;

import vsdk.toolkit.common.color.ColorRgba;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.IndexedColorImageHDRUncompressed;
import vsdk.toolkit.media.RGBAPixelHDR;

public final class ImageTexture
{
    private static final int MAX_PTS = 4;

    public void imageMap(double x, double y, double z, ControlledRGBAImageHDRUncompressed image,
                         ColorRgba color, double smallTolerance)
    {
        SolidTextureCoordinateMapper mapper = new SolidTextureCoordinateMapper();
        double[] xc = {0.0};
        double[] yc = {0.0};
        int[] regNumber = {0};
        if ( mapper.map(x, y, z, image, xc, yc, smallTolerance) ) {
            color.setR(1.0);
            color.setG(1.0);
            color.setB(1.0);
            color.setA(1.0);
            return;
        }
        imageColorAt(image, xc[0], yc[0], color, regNumber);
    }

    public int materialMap(Vector3Dd intersectionPoint, Matrix4x4d textureTransformationInverse,
                           ControlledRGBAImageHDRUncompressed materialImage,
                           int numberOfMaterials, double smallTolerance)
    {
        Vector3Dd transformedPoint;
        if ( textureTransformationInverse != null ) {
            transformedPoint = textureTransformationInverse.transpose().multiply(intersectionPoint);
        }
        else {
            transformedPoint = intersectionPoint;
        }

        SolidTextureCoordinateMapper mapper = new SolidTextureCoordinateMapper();
        double[] xc = {0.0};
        double[] yc = {0.0};
        int[] regNumber = {0};
        int materialNumber;
        if ( mapper.map(transformedPoint.x(), transformedPoint.y(), transformedPoint.z(),
                        materialImage, xc, yc, smallTolerance) ) {
            materialNumber = 0;
        }
        else {
            ColorRgba color = new ColorRgba();
            imageColorAt(materialImage, xc[0], yc[0], color, regNumber);
            if ( materialImage.getIndexedData() == null ) {
                materialNumber = (int)color.getR() * 255;
            }
            else {
                materialNumber = regNumber[0];
            }
        }
        if ( numberOfMaterials > 0 && materialNumber >= numberOfMaterials ) {
            materialNumber %= numberOfMaterials;
        }
        if ( materialNumber < numberOfMaterials ) {
            return materialNumber;
        }
        return -1;
    }

    public Vector3Dd bumpMap(double x, double y, double z, ControlledRGBAImageHDRUncompressed bumpImage,
                             double bumpAmount, Vector3Dd normal, double smallTolerance)
    {
        SolidTextureCoordinateMapper mapper = new SolidTextureCoordinateMapper();
        double[] xc = {0.0};
        double[] yc = {0.0};
        if ( mapper.map(x, y, z, bumpImage, xc, yc, smallTolerance) ) {
            return normal;
        }

        int[] index = {0};
        int[] index2 = {0};
        int[] index3 = {0};
        ColorRgba color = new ColorRgba();
        ColorRgba color2 = new ColorRgba();
        ColorRgba color3 = new ColorRgba();
        imageColorAt(bumpImage, xc[0], yc[0], color, index);

        xc[0]--;
        yc[0]++;
        wrap(bumpImage, xc, yc);
        imageColorAt(bumpImage, xc[0], yc[0], color2, index2);

        xc[0] += 2.0;
        wrap(bumpImage, xc, yc);
        imageColorAt(bumpImage, xc[0], yc[0], color3, index3);

        Vector3Dd p1;
        Vector3Dd p2;
        Vector3Dd p3;
        if ( bumpImage.getIndexedData() == null || bumpImage.getUseColorFlag() ) {
            p1 = new Vector3Dd(0.0, bumpAmount * luminance(color), 0.0);
            p2 = new Vector3Dd(0.0, bumpAmount * luminance(color2), 1.0);
            p3 = new Vector3Dd(1.0, bumpAmount * luminance(color3), 1.0);
        }
        else {
            p1 = new Vector3Dd(0.0, bumpAmount * index[0], 0.0);
            p2 = new Vector3Dd(0.0, bumpAmount * index2[0], 1.0);
            p3 = new Vector3Dd(1.0, bumpAmount * index3[0], 1.0);
        }

        Vector3Dd xPrime = p1.subtract(p2);
        Vector3Dd yPrime = p3.subtract(p2);
        Vector3Dd bumpNormal = yPrime.crossProduct(xPrime).normalized();

        yPrime = new Vector3Dd(normal.x(), normal.y(), normal.z());
        Vector3Dd temp = new Vector3Dd(0.0, 1.0, 0.0);
        xPrime = yPrime.crossProduct(temp);
        double length = xPrime.length();
        if ( length < 1.0e-9 ) {
            if ( Math.abs(normal.y() - 1.0) < smallTolerance ) {
                yPrime = new Vector3Dd(0.0, 1.0, 0.0);
            }
            else {
                yPrime = new Vector3Dd(0.0, -1.0, 0.0);
            }
            xPrime = new Vector3Dd(1.0, 0.0, 0.0);
            length = 1.0;
        }
        xPrime = xPrime.multiply(1.0 / length);
        Vector3Dd zPrime = xPrime.crossProduct(yPrime).normalized();
        xPrime = xPrime.multiply(bumpNormal.x());
        yPrime = yPrime.multiply(bumpNormal.y());
        zPrime = zPrime.multiply(bumpNormal.z());
        return xPrime.add(yPrime).add(zPrime).normalized();
    }

    public void bumpMap(double x, double y, double z, ControlledRGBAImageHDRUncompressed bumpImage,
                        double bumpAmount, Vector3Dd[] normal, double smallTolerance)
    {
        normal[0] = bumpMap(x, y, z, bumpImage, bumpAmount, normal[0], smallTolerance);
    }

    private void imageColorAt(ControlledRGBAImageHDRUncompressed image, double xCoordinate,
                              double yCoordinate, ColorRgba color, int[] index)
    {
        if ( image.getInterpolationTypeEnum() == ImageToSolidTextureInterpolationTypes.NO_INTERPOLATION ) {
            noInterpolation(image, xCoordinate, yCoordinate, color, index);
        }
        else {
            interp(image, xCoordinate, yCoordinate, color, index);
        }
    }

    private void noInterpolation(ControlledRGBAImageHDRUncompressed image, double xCoordinate,
                                 double yCoordinate, ColorRgba color, int[] index)
    {
        double[] xw = {xCoordinate};
        double[] yw = {yCoordinate};
        wrap(image, xw, yw);
        int x = (int)xw[0];
        int y = (int)yw[0];

        if ( image.getIndexedData() == null ) {
            RGBAPixelHDR pixel = new RGBAPixelHDR();
            image.getPixel(x, y, pixel);
            color.setR(color.getR() + pixel.r / 255.0);
            color.setG(color.getG() + pixel.g / 255.0);
            color.setB(color.getB() + pixel.b / 255.0);
            index[0] = -1;
        }
        else {
            IndexedColorImageHDRUncompressed idx = image.getIndexedData();
            index[0] = idx.getPixel(x, y);
            RGBAPixelHDR mapColor = idx.getColorTable()[index[0]];
            color.setR(color.getR() + mapColor.r / 255.0);
            color.setG(color.getG() + mapColor.g / 255.0);
            color.setB(color.getB() + mapColor.b / 255.0);
            color.setA(color.getA() + mapColor.a / 255.0);
        }
    }

    private void interp(ControlledRGBAImageHDRUncompressed image, double xCoordinate,
                        double yCoordinate, ColorRgba color, int[] index)
    {
        int[] cornersIndex = new int[4];
        double[] indexCrn = new double[4];
        ColorRgba[] cornerColor = {
            new ColorRgba(), new ColorRgba(), new ColorRgba(), new ColorRgba()
        };
        double[] redCrn = new double[4];
        double[] greenCrn = new double[4];
        double[] blueCrn = new double[4];
        double[] alphaCrn = new double[4];
        double val1 = 0.0;
        double val2 = 0.0;
        double val3 = 0.0;
        double val4 = 0.0;
        int x = (int)xCoordinate;
        int y = (int)yCoordinate;

        if ( image.getInterpolationTypeEnum() == ImageToSolidTextureInterpolationTypes.BI_LINEAR ) {
            cornersIndex[0] = noInterpolationAt(image, x + 1.0, y, cornerColor[0]);
            cornersIndex[1] = noInterpolationAt(image, x, y, cornerColor[1]);
            cornersIndex[2] = noInterpolationAt(image, x + 1.0, y - 1.0, cornerColor[2]);
            cornersIndex[3] = noInterpolationAt(image, x, y - 1.0, cornerColor[3]);
            fillChannels(cornerColor, redCrn, greenCrn, blueCrn, alphaCrn);
            val1 = biLinear(redCrn, xCoordinate, yCoordinate);
            val2 = biLinear(greenCrn, xCoordinate, yCoordinate);
            val3 = biLinear(blueCrn, xCoordinate, yCoordinate);
            val4 = biLinear(alphaCrn, xCoordinate, yCoordinate);
        }
        if ( image.getInterpolationTypeEnum() == ImageToSolidTextureInterpolationTypes.NORMALIZED_DIST ) {
            cornersIndex[0] = noInterpolationAt(image, x, y - 1.0, cornerColor[0]);
            cornersIndex[1] = noInterpolationAt(image, x + 1.0, y - 1.0, cornerColor[1]);
            cornersIndex[2] = noInterpolationAt(image, x, y, cornerColor[2]);
            cornersIndex[3] = noInterpolationAt(image, x + 1.0, y, cornerColor[3]);
            fillChannels(cornerColor, redCrn, greenCrn, blueCrn, alphaCrn);
            val1 = normDist(redCrn, xCoordinate, yCoordinate);
            val2 = normDist(greenCrn, xCoordinate, yCoordinate);
            val3 = normDist(blueCrn, xCoordinate, yCoordinate);
            val4 = normDist(alphaCrn, xCoordinate, yCoordinate);
        }

        color.setR(color.getR() + val1);
        color.setG(color.getG() + val2);
        color.setB(color.getB() + val3);
        color.setA(color.getA() + val4);
        for ( int i = 0; i < 4; i++ ) {
            indexCrn[i] = cornersIndex[i];
        }
        if ( image.getInterpolationTypeEnum() == ImageToSolidTextureInterpolationTypes.BI_LINEAR ) {
            index[0] = (int)(biLinear(indexCrn, xCoordinate, yCoordinate) + 0.5);
        }
        if ( image.getInterpolationTypeEnum() == ImageToSolidTextureInterpolationTypes.NORMALIZED_DIST ) {
            index[0] = (int)(normDist(indexCrn, xCoordinate, yCoordinate) + 0.5);
        }
    }

    private int noInterpolationAt(ControlledRGBAImageHDRUncompressed image, double x, double y,
                                  ColorRgba color)
    {
        int[] index = {0};
        noInterpolation(image, x, y, color, index);
        return index[0];
    }

    private double biLinear(double[] corners, double x, double y)
    {
        double p = x - (int)x;
        double q = y - (int)y;
        if ( p == 0.0 && q == 0.0 ) {
            return corners[0];
        }
        return p * q * corners[0] + q * (1.0 - p) * corners[1] +
            p * (1.0 - q) * corners[2] + (1.0 - p) * (1.0 - q) * corners[3];
    }

    private double normDist(double[] corners, double x, double y)
    {
        double p = x - (int)x;
        double q = y - (int)y;
        if ( p == 0.0 && q == 0.0 ) {
            return corners[0];
        }
        double[] wts = new double[MAX_PTS];
        wts[0] = pythagoreanSq(p, q);
        wts[1] = pythagoreanSq(1.0 - p, q);
        wts[2] = pythagoreanSq(p, 1.0 - q);
        wts[3] = pythagoreanSq(1.0 - p, 1.0 - q);
        double sumInvWts = 0.0;
        double sumI = 0.0;
        for ( int i = 0; i < MAX_PTS; i++ ) {
            sumInvWts += 1.0 / wts[i];
            sumI += corners[i] / wts[i];
        }
        return sumI / sumInvWts;
    }

    private static double pythagoreanSq(double a, double b)
    {
        return a * a + b * b;
    }

    private static void fillChannels(ColorRgba[] cornerColor, double[] redCrn,
                                     double[] greenCrn, double[] blueCrn,
                                     double[] alphaCrn)
    {
        for ( int i = 0; i < 4; i++ ) {
            redCrn[i] = cornerColor[i].getR();
            greenCrn[i] = cornerColor[i].getG();
            blueCrn[i] = cornerColor[i].getB();
            alphaCrn[i] = cornerColor[i].getA();
        }
    }

    private static double luminance(ColorRgba color)
    {
        return 0.229 * color.getR() + 0.587 * color.getG() + 0.114 * color.getB();
    }

    private static void wrap(ControlledRGBAImageHDRUncompressed image, double[] x, double[] y)
    {
        if ( x[0] < 0.0 ) x[0] += image.getXSize();
        else if ( x[0] >= image.getXSize() ) x[0] -= image.getXSize();
        if ( y[0] < 0.0 ) y[0] += image.getYSize();
        else if ( y[0] >= image.getYSize() ) y[0] -= image.getYSize();
    }
}
