package com.nobigsoftware.dfalex;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

public class StringSearcherTest extends TestBase
{
    @Test
    public void test() throws Exception
    {
        DfaBuilder<JavaToken> builder = new DfaBuilder<>();
        for (JavaToken tok : JavaToken.values())
        {
            builder.addPattern(tok.m_pattern, tok);
        }
        StringSearcher<JavaToken> searcher = builder.buildStringSearcher(null);
        String instr = _readResource("SearcherTestInput.txt");
        String want = _readResource("SearcherTestOutput.txt");
        String have = searcher.findAndReplace(instr, StringSearcherTest::tokenReplace);
        Assert.assertEquals(want, have);
    }

    @Test
    public void testReplaceFunc() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        
        for (JavaToken tok : JavaToken.values())
        {
            final JavaToken t = tok;
            builder.addPattern(tok.m_pattern, (dest, src, s, e) -> tokenReplace(dest, t, src, s, e));
        }
        Function<String, String> replacer = builder.build();
        String instr = _readResource("SearcherTestInput.txt");
        String want = _readResource("SearcherTestOutput.txt");
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }

    @Test
    public void repositionTest() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        builder.addPattern(Pattern.regexI("[a-z0-9]+ +[a-z0-9]+"), (dest, src, s, e) -> {
            for (e=s;src.charAt(e)!=' ';++e);
            dest.append(src, s, e).append(", ");
            for (;src.charAt(e)==' ';++e);
            return e;
        });
        Function<String, String> replacer = builder.build();
        
        String instr = " one two  three   four five ";
        String want = " one, two, three, four, five ";
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }
    
    
    static int tokenReplace(SafeAppendable dest, JavaToken mr, String src, int startPos, int endPos)
    {
        dest.append("[").append(mr.name()).append("=").append(src, startPos, endPos).append("]");
        return 0;
    }

}
