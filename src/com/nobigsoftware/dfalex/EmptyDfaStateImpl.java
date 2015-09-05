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
 * Implementation of an empty DFA state that accepts no strings
 */
class EmptyDfaStateImpl<MATCH> extends DfaStateImpl<MATCH>
{
	@Override
	void fixPlaceholderReferences()
	{
	}

	@Override
	DfaStateImpl<MATCH> resolvePlaceholder()
	{
		return this;
	}

	@Override
	public DfaState<MATCH> getNextState(char c)
	{
		return null;
	}

	@Override
	public MATCH getMatch()
	{
		return null;
	}
}
