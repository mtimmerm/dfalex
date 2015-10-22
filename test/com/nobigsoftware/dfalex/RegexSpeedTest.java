package com.nobigsoftware.dfalex;

import java.util.function.Function;

import org.junit.Test;

public class RegexSpeedTest extends TestBase
{
    private static final int SPINUP=1000;
    @Test
    public void notFoundReplaceTest() throws Exception
    {
        String patString = ("01235|/|456*1|abc|_|\\..*|013|0?1?2?3?4?57");
        String src;
        {
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<10000;i++)
            {
                sb.append("0123456789");
            }
            src = sb.toString();
        }
        java.util.regex.Pattern javapat = java.util.regex.Pattern.compile(patString);
        Function<String,String> replacer;
        {
            SearchAndReplaceBuilder builder=new SearchAndReplaceBuilder();
            builder.addPattern(Pattern.regex(patString), (dest, srcStr, s, e) -> 0);
            replacer = builder.build();
        }
        int javaCount = 0, builderCount = 0;
        
        long start = System.currentTimeMillis();
        String str = src; 
        for (long t = System.currentTimeMillis()-start;t < SPINUP+1000; t=System.currentTimeMillis()-start)
        {
            str = javapat.matcher(str).replaceAll("");
            if (t>=SPINUP)
            {
                ++javaCount;
            }
        }
        start = System.currentTimeMillis();
        str = src; 
        for (long t = System.currentTimeMillis()-start;t < SPINUP+1000; t=System.currentTimeMillis()-start)
        {
            str = replacer.apply(str);
            if (t>=SPINUP)
            {
                ++builderCount;
            }
        }
        
        System.out.println("Search+Replace per second in 100K string, patterns not found:");
        System.out.format("Java Regex: %d    SearchAndReplaceBuilder: %d\n", javaCount, builderCount);
    }
}
