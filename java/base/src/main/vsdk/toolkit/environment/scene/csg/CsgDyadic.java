package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.numerics.FloatingInterval;

/**
Signed exact dyadic number. Fixed-capacity binary limbs avoid heap allocation;
every operation reports failure and leaves its output unchanged on overflow.
The cpp `unsigned int` limbs are Java `int` values read as unsigned through
`toUnsigned`.
*/
public class CsgDyadic {
    private static final int CAPACITY=1100;
    private static final int INT_MAX=Integer.MAX_VALUE;
    private static final int INT_MIN=Integer.MIN_VALUE;
    private final int[] limbs;
    private int length;
    private int exponent;
    private int sign;

    private static long toUnsigned(int limb) { return limb & 0xffffffffL; }

    private boolean normalize() {
        while (length>0 && limbs[length-1]==0) --length;
        if (length == 0) { exponent=0; sign=0; return true; }
        int words=0; while (words<length && limbs[words]==0) ++words;
        if (words != 0) {
            for (int i=words;i<length;++i) limbs[i-words]=limbs[i];
            for (int i=length-words;i<length;++i) limbs[i]=0;
            length-=words;
            final long shiftedExponent=(long)exponent+32L*words;
            if (shiftedExponent>INT_MAX) return false;
            exponent=(int)shiftedExponent;
        }
        while ((limbs[0]&1)==0) {
            int carry=0;
            for (int i=length-1;i>=0;--i) {
                final int next=limbs[i]&1;
                limbs[i]=(limbs[i]>>>1)|(carry<<31); carry=next;
            }
            if (exponent==INT_MAX) return false;
            ++exponent;
            while (length>0 && limbs[length-1]==0) --length;
        }
        return true;
    }

    /** `result` is an output array of at least CAPACITY limbs. */
    private static boolean shifted(CsgDyadic value, int targetExponent, int[] result) {
        final long difference=(long)value.exponent-targetExponent;
        if (difference<0) return false;
        final long words=difference/32; final int bits=(int)(difference%32);
        if (value.length != 0 && (words>=CAPACITY || words+value.length+(bits != 0?1:0)>CAPACITY)) return false;
        for (int i=0;i<CAPACITY;++i) result[i]=0;
        for (int i=0;i<value.length;++i) {
            final long part=toUnsigned(value.limbs[i])<<bits;
            result[(int)words+i] |= (int)(part&0xffffffffL);
            if (bits != 0) result[(int)words+i+1] |= (int)(part>>>32);
        }
        return true;
    }

    public CsgDyadic() {
        limbs=new int[CAPACITY];
        length=0; exponent=0; sign=0;
    }

    public CsgDyadic(CsgDyadic other) {
        limbs=new int[CAPACITY];
        assign(other);
    }

    /** cpp copy assignment. */
    public CsgDyadic assign(CsgDyadic other) {
        if (this != other) {
            System.arraycopy(other.limbs, 0, limbs, 0, CAPACITY);
            length=other.length; exponent=other.exponent; sign=other.sign;
        }
        return this;
    }

    public static boolean fromDouble(double value, CsgDyadic result) {
        if (!FloatingInterval.isSupportedEnvironment() ||
            !Double.isFinite(value)) return false;
        CsgDyadic candidate = new CsgDyadic();
        if (value!=0) {
            final double absolute=Math.abs(value);
            int exp=CsgExactArithmetic.cppGetExponent(absolute);
            final double significand=Math.scalb(Math.scalb(absolute,-exp),52);
            if (!Double.isFinite(significand) || significand<4503599627370496.0 || significand>=9007199254740992.0) return false;
            final long bits=(long)significand;
            candidate.limbs[0]=(int)(bits&0xffffffffL);
            candidate.limbs[1]=(int)(bits>>>32);
            candidate.length=2; candidate.exponent=exp-52; candidate.sign=value<0 ? -1 : 1;
            if (!candidate.normalize()) return false;
        }
        result.assign(candidate); return true;
    }

    public static boolean add(CsgDyadic a, CsgDyadic b, CsgDyadic result) {
        if (a.sign == 0) { result.assign(b); return true; }
        if (b.sign == 0) { result.assign(a); return true; }
        final int common=a.exponent<b.exponent ? a.exponent : b.exponent;
        int[] x=new int[CAPACITY],y=new int[CAPACITY];
        if (!shifted(a,common,x) || !shifted(b,common,y)) return false;
        CsgDyadic candidate = new CsgDyadic(); candidate.exponent=common;
        if (a.sign==b.sign) {
            long carry=0;
            for (int i=0;i<CAPACITY;++i) {
                final long sum=toUnsigned(x[i])+toUnsigned(y[i])+carry;
                candidate.limbs[i]=(int)(sum&0xffffffffL); carry=sum>>>32;
            }
            if (carry != 0) return false;
            candidate.sign=a.sign;
        } else {
            int comparison=0;
            for (int i=CAPACITY-1;i>=0;--i) if (x[i]!=y[i]) { comparison=Integer.compareUnsigned(x[i],y[i])>0 ? 1 : -1; break; }
            if (comparison == 0) { result.assign(candidate); return true; }
            final int[] large=comparison>0?x:y; final int[] small=comparison>0?y:x;
            long borrow=0;
            for (int i=0;i<CAPACITY;++i) {
                final long lhs=toUnsigned(large[i]), rhs=toUnsigned(small[i])+borrow;
                candidate.limbs[i]=(int)((lhs-rhs)&0xffffffffL); borrow=lhs<rhs ? 1 : 0;
            }
            candidate.sign=comparison>0 ? a.sign : b.sign;
        }
        candidate.length=CAPACITY; if (!candidate.normalize()) return false; result.assign(candidate); return true;
    }

    public static boolean subtract(CsgDyadic a, CsgDyadic b, CsgDyadic result)
    { CsgDyadic opposite=new CsgDyadic(b); opposite.negate(); return add(a,opposite,result); }

    public static boolean multiply(CsgDyadic a, CsgDyadic b, CsgDyadic result) {
        CsgDyadic candidate = new CsgDyadic();
        if (a.sign == 0 || b.sign == 0) { result.assign(candidate); return true; }
        if (a.length+b.length-1>CAPACITY) return false;
        for (int i=0;i<a.length;++i) {
            long carry=0;
            for (int j=0;j<b.length;++j) {
                final int k=i+j;
                if (k>=CAPACITY) return false;
                // Unsigned 64-bit: the product of two 32-bit limbs plus two
                // 32-bit terms never exceeds 2^64 - 1.
                final long value=toUnsigned(a.limbs[i])*toUnsigned(b.limbs[j])+toUnsigned(candidate.limbs[k])+carry;
                candidate.limbs[k]=(int)(value&0xffffffffL); carry=value>>>32;
            }
            int k=i+b.length;
            while (carry != 0) {
                if (k>=CAPACITY) return false;
                final long value=toUnsigned(candidate.limbs[k])+carry;
                candidate.limbs[k]=(int)(value&0xffffffffL); carry=value>>>32; ++k;
            }
        }
        final long exp=(long)a.exponent+b.exponent;
        if (exp<INT_MIN || exp>INT_MAX) return false;
        candidate.exponent=(int)exp; candidate.sign=a.sign*b.sign;
        candidate.length=a.length+b.length; if (candidate.length>CAPACITY) candidate.length=CAPACITY;
        if (!candidate.normalize()) return false; result.assign(candidate); return true;
    }

    public int getSign() { return sign; }

    public boolean equals(CsgDyadic other) {
        if (sign!=other.sign || exponent!=other.exponent || length!=other.length) return false;
        for (int i=0;i<length;++i) if (limbs[i]!=other.limbs[i]) return false;
        return true;
    }

    public void negate() { sign=-sign; }

    /** Array of `count` zero-valued numbers (cpp array of default-constructed values). */
    public static CsgDyadic[] newArray(int count) {
        CsgDyadic[] result = new CsgDyadic[count];
        for (int i = 0; i < count; i++) result[i] = new CsgDyadic();
        return result;
    }
}
