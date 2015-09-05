package com.nobigsoftware.dfalex;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.junit.Assert;

import com.nobigsoftware.dfalex.DfaState;

public class TestBase
{
    final PrettyPrinter m_printer = new PrettyPrinter();

    void checkDfa(DfaState<?> start, String resource, boolean doStdout) throws Exception
    {
        String have;
        {
            StringWriter w = new StringWriter();
            m_printer.print(new PrintWriter(w), start);
            have = w.toString();
        }
        if (doStdout)
        {
            System.out.print(have);
            System.out.flush();
        }
        String want = readResource(resource);
        Assert.assertEquals(want, have);
    }
    
    String readResource(String resource) throws Exception
    {
        String pkg = getClass().getPackage().getName().replace('.', '/');
        InputStream instream = getClass().getClassLoader().getResourceAsStream(pkg+"/"+resource);
        try
        {
            InputStreamReader inreader = new InputStreamReader(instream, Charset.forName("UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            for(;;)
            {
                int rlen = inreader.read(buf);
                if (rlen <= 0)
                {
                    break;
                }
                sb.append(buf, 0, rlen);
            }
            return sb.toString();
        }
        finally
        {
            instream.close();
        }
    }
}
