package com.gambit.sdk.response;

import com.gambit.sdk.GambitResponse;

public class GambitResponseEvent extends GambitResponse {

    /**
     * Gambit API Event Response Message
     */
    protected String mMessage;

    /**
     * Construct the response object using the raw response body and response code
     * @param response The raw HTTP response body as text
     * @param code The raw HTTP response code as an integer
     */
    public GambitResponseEvent(String response, int code) {
        super(response, code);
        
        if (isSuccess()) {
            if (mJson.has("message")) {
                mMessage = mJson.getString("message");
            }
            else {
                //not good at all
                
                mIsSuccess = false;
                mErrorCode = "UNKNOWN";
                mErrorDetails = "Unknown response: "+response;
            }
        }
    }
    
    /**
     * Get Gambit API Event Response Message
     * @return A server message indicating the request status in human readable format
     */
    public String getMessage() {
        return mMessage;
    }

}
