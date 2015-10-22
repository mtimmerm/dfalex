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

class BitUtils
{
    private static final int[] DEBRUIJN_WINDOW_TO_BIT_POSITION= 
    {
      0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 
      31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
    };
    
    public static int lowBit(int x)
    {
        return x&-x; //== x & ~(x-1)
    }
    
    public static int lowBitIndex(int x) //undefined if x==0
    {
        x&=-x;
        return DEBRUIJN_WINDOW_TO_BIT_POSITION[((x * 0x077CB531) >>> 27)&31];
    }
    
    public static int turnOffLowBit(int x)
    {
        return x & (x-1);
    }
    
    public static int bitsGE(int position)
    {
        return ~((1<<position)-1);
    }
}
