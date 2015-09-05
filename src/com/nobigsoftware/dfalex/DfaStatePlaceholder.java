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

import java.util.List;

/**
 * Base class for placeholders that constructs final-form DFA states and
 * temporarily assumes their place in the DFA.
 * <P>
 * Placeholders are serializable
 */
abstract class DfaStatePlaceholder<MATCH> extends DfaStateImpl<MATCH> implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;

	private static EmptyDfaStateImpl<Object> EMPTY_DELEGATE = new EmptyDfaStateImpl<>();
	
	protected transient DfaStateImpl<MATCH> m_delegate;
	
	/**
	 * Create a new DfaStatePlaceholder
	 * <P>
	 * The initially constructed state will accept no strings
	 */
	@SuppressWarnings("unchecked")
	public DfaStatePlaceholder()
	{
		 m_delegate = (DfaStateImpl<MATCH>)EMPTY_DELEGATE;
	}

	/**
	 * Creates the final form delegate state, implementing all the required
	 * transitions and matches.
	 * <P>
	 * This is called on all DFA state placeholders after they are constructed
	 */
	abstract void createDelegate(List<DfaStatePlaceholder<MATCH>> allStates);
	
	@Override
	final void fixPlaceholderReferences()
	{
		m_delegate.fixPlaceholderReferences();
	}
	
	@Override
	final DfaStateImpl<MATCH> resolvePlaceholder()
	{
		return m_delegate.resolvePlaceholder();
	}
	
	@Override
	final public DfaState<MATCH> getNextState(char c)
	{
		return m_delegate.getNextState(c);
	}
	@Override
	final public MATCH getMatch()
	{
		return m_delegate.getMatch();
	}
}
