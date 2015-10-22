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
import java.util.Set;
import java.util.function.Function;

import com.nobigsoftware.util.BuilderCache;

/**
 * Builds search and replace functions that finds patterns in strings and replaces them
 * <P>
 * Given a set of patterns and associated {@link StringReplacement} functions, you can produce an
 * optimized, thread-safe Function<String,String> that will find all occurrences of those patterns and replace
 * them with their replacements.
 * <P>
 * The returned function is thread-safe.
 * <P>
 * NOTE that building a search and replace function is a relatively complex procedure.  You should typically do it only once for each
 * pattern set you want to use.  Usually you would do this in a static initializer.
 * <P>
 * You can provide a cache that can remember and recall built functions, which allows you to build
 * them during your build process in various ways, instead of building them at runtime.  Or you can use
 * the cache to store built functions on the first run of your program so they don't need to be built
 * the next time...  But this is usually unnecessary, since building them is more than fast enough to
 * do during runtime initialization.
 */
public class SearchAndReplaceBuilder
{
    private final DfaBuilder<Integer> m_dfaBuilder;
    private final ArrayList<StringReplacement> m_replacements = new ArrayList<>();

    /**
     * Create a new SearchAndReplaceBuilder without a {@link BuilderCache}
     */
    public SearchAndReplaceBuilder()
    {
        m_dfaBuilder = new DfaBuilder<>();
    }
    
    /**
     * Create a new SearchAndReplaceBuilder, with a builder cache to bypass recalculation of pre-built functions
     * 
     * @param cache    The BuilderCache to use
     */
    public SearchAndReplaceBuilder(BuilderCache cache)
    {
        m_dfaBuilder = new DfaBuilder<>(cache);
    }
    
    /**
     * Reset this builder by forgetting all the patterns that have been added
     */
    public void clear()
    {
        m_dfaBuilder.clear();
        m_replacements.clear();
    }
    
    
    /**
     * Add a search and replace pattern
     * @param pat   The pattern to search for
     * @param replacement   A function to generate the replacement value
     */
    public void addPattern(Matchable pat, StringReplacement replacement)
    {
        Integer result = m_replacements.size();
        m_replacements.add(replacement);
        m_dfaBuilder.addPattern(pat, result);
    }
    
    /**
     * Build a search and replace function
     * <P>
     * The resulting function finds all patterns in the string you give it, and replaces them all with
     * the associated {@StringReplacement}.
     * <P>
     * Matches are found in order.  If matches to more than one pattern occur at the same position,
     * then the <i>longest</i> match will be used.  If there is a tie, then the first one added to this
     * builder will be used.
     *
     * @return The search+replace function
     */
    public Function<String,String> build()
    {
        final StringSearcher<Integer> searcher = m_dfaBuilder.buildStringSearcher(SearchAndReplaceBuilder::ambiguityResolver);
        final StringReplacement[] funcs = m_replacements.toArray(new StringReplacement[m_replacements.size()]);
        final StringSearcher.ReplaceFunc<Integer> replacer = (dest, mr, src, startPos, endPos) ->
            funcs[mr].apply(dest, src, startPos, endPos);
        return (str -> searcher.findAndReplace(str, replacer));
    }
    
    /**
     * Build a search and replace function from a searcher and replacer
     * 
     * @param searcher the searcher
     * @param replacer the replacer
     * @return The search+replace function
     */
    public static <MR> Function<String,String> buildFromSearcher(StringSearcher<MR> searcher, StringSearcher.ReplaceFunc<? super MR> replacer)
    {
        return (str -> searcher.findAndReplace(str, replacer));
    }
    
    private static Integer ambiguityResolver(Set<? extends Integer> candidates)
    {
        Integer ret = null;
        for (Integer c : candidates)
        {
            if (ret == null || c < ret)
            {
                ret = c;
            }
        }
        return ret;
    }
}
