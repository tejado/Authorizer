package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxObject;

/**
 * This class acts as a wrapper for holding onto the response or exceptions returned from the result of a BoxRequest.
 * @param <E> The BoxObject that was generated from a response to a BoxRequest.
 */
public class BoxResponse<E extends BoxObject> {

    protected final E mResult;
    protected final Exception mException;
    protected final BoxRequest mRequest;

    // BoxResponse should never be instantiated by themselves and instead should be created from a BoxHttpResponse

    /**
     *
     * @param result the BoxObject generated from handling BoxHttpResponse if any.
     * @param ex the exception thrown from a handling a BoxHttpResponse if any.
     * @param request the original request that generated the BoxHttpResponse sourcing the result or exception.
     */
    public BoxResponse(E result, Exception ex, BoxRequest request) {
        mResult = result;
        mException = ex;
        mRequest = request;
    }

    /**
     *
     * @return the BoxObject parsed from a given server response.
     */
    public E getResult() {
        return mResult;
    }

    /**
     *
     * @return the exception thrown from a given server response.
     */
    public Exception getException() {
        return mException;
    }

    /**
     *
     * @return the request used to generate the response or exception backing this object.
     */
    public BoxRequest getRequest() {
        return mRequest;
    }

    /**
     *
     * @return true if this object was not created from an exception, false otherwise.
     */
    public boolean isSuccess() {
        return mException == null;
    }
}
