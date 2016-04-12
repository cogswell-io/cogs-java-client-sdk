package com.gambit.sdk;

import org.json.JSONException;
import org.json.JSONObject;

public class GambitResponse {

    /**
     * raw response body
     */
    protected String mRawBody;
    
    /**
     * raw response HTTP code
     */
    protected int mRawCode;
    
    /**
     * indicate whether the request went as planned
     */
    protected boolean mIsSuccess;
    
    /**
     * used when the request failed
     */
    protected String mErrorCode;
    
    /**
     * used when the request failed; not always populated
     */
    protected String mErrorDetails;
    
    /**
     * JSON representation of the mRawBody; not always populated
     */
    protected JSONObject mJson;

    /**
     * Create an instance to wrap an API call response
     * @param response The raw HTTP response body as text
     * @param code The raw HTTP response code as an integer
     */
    public GambitResponse(String response, int code) {
        mRawBody = response;
        mRawCode = code;
        
        try {
            mJson = new JSONObject(response);
            
            if (mJson.has("error_message")) {
                mIsSuccess = false;
                
                mErrorCode = mJson.getString("error_message");
                
                if (mJson.has("details")) {
                    mErrorDetails = mJson.getString("details");
                }
            }
            else {
                mIsSuccess = true;
            }
        }
        catch (JSONException e) {
            //bad response
            
            mIsSuccess = false;
            
            mErrorCode = "UNKNOWN";
            mErrorDetails = e.getMessage()+": "+response;
        }
    }
    
    /**
     * Returns the raw HTTP response body
     * @return The raw HTTP response body as read from the input stream
     */
    public String getRawBody() {
        return mRawBody;
    }
    
    /**
     * Returns the raw HTTP response code
     * @return The original HTTP response code as an integer
     */
    public int getRawCode() {
        return mRawCode;
    }
    
    /**
     * Indicates whether the request went as planned
     * @return An indication whether the response is valid and successful
     */
    public boolean isSuccess() {
        return mIsSuccess;
    }
    
    /**
     * Returns the raw JSONObject parsed from the raw HTTP response body
     * @return JSONObject based on the original raw HTTP response body
     */
    public JSONObject getObject() {
        return mJson;
    }
    
    
}
