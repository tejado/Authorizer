package com.box.androidsdk.content.utils;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxListRealTimeServers;
import com.box.androidsdk.content.models.BoxRealTimeServer;
import com.box.androidsdk.content.models.BoxSimpleMessage;
import com.box.androidsdk.content.requests.BoxRequestsEvent;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxResponse;

import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class is used to monitor a long polling server connection, specifically the one used to check that a change has been made to a user's account.
 */
public class RealTimeServerConnection implements BoxFutureTask.OnCompletedListener<BoxSimpleMessage> {
    private BoxRequest mRequest;
    private BoxRealTimeServer mBoxRealTimeServer;
    private BoxSession mSession;
    private final OnChangeListener mChangeListener;
    private final ThreadPoolExecutor mExecutor = SdkUtils.createDefaultThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS);

    private int mRetries = 0;

    /**
     * Constructions a RealTimeServerConnection which will detect when the RealTimeServer indicates when a change has occurred.
     * @param getRealTimeServerRequest A request that will generate a BoxRealTimeServer list. Currently this is only EventRealTimeServerRequest.
     * @param changeListener A listener that will be notified when a change or exception occurs.
     * @param session A session object to use for required network calls.
     */
    public RealTimeServerConnection(BoxRequest getRealTimeServerRequest, OnChangeListener changeListener, BoxSession session) {
        mRequest = getRealTimeServerRequest;
        mSession = session;
        mChangeListener = changeListener;
    }

    /**
     *
     * @return the original request used to construct this object.
     */
    public BoxRequest getRequest(){
        return mRequest;
    }

    /**
     *
     * @return the number of times this class has retried connecting to the server.
     */
    public int getTimesRetried() {
        return mRetries;
    }

    /**
     *
     * @return the information for the real time server this class is trying to connect to.
     */
    public BoxRealTimeServer getRealTimeServer(){
        return mBoxRealTimeServer;
    }

    /**
     *
     * @return create a future task which can be used to monitor the real time connection asynchronously.
     */
    public FutureTask<BoxSimpleMessage> toTask(){
        return new FutureTask<BoxSimpleMessage>(new Callable<BoxSimpleMessage>() {

            @Override
            public BoxSimpleMessage call() throws Exception {
                return RealTimeServerConnection.this.connect();
            }
        });
    }

    /**
     * Returns a message once a change has been detected or error occurs. Otherwise this method will continue reconnecting.
     * @return A BoxSimpleMessage with a simple message indicating a change in the user's account.
     */
    public BoxSimpleMessage connect() {
        mRetries = 0;

        try {
            BoxListRealTimeServers servers =  (BoxListRealTimeServers)mRequest.send();
            mBoxRealTimeServer = servers.get(0);
        } catch (BoxException e){
           mChangeListener.onException(e, this);
           return null;
        }
        BoxRequestsEvent.LongPollMessageRequest messageRequest = new BoxRequestsEvent.LongPollMessageRequest(mBoxRealTimeServer.getUrl(),mSession);
        messageRequest.setTimeOut(mBoxRealTimeServer.getFieldRetryTimeout().intValue() * 1000 );
        boolean shouldRetry = true;
        do {
            BoxFutureTask<BoxSimpleMessage> task = null;
            try {
                task = messageRequest.toTask().addOnCompletedListener(this);
                mExecutor.submit(task);
                BoxResponse<BoxSimpleMessage> response = task.get(mBoxRealTimeServer.getFieldRetryTimeout().intValue(), TimeUnit.SECONDS);
                if (response.isSuccess() && !response.getResult().getMessage().equals(BoxSimpleMessage.MESSAGE_RECONNECT)){
                    return response.getResult();
                }
            } catch (TimeoutException e) {
                if (task != null) {
                    try {
                        // if the timeout is coming from the task then cancel the task (as httpurlconnection timeout is unreliable).
                        task.cancel(true);
                    } catch (CancellationException e1){
                    }
                }
            } catch (InterruptedException e){
                mChangeListener.onException(e,this);
            } catch (ExecutionException e){
                mChangeListener.onException(e,this);
            }
            mRetries++;
            if (mBoxRealTimeServer.getMaxRetries() < mRetries) {
                shouldRetry = false;
            }

        } while(shouldRetry);
        mChangeListener.onException(new BoxException.MaxAttemptsExceeded("Max retries exceeded, ", mRetries), this);
        return null;
    }

    /**
     *
     * @param response BoxSimpleMessage response coming from doing a get call to a RealTimeServer.
     */
    protected void handleResponse(BoxResponse<BoxSimpleMessage> response){
        if (response.isSuccess()) {
            if (!response.getResult().getMessage().equals(BoxSimpleMessage.MESSAGE_RECONNECT)){
                mChangeListener.onChange(response.getResult(),this);
            }
        } else  if (response.getException() instanceof BoxException && response.getException().getCause() instanceof SocketTimeoutException){
            return;
        } else{

            mChangeListener.onException(response.getException(),this);
        }
    }

    /**
     *
     * @param response called with a response from the server connection call.
     */
    @Override
    public void onCompleted(BoxResponse<BoxSimpleMessage> response) {
        handleResponse(response);
    }

    /**
     * An interface used to handle new events from the real time server connection.
     */
    public interface OnChangeListener {

        /**
         *
         * @param message The message returned from the server.
         * @param realTimeServerConnection The real time server connection. Call connect to reconnect.
         *
         */
        public void onChange(final BoxSimpleMessage message, final RealTimeServerConnection realTimeServerConnection);

        /**
         *
         * @param e Exception returned, this can be a MaxRetriesExceededException exception.
         * @param realTimeServerConnection The real time server that connection failed with. Call connect to reconnect.
         */
        public void onException(Exception e, final RealTimeServerConnection realTimeServerConnection);

    }
}