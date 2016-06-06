package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxObject;

import java.util.ArrayList;

/**
 * Batch response class that contains all the response information for a completed BoxRequestBatch
 */
public class BoxResponseBatch extends BoxObject {

    /**
     * Collection of the response objects
     */
    protected ArrayList<BoxResponse> mResponses = new ArrayList<BoxResponse>();

    /**
     * Returns the collection of response objects
     *
     * @return collection of response objects
     */
    public ArrayList<BoxResponse> getResponses() {
        return mResponses;
    }

    /**
     * Adds a response to the collection
     *
     * @param response the response to add
     */
    public void addResponse(BoxResponse response) {
        mResponses.add(response);
    }

}
