public class program
{
    public static void main(String args[])
    {
        int val;
        MyNativeInterface nativeinterface;
        nativeinterface = new MyNativeInterface();

        val = nativeinterface.myfunc();
        System.out.println("Hello again cruel native-virtual world with value " + val + "!");
    }
}
