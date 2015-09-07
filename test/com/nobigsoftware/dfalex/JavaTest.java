package com.nobigsoftware.dfalex;

import java.util.EnumSet;

import org.junit.Test;

public class JavaTest extends TestBase
{
    final PrettyPrinter m_printer = new PrettyPrinter();
    @Test
    public void test() throws Exception
    {
        DfaBuilder<JavaToken> builder = new DfaBuilder<>();
        for (JavaToken tok : JavaToken.values())
        {
            builder.addPattern(tok.m_pattern, tok);
        }
        EnumSet<JavaToken> lang = EnumSet.allOf(JavaToken.class);
        DfaState<?> start = builder.build(lang, null);
        _checkDfa(start, "JavaTest.out.txt", false);
    }
}
