package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.numerics.FloatingInterval;

/**
Exact comparisons of finite binary64 expressions using dyadic integer limbs.
Unsupported arithmetic environments or out-of-range intermediates return false.
The cpp `unsigned long` limbs are Java `long` values read as unsigned:
every limb holds 32 bits, so only the right shifts need the unsigned form.
*/
public class CsgExactArithmetic {
    /**
    The cpp `java::Math::getExponent` is `std::ilogb`, which gives the true
    binary exponent of a subnormal value (-1074 for Double.MIN_VALUE), while
    `java.lang.Math.getExponent` gives Double.MIN_EXPONENT-1 for every
    subnormal. The exact arithmetic depends on the cpp result, so it is
    reproduced here for finite nonzero values.
    */
    static int cppGetExponent(double magnitude) {
        final int exponent=Math.getExponent(magnitude);
        if (exponent>=Double.MIN_EXPONENT) return exponent;
        return Math.getExponent(magnitude*0x1p54)-54;
    }

    /** `significand` and `exponent` are one-element output holders. */
    private static boolean decompose(double value, long[] significand, int[] exponent) {
        if (!Double.isFinite(value)) return false;
        if (value==0) { significand[0]=0; exponent[0]=0; return true; }
        final double magnitude=Math.abs(value);
        exponent[0]=cppGetExponent(magnitude);
        final double normalized=Math.scalb(magnitude,-exponent[0]);
        final double integer=Math.scalb(normalized,52);
        if (!Double.isFinite(integer) || integer<4503599627370496.0 || integer>=9007199254740992.0)
            return false;
        significand[0]=(long)integer;
        exponent[0]-=52;
        while ((significand[0]&1L)==0) { significand[0]>>>=1; ++exponent[0]; }
        return true;
    }

    private static boolean addWord(long[] target, int index, long word) {
        final long mask=0xffffffffL;
        while (word!=0) {
            if (index<0 || index>=140) return false;
            final long sum=target[index]+word;
            target[index]=sum&mask;
            word=sum>>>32;
            ++index;
        }
        return true;
    }

    private static boolean addProduct(long[] positive, long[] negative,
        double a, double b, boolean negate) {
        long[] x={0},y={0};
        int[] ex={0},ey={0};
        if (!decompose(a,x,ex) || !decompose(b,y,ey)) return false;
        if (x[0]==0 || y[0]==0) return true;
        final long mask=0xffffffffL;
        final long[] xl={x[0]&mask,x[0]>>>32}, yl={y[0]&mask,y[0]>>>32};
        long[] product={0,0,0,0};
        for (int i=0;i<2;++i) {
            long carry=0;
            for (int j=0;j<2;++j) {
                final long value=xl[i]*yl[j]+product[i+j]+carry;
                product[i+j]=value&mask;
                carry=value>>>32;
            }
            product[i+2]=carry;
        }
        final int offset=ex[0]+ey[0]+2148;
        if (offset<0) return false;
        final int wordOffset=offset/32, shift=offset%32;
        long[] target=((a<0)!=(b<0))!=negate ? negative : positive;
        for (int i=0;i<4;++i) {
            final long shifted=product[i]<<shift;
            if (!addWord(target,wordOffset+i,shifted&mask) ||
                !addWord(target,wordOffset+i+1,shifted>>>32)) return false;
        }
        return true;
    }

    public static boolean equalSumProducts(double[] a, double[] b,
        double[] c, double[] d, int count) {
        if (a == null || b == null || c == null || d == null || count<=0 || !FloatingInterval.isSupportedEnvironment()) return false;
        long[] positive=new long[140], negative=new long[140];
        for (int i=0;i<count;++i)
            if (!addProduct(positive,negative,a[i],b[i],false) ||
                !addProduct(positive,negative,c[i],d[i],true)) return false;
        for (int i=0;i<140;++i) if (positive[i]!=negative[i]) return false;
        return true;
    }

    public static boolean equalProducts(double a, double b, double c, double d) {
        if (!FloatingInterval.isSupportedEnvironment()) return false;
        long[] positive=new long[140], negative=new long[140];
        if (!addProduct(positive,negative,a,b,false) || !addProduct(positive,negative,c,d,true)) return false;
        for (int i=0;i<140;++i) if (positive[i]!=negative[i]) return false;
        return true;
    }

    public static boolean equalAffine(double a, double b, double c, double d, double e, double f) {
        if (!FloatingInterval.isSupportedEnvironment()) return false;
        long[] positive=new long[140], negative=new long[140];
        if (!addProduct(positive,negative,a,b,false) || !addProduct(positive,negative,c,1,false) ||
            !addProduct(positive,negative,d,e,true) || !addProduct(positive,negative,f,1,true)) return false;
        for (int i=0;i<140;++i) if (positive[i]!=negative[i]) return false;
        return true;
    }
}
