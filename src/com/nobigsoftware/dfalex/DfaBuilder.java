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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.nobigsoftware.util.BuilderCache;
import com.nobigsoftware.util.SHAOutputStream;

/**
 * Builds deterministic finite automata (google phrase) or DFAs that find patterns in strings
 * <P>
 * Given a set of patterns and the desired result of matching each pattern, you can produce a
 * DFA that will simultaneously match a sequence of characters against all of those patterns.
 * <P>
 * You can also build DFAs for multiple sets of patterns simultaneously. The resulting DFAs will
 * be optimized to shared states wherever possible.
 * <P>
 * When you build a DFA to match a set of patterns, you get a "start state" (a {@link DfaState}) for
 * that pattern set. Each character of a string can be passed in turn to {@link DfaState#getNextState(char)},
 * which will return a new {@link DfaState}.
 * <P>
 * {@link DfaState#getMatch()} can be called at any time to get the MATCHRESULT (if any) for
 * the patterns that match the characters processed so far.
 * <P>
 * A {@link DfaState} can be used with a {@link StringMatcher} to find instances of patterns in strings,
 * or with other pattern-matching classes.
 * <P>
 * NOTE that building a Dfa is a complex procedure.  You should typically do it only once for each
 * pattern set you want to use.  Usually you would do this in a static initializer.
 * <P>
 * You can provide a cache that can remember and recall built DFAs, which allows you to build DFAs
 * during your build process in various ways, instead of building them at runtime.  Or you can use
 * the cache to store built DFAs on the first run of your program so they don't need to be built
 * the next time.
 * 
 * @param MATCHRESULT The type of result to produce by matching a pattern.  This must be serializable
 *      to support caching of built DFAs
 */
public class DfaBuilder<MATCHRESULT extends Serializable>
{
    private final BuilderCache m_cache;
	private final Map<MATCHRESULT, List<Pattern>> m_patterns = new LinkedHashMap<>();
	
	/**
	 * Create a new DfaBuilder without a {@link BuilderCache}
	 */
	public DfaBuilder()
	{
	    m_cache = null;
	}
	
	/**
	 * Create a new DfaBuilder, with a builder cache to bypass recalculation of pre-build DFAs
	 * 
	 * @param cache    The BuilderCache to use
	 */
	public DfaBuilder(BuilderCache cache)
	{
	    m_cache = cache;
	}
	
	/**
	 * Reset this DFA builder by forgetting all the patterns that have been added
	 */
	public void clear()
	{
	    m_patterns.clear();
	}
	
	public void addPattern(Pattern pat, MATCHRESULT accept)
	{
		List<Pattern> patlist = m_patterns.computeIfAbsent(accept, x -> new ArrayList<>());
		patlist.add(pat);
	}
	
	
    /**
     * Build DFA for a single language
     * <P>
     * The language is specified as a subset of available MATCHRESULTs, and will include patterns
     * for each result in its set.
     * 
     * @param language     set defining the languages to build
     * @param ambiguityResolver     When patterns for multiple results match the same string, this is called to
     *                              combine the multiple results into one.  If this is null, then a DfaAmbiguityException
     *                              will be thrown in that case.
     */
    public DfaState<MATCHRESULT> build(Set<MATCHRESULT> language, DfaAmbiguityResolver<MATCHRESULT> ambiguityResolver)
    {
        return build(Collections.singletonList(language), ambiguityResolver).get(0);
    }

    /**
	 * Build DFAs for multiple languages simultaneously.
	 * <P>
	 * Each language is specified as a subset of available MATCHRESULTs, and will include patterns
	 * for each result in its set.
	 * <P>
	 * Languages built simultaneously will be globally minimized and will share as many states as possible.
	 * 
	 * @param languages 	sets defining the languages to build
	 * @param ambiguityResolver	 	When patterns for multiple results match the same string, this is called to
	 * 								combine the multiple results into one.	If this is null, then a DfaAmbiguityException
	 * 								will be thrown in that case.
	 */
    @SuppressWarnings("unchecked")
    public List<DfaState<MATCHRESULT>> build(List<Set<MATCHRESULT>> languages, DfaAmbiguityResolver<MATCHRESULT> ambiguityResolver)
    {
        if (languages.isEmpty())
        {
            return Collections.emptyList();
        }
        
        SerializableDfa<MATCHRESULT> serializableDfa = null;
        if (m_cache == null)
        {
            serializableDfa = _build(languages, ambiguityResolver);
        }
        else
        {
            String cacheKey;
            try
            {
                //generate the cache key by serializing key info into an SHA hash
                SHAOutputStream sha = new SHAOutputStream();
                sha.on(false);
                ObjectOutputStream os = new ObjectOutputStream(sha);
                os.flush();
                sha.on(true);
                final int numLangs = languages.size();
                os.writeInt(numLangs);
                
                //write key stuff out in an order based on our LinkedHashMap, for deterministic serialization
                for (Entry<MATCHRESULT, List<Pattern>> patEntry : m_patterns.entrySet())
                {
                    boolean included = false;
                    List<Pattern> patList = patEntry.getValue();
                    if (patList.isEmpty())
                    {
                        continue;
                    }
                    for (int i=0; i<numLangs; ++i)
                    {
                        if (!languages.get(i).contains(patEntry.getKey()))
                        {
                            continue;
                        }
                        included = true;
                        break;
                    }
                    if (!included)
                    {
                        continue;
                    }
                    os.writeInt(patList.size());
                    if (numLangs>1)
                    {
                        int bits=languages.get(0).contains(patEntry.getKey()) ? 1:0;
                        for (int i=1; i<languages.size(); ++i)
                        {
                            if ((i&31)==0)
                            {
                                os.writeInt(bits);
                                bits=0;
                            }
                            if (languages.get(i).contains(patEntry.getKey()))
                            {
                                bits |= 1<<(i&31);
                            }
                        }
                        os.writeInt(bits);
                    }
                    for (Pattern pat : patList)
                    {
                        os.writeObject(pat);
                    }
                    os.writeObject(patEntry.getKey());
                }
                os.writeInt(0); //0-size pattern list terminates pattern map
                os.writeObject(ambiguityResolver);
                os.flush();
                
                cacheKey = sha.getBase32Digest();
                os.close();
            }
            catch(IOException e)
            {
                //doesn't really happen
                throw new RuntimeException(e);
            }
            serializableDfa = (SerializableDfa<MATCHRESULT>) m_cache.getCachedItem(cacheKey);
            if (serializableDfa == null)
            {
                serializableDfa = _build(languages, ambiguityResolver);
                m_cache.maybeCacheItem(cacheKey, serializableDfa);
            }
        }
        return serializableDfa.getStartStates();
    }
    
    
	public SerializableDfa<MATCHRESULT> _build(List<Set<MATCHRESULT>> languages, DfaAmbiguityResolver<MATCHRESULT> ambiguityResolver)
	{
		Nfa<MATCHRESULT> nfa = new Nfa<>();
		
		int[] nfaStartStates = new int[languages.size()];
		for (int i=0; i<languages.size(); ++i)
		{
			nfaStartStates[i] = nfa.addState(null);
		}
		
		if (ambiguityResolver == null)
		{
			ambiguityResolver = DfaBuilder::defaultAmbiguityResolver;
		}
		
		for (Entry<MATCHRESULT, List<Pattern>> patEntry : m_patterns.entrySet())
		{
			List<Pattern> patList = patEntry.getValue();
			if (patList == null || patList.size()<1)
			{
				continue;
			}
			int matchState = -1; //start state for matching this token
			for (int i=0; i<languages.size(); ++i)
			{
				if (!languages.get(i).contains(patEntry.getKey()))
				{
					continue;
				}
				if (matchState<0)
				{
					int acceptState = nfa.addState(patEntry.getKey()); //final state accepting this token
					if (patList.size()>1)
					{
					    //we have multiple patterns.  Make a union
						matchState = nfa.addState(null);
						for (Pattern pat : patList)
						{
							nfa.addEpsilon(matchState, pat.addToNFA(nfa, acceptState));
						}
					}
					else
					{
					    //only one pattern no union necessary
						matchState = patList.get(0).addToNFA(nfa, acceptState);
					}
				}
				//language i matches these patterns
				nfa.addEpsilon(nfaStartStates[i],matchState);
			}
		}
		
		SerializableDfa<MATCHRESULT> serializableDfa;
		{
			RawDfa<MATCHRESULT> minimalDfa;
			{
				RawDfa<MATCHRESULT> rawDfa = (new DfaFromNfa<MATCHRESULT>(nfa, nfaStartStates, ambiguityResolver)).getDfa();
				minimalDfa = (new DfaMinimizer<MATCHRESULT>(rawDfa)).getMinimizedDfa();
			}
			serializableDfa = new SerializableDfa<>(minimalDfa);
		}
		return serializableDfa;
	}
	
	
	private static <T> T defaultAmbiguityResolver(Set<T> matches)
	{
		T ret = null;
		for (T match : matches)
		{
			if (ret != null)
			{
				throw new DfaAmbiguityException(matches);
			}
			ret = match;
		}
		return ret;
	}
}
