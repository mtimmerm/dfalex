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

import java.util.Arrays;
import java.util.Collection;

/**
 * A Pattern represents a set of strings.  A string in the set is said to
 * "match" the pattern.
 * <P>
 * Methods are provided here for building up patterns from smaller ones,
 * like regular expressions.
 * <P>
 * The {@link #regex(String)}, {@link #regexI(String)}, {@link #thenRegex(String)},
 * and {@link #thenRegexI(String)} methods can be used to parse regular expression
 * syntax into patterns.  See {@link RegexParser} for syntax information
 * <P>
 * In DFALex, the only requirement for a pattern is that it can add itself to an
 * {@link Nfa} so we can make matchers with it.
 */
public abstract class Pattern implements Matchable
{
    private static final long serialVersionUID = 1L;
    
    public static final Pattern EMPTY = new EmptyPattern();
    /**
     * Pattern that matches one or more decimal digits
     */
    public static final Pattern DIGITS = repeat(CharRange.DIGITS);
    
    /**
     * Pattern that matches one or more hexadecimal digits
     */
    public static final Pattern HEXDIGITS = repeat(CharRange.HEXDIGITS);
    
    /**
     * Pattern that matches an optional sign, followed by one or more decimal digits
     */
    public static final Pattern INTEGER = maybe(anyCharIn("+-")).then(DIGITS);
    
    /**
     * A pattern that matches an {@link #INTEGER}, optionally followed by a '.' and zero or more digits
     */
    public static final Pattern SIMPLE_DECIMAL = INTEGER.thenMaybe(match(".").then(DIGITS));

    /**
     * A decimal number that includes a decimal point and/or scientific exponent, and does NOT match {@link #INTEGER}.
     * It CAN start with a decimal point
     */
    public static final Pattern FLOAT_DECIMAL = anyOf(
        INTEGER.then(".").thenMaybe(DIGITS).thenMaybe(matchI("E").then(INTEGER)),
        match(".").then(DIGITS).thenMaybe(matchI("E").then(INTEGER)),
        INTEGER.then(matchI("E").then(INTEGER))
        );
    /**
     * A {@link #FLOAT_DECIMAL} or an {@link #INTEGER}.
     * Anything this matches is valid for {@link Double#parseDouble(String)}, if the value fits
     */
    public static final Pattern DECIMAL = anyOf(FLOAT_DECIMAL,INTEGER);
    
    /**
     * Typical Java/C/CSS - style block comment
     */
    public static final Pattern BLOCK_COMMENT = match("/*").thenMaybeRepeat(
            //things that don't end in *
            maybeRepeat("*").then(CharRange.notAnyOf("*"))
            ).thenRepeat("*").then("/");
    
    /**
     * Typical Java/C++ - style line comment
     * note that this doesn't include the newline.
     */
    public static final Pattern LINE_COMMENT = match("//").thenMaybeRepeat(CharRange.notAnyOf("\n"));
    
    /**
     * double-quoted string with backslash escapes and no carriage returns or newlines
     */
    public static final Pattern DQ_STRING = match("\"").thenMaybeRepeat(anyOf(
            CharRange.notAnyOf("\"\\\n\r"),
            match("\\").then(CharRange.notAnyOf("\r\n"))
            )).then("\"");
    
    /**
     * single-quoted string with backslash escapes and no carriage returns or newlines
     */
    public static final Pattern SQ_STRING = match("\'").thenMaybeRepeat(anyOf(
            CharRange.notAnyOf("\'\\\n\r"),
            match("\\").then(CharRange.notAnyOf("\r\n"))
            )).then("\'");
    
    /**
     * single or double-quoted string with backslash escapes and no carriage returns or newlines
     */
    public static final Pattern STRING = anyOf(SQ_STRING, DQ_STRING);
    
    /**
     * Create a pattern that exactly matches a single string, case-dependent
     * 
     * @param tomatch string to match
     * @return the pattern that matches the string
     */
    public static Pattern match(String tomatch)
	{
		return new StringPattern(tomatch);
	}
	
    /**
     * Create a pattern that exactly matches a single string, case-independent
     * 
     * @param tomatch string to match
     * @return the pattern that matches the string
     */
    public static Pattern matchI(String tomatch)
    {
        return new StringIPattern(tomatch);
    }
    
    /**
     * Get a Pattern corresponding to a {@link CharRange} or other {@link Matchable}
     * 
     * @param tomatch pattern to match
     * @return a Pattern that matches the given {@link Matchable}
     */
    public static Pattern match(Matchable tomatch)
    {
        if (tomatch instanceof Pattern)
        {
            return (Pattern)tomatch;
        }
        return new WrapPattern(tomatch);
    }
    
    /**
     * Parse the given regular expression into a pattern.
     * <P>
     * See {@link RegexParser} for syntax information
     * 
     * @param regex regular expression string to parse
     * @return a pattern that implements the regular expression
     */
    public static Pattern regex(String regex)
    {
        return match(RegexParser.parse(regex, false));
    }
    
    /**
     * Parse the given regular expression into a pattern, case independent
     * <P>
     * See {@link RegexParser} for syntax information
     * 
     * @param regex regular expression string to parse
     * @return a pattern that implements the regular expression
     */
    public static Pattern regexI(String regex)
    {
        return match(RegexParser.parse(regex, true));
    }
    
	/**
	 * Create a pattern that matches one or more occurrences of a given pattern
	 * 
	 * @param pat  given pattern
	 * @return the new pattern
	 */
	public static Pattern repeat(Matchable pat)
	{
		return new RepeatingPattern(pat, true);
	}

	/**
     * Create a pattern that matches one or more occurrences of a particular string, case dependent
     * 
     * @param str the string to match
     * @return the new pattern
	 */
	public static Pattern repeat(String str)
	{
		return repeat(match(str));
	}

    /**
     * Create a pattern that matches one or more occurrences of a particular string, case independent
     * 
     * @param str the string to match
     * @return the new pattern
     */
    public static Pattern repeatI(String str)
    {
        return repeat(match(str));
    }

    /**
     * Create a pattern that matches a given pattern or the empty string
     * 
     * @param pat  given pattern
     * @return the new pattern
     */
	public static Pattern maybe(Matchable pat)
	{
		return new OptionalPattern(pat);
	}
	
    /**
     * Create a pattern that a particular string, or the empty string, case dependent
     * 
     * @param str the string to match
     * @return the new pattern
     */
	public static Pattern maybe(String str)
	{
		return maybe(match(str));
	}

    /**
     * Create a pattern that a particular string, or the empty string, case independent
     * 
     * @param str the string to match
     * @return the new pattern
     */
    public static Pattern maybeI(String str)
    {
        return maybe(matchI(str));
    }
    
    /**
     * Create a pattern that matches zero or more occurrences of a given pattern
     * 
     * @param pat  given pattern
     * @return the new pattern
     */
	public static Pattern maybeRepeat(Matchable pat)
	{
		return new RepeatingPattern(pat, false);
	}
	
    /**
     * Create a pattern that matches zero or more occurrences of a particular string, case dependent
     * 
     * @param str the string to match
     * @return the new pattern
     */
	public static Pattern maybeRepeat(String str)
	{
		return maybeRepeat(match(str));
	}

    /**
     * Create a pattern that matches zero or more occurrences of a particular string, case dependent
     * 
     * @param str the string to match
     * @return the new pattern
     */
    public static Pattern maybeRepeatI(String str)
    {
        return maybeRepeat(matchI(str));
    }

    /**
     * Create a pattern that matches any of the given patterns
     * 
     * @param patterns patterns to accept
     * @return the new pattern
     */
	public static Pattern anyOf(Matchable...patterns)
	{
		return new UnionPattern(patterns);
	}

    /**
     * Create a pattern that matches any of the given patterns
     * 
     * @param patterns patterns to accept
     * @return the new pattern
     */
    public static Pattern anyOf(Collection<? extends Matchable> patterns)
    {
        return new UnionPattern(patterns.toArray(new Matchable[patterns.size()]));
    }

    /**
     * Create a pattern that matches any of the given strings
     * 
     * @param p0 first possible string
     * @param p1 second possible string
     * @param strings remaining possible strings, if any
     * @return the new pattern
     */
    public static Pattern anyOf(String p0, String p1, String...strings)
    {
        Pattern[] patterns = new Pattern[strings.length+2];
        patterns[0] = match(p0);
        patterns[1] = match(p1);
        for (int i=1; i<strings.length; ++i)
        {
            patterns[i+2] = match(strings[i]);
        }
        return new UnionPattern(patterns);
    }

    /**
     * Create a pattern that matches any of the given strings, case independent
     * 
     * @param p0 first possible string
     * @param p1 second possible string
     * @param strings remaining possible strings, if any
     * @return the new pattern
     */
    public static Pattern anyOfI(String p0, String p1, String...strings)
    {
        Pattern[] patterns = new Pattern[strings.length+2];
        patterns[0] = matchI(p0);
        patterns[1] = matchI(p1);
        for (int i=1; i<strings.length; ++i)
        {
            patterns[i+2] = matchI(strings[i]);
        }
        return new UnionPattern(patterns);
    }

    /**
     * Create a pattern that matches any single character from the given string
     * 
     * @param chars the characters to accept
     * @return the new pattern
     */
    public static Pattern anyCharIn(String chars)
    {
        return match(CharRange.builder().addChars(chars).build());
    }

	/**
	 * Create a pattern that matches strings from this pattern, followed by strings from the given pattern
	 * 
	 * @param tocat    pattern to append to this one
	 * @return the new pattern
	 */
	public Pattern then(Matchable tocat)
	{
		return new CatPattern(this, tocat);
	}
	
    /**
     * Create a pattern that matches strings from this pattern, followed by a given string, case dependent
     * 
     * @param str  string to append to this pattern
     * @return the new pattern
     */
	public Pattern then(String str)
	{
		return then(match(str));
	}

    /**
     * Create a pattern that matches strings from this pattern, followed by a given string, case independent
     * 
     * @param str  string to append to this pattern
     * @return the new pattern
     */
    public Pattern thenI(String str)
    {
        return then(matchI(str));
    }

    /**
     * Create a pattern that matches strings from this pattern,
     * followed by strings that match a regular expression, case dependent
     * 
     * @param regexStr  regular expression to append to this pattern
     * @return the new pattern
     */
    public Pattern thenRegex(String regexStr)
    {
        return then(regex(regexStr));
    }

    /**
     * Create a pattern that matches strings from this pattern,
     * followed by strings that match a regular expression, case independent
     * 
     * @param regexStr  regular expression to append to this pattern
     * @return the new pattern
     */
    public Pattern thenRegexI(String regexStr)
    {
        return then(regexI(regexStr));
    }

    /**
     * Create a pattern that matches strings from this pattern, followed by one
     * or more occurrences of a given pattern
     * 
	 * @param pat the given pattern
	 * @return the new pattern
	 */
	public Pattern thenRepeat(Matchable pat)
	{
		return then(repeat(pat));
	}

    /**
     * Create a pattern that matches strings from this pattern, followed by one
     * or more occurrences of a given string, case dependent
     * 
     * @param str the given given string
     * @return the new pattern
     */
	public Pattern thenRepeat(String str)
	{
		return then(repeat(str));
	}

    /**
     * Create a pattern that matches strings from this pattern, followed by one
     * or more occurrences of a given string, case independent
     * 
     * @param str the given given string
     * @return the new pattern
     */
    public Pattern thenRepeatI(String str)
    {
        return then(repeatI(str));
    }

    /**
     * Create a pattern that matches strings from this pattern, maybe followed by a
     * match of the given pattern
     * 
     * @param pat the given pattern
     * @return the new pattern
     */
	public Pattern thenMaybe(Matchable pat)
	{
		return then(maybe(pat));
	}

    /**
     * Create a pattern that matches strings from this pattern, maybe followed by a
     * match of the given string, case dependent
     * 
     * @param str the given string
     * @return the new pattern
     */
	public Pattern thenMaybe(String str)
	{
		return then(maybe(str));
	}

    /**
     * Create a pattern that matches strings from this pattern, maybe followed by a
     * match of the given string, case independent
     * 
     * @param str the given string
     * @return the new pattern
     */
    public Pattern thenMaybeI(String str)
    {
        return then(maybeI(str));
    }

    /**
     * Create a pattern that matches strings from this pattern, followed by zero
     * or more occurrences of a given pattern
     * 
     * @param pat the given pattern
     * @return the new pattern
     */
	public Pattern thenMaybeRepeat(Matchable pat)
	{
		return then(maybeRepeat(pat));
	}

    /**
     * Create a pattern that matches strings from this pattern, followed by zero
     * or more occurrences of a given string, case dependent
     * 
     * @param str the given given string
     * @return the new pattern
     */
	public Pattern thenMaybeRepeat(String str)
	{
		return then(maybeRepeat(str));
	}

    /**
     * Create a pattern that matches strings from this pattern, followed by zero
     * or more occurrences of a given string, case independent
     * 
     * @param str the given given string
     * @return the new pattern
     */
    public Pattern thenMaybeRepeatI(String str)
    {
        return then(maybeRepeatI(str));
    }
	
	private static class CatPattern extends Pattern
	{
        private static final long serialVersionUID = 1L;
        
        private final Matchable m_first, m_then;
		private final boolean m_matchesEmpty;
		
		CatPattern(Matchable first, Matchable then)
		{
			m_first = first;
			m_then = then;
			m_matchesEmpty = m_first.matchesEmpty() && m_then.matchesEmpty();
		}

		@Override
		public int addToNFA(Nfa<?> nfa, int targetState)
		{
			targetState = m_then.addToNFA(nfa, targetState);
			targetState = m_first.addToNFA(nfa, targetState);
			return targetState;
		}

		@Override
		public boolean matchesEmpty()
		{
			return m_matchesEmpty;
		}
	}
    private static class WrapPattern extends Pattern
    {
        private static final long serialVersionUID = 1L;
        
        private final Matchable m_tomatch;
        
        WrapPattern(Matchable tomatch)
        {
            m_tomatch = tomatch;
        }
        
        @Override
        public int addToNFA(Nfa<?> nfa, int targetState)
        {
            return m_tomatch.addToNFA(nfa, targetState);
        }
        
        @Override
        public boolean matchesEmpty()
        {
            return m_tomatch.matchesEmpty();
        }
    }
    
    private static class EmptyPattern extends Pattern
    {
        private static final long serialVersionUID = 1L;
        
        EmptyPattern()
        {
        }
        
        @Override
        public int addToNFA(Nfa<?> nfa, int targetState)
        {
            return targetState;
        }
        
        @Override
        public boolean matchesEmpty()
        {
            return true;
        }

        @Override
        public Pattern then(Matchable tocat)
        {
            return match(tocat);
        }
        
    }
	private static class StringPattern extends Pattern
	{
        private static final long serialVersionUID = 1L;
        
        private final String m_tomatch;
		
		StringPattern(String tomatch)
		{
			m_tomatch = tomatch;
		}
		
		@Override
		public int addToNFA(Nfa<?> nfa, int targetState)
		{
			for (int i=m_tomatch.length()-1; i>=0 ;--i)
			{
				char c = m_tomatch.charAt(i);
				int newst = nfa.addState(null);
				nfa.addTransition(newst, targetState, c, c);
				targetState = newst;
			}
			return targetState;
		}
		
		@Override
		public boolean matchesEmpty()
		{
			return m_tomatch.length() == 0;
		}
	}
    private static class StringIPattern extends Pattern
    {
        private static final long serialVersionUID = 1L;
        
        private final String m_tomatch;
        
        StringIPattern(String tomatch)
        {
            m_tomatch = tomatch;
        }
        
        @Override
        public int addToNFA(Nfa<?> nfa, int targetState)
        {
            for (int i=m_tomatch.length()-1; i>=0 ;--i)
            {
                char c = m_tomatch.charAt(i);
                int newst = nfa.addState(null);
                nfa.addTransition(newst, targetState, c, c);
                char lc = Character.toLowerCase(c);
                if (lc != c)
                {
                    nfa.addTransition(newst, targetState, lc, lc);
                }
                char uc = Character.toUpperCase(c);
                if (uc != c)
                {
                    nfa.addTransition(newst, targetState, uc, uc);
                }
                targetState = newst;
            }
            return targetState;
        }
        
        @Override
        public boolean matchesEmpty()
        {
            return m_tomatch.length() == 0;
        }
    }
	private static class RepeatingPattern extends Pattern
	{
        private static final long serialVersionUID = 1L;
        
        private final Matchable m_pattern;
		private boolean m_needAtLeastOne;

		RepeatingPattern(Matchable pattern, boolean needAtLeastOne)
		{
			m_pattern = pattern;
			m_needAtLeastOne = needAtLeastOne;
		}
		
		@Override
		public int addToNFA(Nfa<?> nfa, int targetState)
		{
			int repState = nfa.addState(null);
			nfa.addEpsilon(repState, targetState);
			int startState = m_pattern.addToNFA(nfa,  repState);
			nfa.addEpsilon(repState,startState);
			if (m_needAtLeastOne || m_pattern.matchesEmpty())
			{
				return startState;
			}
			int skipState = nfa.addState(null);
			nfa.addEpsilon(skipState, targetState);
			nfa.addEpsilon(skipState, startState);
			return skipState;
		}
		
		@Override
		public boolean matchesEmpty()
		{
			return (!m_needAtLeastOne) || m_pattern.matchesEmpty();
		}
	}
	private static class OptionalPattern extends Pattern
	{
        private static final long serialVersionUID = 1L;
        
        private final Matchable m_pattern;

		OptionalPattern(Matchable pattern)
		{
			m_pattern = pattern;
		}
		
		@Override
		public int addToNFA(Nfa<?> nfa, int targetState)
		{
			int startState = m_pattern.addToNFA(nfa, targetState);
			if (m_pattern.matchesEmpty())
			{
				return startState;
			}
			int skipState = nfa.addState(null);
			nfa.addEpsilon(skipState, targetState);
			nfa.addEpsilon(skipState, startState);
			return skipState;
		}
		
		@Override
		public boolean matchesEmpty()
		{
			return true;
		}
	}
	private static class UnionPattern extends Pattern
	{
        private static final long serialVersionUID = 1L;
        
        private final Matchable[] m_choices;
		private final boolean m_matchesEmpty;
		
		UnionPattern(Matchable[] choices)
		{
			m_choices = Arrays.copyOf(choices, choices.length);
			boolean matchesEmpty = false;
			for (Matchable pat : choices)
			{
				if (pat.matchesEmpty())
				{
					matchesEmpty = true;
					break;
				}
			}
			m_matchesEmpty = matchesEmpty;
		}

		@Override
		public int addToNFA(Nfa<?> nfa, int targetState)
		{
			int startState = nfa.addState(null);
			for (Matchable pat : m_choices)
			{
				nfa.addEpsilon(startState, pat.addToNFA(nfa, targetState));
			}
			return startState;
		}

		@Override
		public boolean matchesEmpty()
		{
			return m_matchesEmpty;
		}
	}
}
