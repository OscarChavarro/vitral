public class MyNativeInterface 
{
    static {
            System.loadLibrary("myLibraryNative");
    }
    public native int myfunc();
}
