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

class Transition
{
	public final char m_firstChar;
	public final char m_lastChar;
	public final int m_stateNum;
	
	/**
	 * Create a new Transition.
	 * @param firstChar
	 * @param lastChar
	 * @param stateNum
	 */
	public Transition(char firstChar, char lastChar, int stateNum)
	{
		super();
		m_firstChar = firstChar;
		m_lastChar = lastChar;
		m_stateNum = stateNum;
	}
}