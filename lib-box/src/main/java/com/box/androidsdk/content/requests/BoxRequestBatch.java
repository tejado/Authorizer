package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxObject;

import java.util.ArrayList;

/**
 * Batch request class that allows the ability to send multiple BoxRequests through an executor and
 * return a batch response object that contains all of the response information for each individual request
 */
public class BoxRequestBatch extends BoxRequest<BoxResponseBatch, BoxRequestBatch> {

    protected ArrayList<BoxRequest> mRequests = new ArrayList<BoxRequest>();

    /**
     * Initializes a new BoxRequestBatch
     */
    public BoxRequestBatch() {
        super(BoxResponseBatch.class, null, null);
    }

    /**
     * Adds a BoxRequest to the batch
     *
     * @param request the BoxRequest to add
     * @return the batch request
     */
    public BoxRequestBatch addRequest(BoxRequest request) {
        mRequests.add(request);
        return this;
    }

    @Override
    public BoxResponseBatch send() throws BoxException {
        BoxResponseBatch responses = new BoxResponseBatch();

        for (BoxRequest req : mRequests) {
            BoxObject value = null;
            Exception ex = null;
            try {
                value = req.send();
            } catch (Exception e) {
                ex = e;
            }

            BoxResponse<BoxObject> response = new BoxResponse<BoxObject>(value, ex, req);
            responses.addResponse(response);
        }

        return responses;
    }
}
