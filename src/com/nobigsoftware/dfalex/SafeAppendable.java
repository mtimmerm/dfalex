package com.nobigsoftware.dfalex;


/**
 * A refinement of the {@link Appendable} interface that doesn't throw exceptions
 */
public interface SafeAppendable extends Appendable
{
    @Override
    public SafeAppendable append(char c);

    @Override
    public SafeAppendable append(CharSequence csq, int start, int end);

    @Override
    public SafeAppendable append(CharSequence csq);
}
