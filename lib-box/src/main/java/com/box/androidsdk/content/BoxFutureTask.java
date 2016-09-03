package com.box.androidsdk.content;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.box.androidsdk.content.models.BoxObject;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxResponse;

/**
 * 
 * @param <E>
 *            - entity type returned from request
 */
public class BoxFutureTask<E extends BoxObject> extends FutureTask<BoxResponse<E>> {

    protected final BoxRequest mRequest;
    protected ArrayList<OnCompletedListener<E>> mCompletedListeners = new ArrayList<OnCompletedListener<E>>();

    public BoxFutureTask(final Class<E> clazz, final BoxRequest request) {
        super(new Callable<BoxResponse<E>>() {

            @Override
            public BoxResponse<E> call() throws Exception {
                E ret = null;
                Exception ex = null;
                try {
                    ret = (E) request.send();
                } catch (Exception e) {
                    ex = e;
                }
                return new BoxResponse<E>(ret, ex, request);
            }
        });
        mRequest = request;
    }

    @Override
    protected void done() {
        BoxResponse<E> response = null;
        Exception ex = null;
        try {
            response = this.get();
        } catch (InterruptedException e) {
            ex = e;
        } catch (ExecutionException e) {
            ex = e;
        }

        if (ex != null) {
            response = new BoxResponse<E>(null, new BoxException("Unable to retrieve response from FutureTask.", ex), mRequest);
        }

        ArrayList<OnCompletedListener<E>> listener = getCompletionListeners();
        for (OnCompletedListener<E> l : listener) {
            l.onCompleted(response);
        }
    }

    public ArrayList<OnCompletedListener<E>> getCompletionListeners() {
        return mCompletedListeners;
    }

    @SuppressWarnings("unchecked")
    public BoxFutureTask<E> addOnCompletedListener(OnCompletedListener<E> listener) {
        mCompletedListeners.add(listener);
        return this;
    }

    public interface OnCompletedListener<E extends BoxObject> {

        public void onCompleted(BoxResponse<E> response);
    }

}
