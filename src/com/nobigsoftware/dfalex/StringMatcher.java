/*
 * Copyright 2015 Matthew Timmermans
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nobigsoftware.dfalex;

/**
 * This class implements fast matching in a string using DFAs
 */
public class StringMatcher<MATCHRESULT>
{
    private static final int NMM_SIZE = 128;
    private final String m_src;
    private MATCHRESULT m_currentMatch = null;
    private int m_currentStart = 0;
    private int m_currentEnd = 0;
    
    //non-matching memo
    //For all x >= m_nmmStart, whenever you're in m_nmmState[x] at position m_nmmPositions[x],
    //you will fail to find a match 
    private int m_nmmStart;
    private final int[] m_nmmPositions = new int[NMM_SIZE];
    @SuppressWarnings("unchecked")
    private final DfaState<MATCHRESULT>[] m_nmmStates = (DfaState<MATCHRESULT>[]) new DfaState[NMM_SIZE];
    
    public StringMatcher(String src)
    {
        m_src = src;
    }
    
    public MATCHRESULT getCurrentMatch()
    {
        return m_currentMatch;
    }
    public int getCurrentMatchStart()
    {
        return m_currentStart;
    }

    public int getCurrentMatchEnd()
    {
        return m_currentStart;
    }

    public MATCHRESULT findNext(DfaState<MATCHRESULT> state)
    {
        int pos = m_currentEnd;
        MATCHRESULT ret=matchAt(state, pos);
        while(ret == null && pos < m_src.length())
        {
            ++pos;
            ret=matchAt(state, pos);
        }
        return ret;
    }
    
    public MATCHRESULT matchNext(DfaState<MATCHRESULT> state)
    {
        return matchAt(state, m_currentEnd);
    }
    
    public MATCHRESULT matchAt(DfaState<MATCHRESULT> state, final int startPos)
    {
        m_currentStart = m_currentEnd = startPos;
        m_currentMatch = state.getMatch();
        final int lim = m_src.length();
        int newNmmSize = 0;
        int writeNmmNext = startPos + 2;

        POSLOOP:
        for(int pos = startPos; pos < lim ;)
        {
            state = state.getNextState(m_src.charAt(pos));
            pos++;
            if (state == null)
            {
                break;
            }
            MATCHRESULT match = state.getMatch();
            if (match != null)
            {
                m_currentMatch = match;
                m_currentEnd = pos;
                newNmmSize = 0;
                continue;
            }
            
            //Check and update the non-matching memo, to accelerate processing long sequences
            //of non-accepting states
            //Many DFAs simply don't have long sequences of non-accepting states, so we only
            //want to incur this overhead when we're actually in a non-accepting state
            while (m_nmmStart < NMM_SIZE && m_nmmPositions[m_nmmStart] <= pos)
            {
                if (m_nmmPositions[m_nmmStart] == pos && m_nmmStates[m_nmmStart] == state)
                {
                    //hit the memo -- we won't find a match.  Merge queues
                    break POSLOOP;
                }
                ++m_nmmStart;
            }
            if (pos >= writeNmmNext && newNmmSize < m_nmmStart)
            {
                m_nmmPositions[m_nmmStart] = pos;
                m_nmmStates[m_nmmStart] = state;
                ++newNmmSize;
                writeNmmNext += (writeNmmNext + 4 - startPos)>>1;
            }
        }
        //successful or not, we're done.  Update non-matching memo
        while (m_nmmStart < NMM_SIZE && m_nmmPositions[m_nmmStart] < writeNmmNext)
        {
            ++m_nmmStart;
        }
        while(newNmmSize > 0)
        {
            --newNmmSize;
            --m_nmmStart;
            m_nmmPositions[m_nmmStart] = m_nmmPositions[newNmmSize]; 
            m_nmmStates[m_nmmStart] = m_nmmStates[newNmmSize]; 
        }
        return m_currentMatch;
    }
}
