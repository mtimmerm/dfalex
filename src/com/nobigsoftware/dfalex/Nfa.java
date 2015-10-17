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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple non-deterministic finite automaton (NFA) representation
 * <P>
 * A set of {@link Matchable} patterns is converted to an NFA as an
 * intermediate step toward creating the DFA.
 * <P>
 * You can also build an NFA directly with this class and convert it to a DFA
 * with {@link DfaBuilder#buildFromNfa(Nfa, int[], DfaAmbiguityResolver, com.nobigsoftware.util.BuilderCache)}.
 * <P>
 * See <a href="https://en.wikipedia.org/wiki/Nondeterministic_finite_automaton">NFA on Wikipedia</a>
 * 
 * @param MATCHRESULT The type of result produce by matching a pattern.  This must be serializable
 *      to support caching of built DFAs
 */
public class Nfa<MATCHRESULT>
{
	private final ArrayList<List<NfaTransition>> m_stateTransitions = new ArrayList<>();
	private final ArrayList<List<Integer>> m_stateEpsilons = new ArrayList<>();
	private final ArrayList<MATCHRESULT> m_stateAccepts = new ArrayList<>();
	
	public int numStates()
	{
		return m_stateAccepts.size();
	}
	
	public int addState(MATCHRESULT accept)
	{
		int ret = m_stateAccepts.size();
		m_stateAccepts.add(accept);
		m_stateTransitions.add(null);
		m_stateEpsilons.add(null);
		assert(m_stateTransitions.size() == m_stateAccepts.size());
		assert(m_stateEpsilons.size() == m_stateAccepts.size());
		return ret;
	}
	
	public void addTransition(int from, int to, char firstChar, char lastChar)
	{
		List<NfaTransition> list = m_stateTransitions.get(from);
		if (list == null)
		{
			list = new ArrayList<>();
			m_stateTransitions.set(from, list);
		}
		list.add(new NfaTransition(firstChar, lastChar, to));
	}
	
	public void addEpsilon(int from, int to)
	{
		List<Integer> list = m_stateEpsilons.get(from);
		if (list == null)
		{
			list = new ArrayList<Integer>();
			m_stateEpsilons.set(from, list);
		}
		list.add(to);
	}
	
	public MATCHRESULT getAccept(int state)
	{
		return m_stateAccepts.get(state);
	}
	
	public boolean hasTransitionsOrAccepts(int state)
	{
		return (m_stateAccepts.get(state) != null || m_stateTransitions.get(state) != null);
	}
	
	public Iterable<Integer> getStateEpsilons(int state)
	{
        List<Integer> list = m_stateEpsilons.get(state);
        return (list != null ? list : Collections.emptyList());
	}
	
    public Iterable<NfaTransition> getStateTransitions(int state)
    {
        List<NfaTransition> list = m_stateTransitions.get(state);
        return (list != null ? list : Collections.emptyList());
    }
    
	void forStateEpsilons(int state, Consumer<Integer> dest)
	{
		List<Integer> list = m_stateEpsilons.get(state);
		if (list != null)
		{
			list.forEach(dest);
		}
	}
	
	void forStateTransitions(int state, Consumer<NfaTransition> dest)
	{
		List<NfaTransition> list = m_stateTransitions.get(state);
		if (list != null)
		{
			list.forEach(dest);
		}
	}
}
