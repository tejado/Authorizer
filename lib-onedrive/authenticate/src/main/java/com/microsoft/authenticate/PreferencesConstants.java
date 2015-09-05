// ------------------------------------------------------------------------------
// Copyright (c) 2014 Microsoft Corporation
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
// ------------------------------------------------------------------------------

package com.microsoft.authenticate;

/**
 * Static class that holds constants used by an application's preferences.
 */
final class PreferencesConstants {
    /**
     * Cookies key
     */
    public static final String COOKIES_KEY = "cookies";

    /**
     * Name of the preference file
     * */
    public static final String FILE_NAME = "com.microsoft.authenticate";

    /**
     * The refresh token key
     */
    public static final String REFRESH_TOKEN_KEY = "refresh_token";

    /**
     * The delimiter for cookies
     */
    public static final String COOKIE_DELIMITER = ",";

    /**
     * Private constructor for utility classes
     */
    private PreferencesConstants() {
    }
}
