package com.nobigsoftware.dfalex;

import java.io.PrintWriter;
import java.util.EnumSet;

import org.junit.Test;

import com.nobigsoftware.dfalex.DfaBuilder;
import com.nobigsoftware.dfalex.DfaState;

public class JavaTest
{
    final PrettyPrinter m_printer = new PrettyPrinter();
    @Test
    public void test()
    {
        DfaBuilder<JavaToken> builder = new DfaBuilder<>();
        for (JavaToken tok : JavaToken.values())
        {
            builder.addPattern(tok.m_pattern, tok);
        }
        EnumSet<JavaToken> lang = EnumSet.allOf(JavaToken.class);
        DfaState<?> start = builder.build(lang, null);
        PrintWriter w = new PrintWriter(System.out);
        m_printer.print(w, start);
        w.flush();
    }
    
}
