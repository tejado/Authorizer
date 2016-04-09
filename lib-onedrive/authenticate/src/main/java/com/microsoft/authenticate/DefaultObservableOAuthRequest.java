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

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of an ObserverableOAuthRequest.
 * Other classes that need to be observed can compose themselves out of this class.
 */
class DefaultObservableOAuthRequest implements ObservableOAuthRequest {

    /**
     * The observer
     */
    private final List<OAuthRequestObserver> mObservers;

    /**
     * Default constructor
     */
    public DefaultObservableOAuthRequest() {
        this.mObservers = new ArrayList<>();
    }

    @Override
    public void addObserver(final OAuthRequestObserver observer) {
        this.mObservers.add(observer);
    }

    /**
     * Calls all the Observerable's observer's onException.
     *
     * @param exception to give to the observers
     */
    public void notifyObservers(final AuthException exception) {
        for (final OAuthRequestObserver observer : this.mObservers) {
            observer.onException(exception);
        }
    }

    /**
     * Calls all this Observable's observer's onResponse.
     *
     * @param response to give to the observers
     */
    public void notifyObservers(final OAuthResponse response) {
        for (final OAuthRequestObserver observer : this.mObservers) {
            observer.onResponse(response);
        }
    }

    @Override
    public boolean removeObserver(final OAuthRequestObserver observer) {
        return this.mObservers.remove(observer);
    }
}
