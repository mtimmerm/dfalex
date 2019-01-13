package com.nobigsoftware.dfalex;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class DestinyTest extends TestBase
{
    @Test
    public void test() throws Exception
    {
        DfaBuilder<JavaToken> builder = new DfaBuilder<>(null);
        for (JavaToken tok : JavaToken.values())
        {
            builder.addPattern(tok.m_pattern, tok);
        }
        EnumSet<JavaToken> lang = EnumSet.allOf(JavaToken.class);
        DfaState<JavaToken> start = builder.build(lang, null);
        DfaAuxiliaryInformation<JavaToken> auxInfo = new DfaAuxiliaryInformation<>(Collections.singleton(start));

        //calculate destinies the slow way
        List<DfaState<JavaToken>> states = auxInfo.getStatesByNumber();
        List<Set<JavaToken>> slowDestinies = new ArrayList<>(states.size());
        final int numStates = states.size();
        for (int i=0; i<numStates; i++)
        {
            slowDestinies.add(EnumSet.noneOf(JavaToken.class));
            final DfaState<JavaToken> state = states.get(i);
            if (state.getMatch() != null)
            {
                slowDestinies.get(i).add(state.getMatch());
            }
        }
        final AtomicBoolean again = new AtomicBoolean(true);
        while(again.get())
        {
            again.set(false);
            for (int i=0; i<numStates; ++i)
            {
                Set<JavaToken> set = slowDestinies.get(i);
                final DfaState<JavaToken> state = states.get(i);
                state.enumerateTransitions((f,l,target) -> {
                    Set<JavaToken> targetSet = slowDestinies.get(target.getStateNumber());
                    if (set.addAll(targetSet))
                    {
                        again.set(true);
                    }
                });
            }
        }

        /*
            PrettyPrinter p = new PrettyPrinter(true);
            PrintWriter pw = new PrintWriter(System.out);
            p.print(pw, start);
            pw.flush();
        */
        List<JavaToken> destinies = auxInfo.getDestinies();
        for (int i=0; i<numStates; ++i)
        {
            Set<JavaToken> set = slowDestinies.get(i);
            JavaToken wantDestiny = null;
            if (set.size() == 1)
            {
                wantDestiny = set.stream().findFirst().orElse(null);
            }
            Assert.assertEquals("State " + i + " destiny", wantDestiny, destinies.get(i));
        }
    }
}
