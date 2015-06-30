package com.microsoft.authenticate;

import android.os.AsyncTask;

/**
 * TokenRequestAsync performs an async token request. It takes in a TokenRequest,
 * executes it, checks the OAuthResponse, and then calls the given listener.
 */
class TokenRequestAsync extends AsyncTask<Void, Void, Void> implements ObservableOAuthRequest {

    /**
     * The observables
     */
    private final DefaultObservableOAuthRequest mObservable;

    /** Not null if there was an exception */
    private AuthException mException;

    /** Not null if there was a response */
    private OAuthResponse mResponse;

    /**
     * The token request
     */
    private final TokenRequest mRequest;

    /**
     * Constructs a new TokenRequestAsync and initializes its member variables
     *
     * @param request to perform
     */
    public TokenRequestAsync(final TokenRequest request) {
        mObservable = new DefaultObservableOAuthRequest();
        mRequest = request;
    }

    @Override
    public void addObserver(final OAuthRequestObserver observer) {
        mObservable.addObserver(observer);
    }

    @Override
    public boolean removeObserver(final OAuthRequestObserver observer) {
        return mObservable.removeObserver(observer);
    }

    @Override
    protected Void doInBackground(final Void... params) {
        try {
            mResponse = mRequest.execute();
        } catch (final AuthException e) {
            mException = e;
        }

        return null;
    }

    @Override
    protected void onPostExecute(final Void result) {
        super.onPostExecute(result);

        if (mResponse != null) {
            mObservable.notifyObservers(mResponse);
        } else if (mException != null) {
            mObservable.notifyObservers(mException);
        } else {
            final AuthException exception = new AuthException(ErrorMessages.CLIENT_ERROR);
            mObservable.notifyObservers(exception);
        }
    }
}
