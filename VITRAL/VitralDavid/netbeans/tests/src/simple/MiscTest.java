package simple;

import java.util.*;

public class MiscTest
{
    public static void main(String[] args)
    {
        String input="1/1/";
        StringTokenizer st=new StringTokenizer(input, "/");
        System.out.println(st.countTokens());
    }
}
