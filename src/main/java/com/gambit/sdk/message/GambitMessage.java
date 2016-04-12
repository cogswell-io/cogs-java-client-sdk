package com.gambit.sdk.message;

import org.json.JSONObject;

public class GambitMessage {

    /**
     * Message ID
     */
    protected String mMessageId;

    /**
     * CIID Hash
     */
    protected String mCiidHash;

    /**
     * Campaign ID, if any, default is -1
     */
    protected int mCampaignId = -1;

    /**
     * Campaign Name, if any
     */
    protected String mCampaignName;

    /**
     * Namespace associated with this event
     */
    protected String mNamespace;

    /**
     * Event name
     */
    protected String mEventName;

    /**
     * The raw payload of the message data property
     */
    protected String mData;

    /**
     * The JSONObject representation of the message data property
     */
    protected JSONObject mDataObject;

    /**
     * Determines whether the message format is OK
     */
    protected boolean mIsValid;

    /**
     * The original message content. Useful for debugging and logging.
     */
    protected String mRawMessage;

    public GambitMessage(String message) {

        mRawMessage = message;
        mIsValid = true;

        JSONObject payload = null;

        try {
            payload = new JSONObject(mRawMessage);
        }
        catch (Exception e) {
            mIsValid = false;

            return;
        }

        if (payload.has("message_id")) {
            mMessageId = payload.getString("message_id");
        }
        else {
            mIsValid = false;
        }

        if (payload.has("ciid_hash")) {
            mCiidHash = payload.getString("ciid_hash");
        }
        else {
            mIsValid = false;
        }

        if (payload.has("campaign_id")) {
            mCampaignId = payload.getInt("campaign_id");
        }
        else {
            mIsValid = false;
        }

        if (payload.has("campaign_name")) {
            mCampaignName = payload.getString("campaign_name");
        }
        else {
            mIsValid = false;
        }

        if (payload.has("namespace")) {
            mNamespace = payload.getString("namespace");
        }
        else {
            mIsValid = false;
        }

        if (payload.has("event_name")) {
            mEventName = payload.getString("event_name");
        }
        else {
            mIsValid = false;
        }

        if (payload.has("data")) {
            mData = payload.getString("data");

            try {
                mDataObject = new JSONObject(mData);
            }
            catch (Exception e) {
                mIsValid = false;
            }
        }

    }

    /**
     * Message ID
     *
     * @return Message ID
     */
    public String getMessageId() {
        return mMessageId;
    }

    /**
     * CIID Hash
     *
     * @return CIID Hash
     */
    public String getCiidHash() {
        return mCiidHash;
    }

    /**
     * Campaign ID, if any, default is -1
     *
     * @return Campaign ID
     */
    public int getCampaignId() {
        return mCampaignId;
    }

    /**
     * Campaign Name, if any
     *
     * @return Campaign name
     */
    public String getCampaignName() {
        return mCampaignName;
    }

    /**
     * Namespace associated with this event
     *
     * @return Namespace
     */
    public String getNamespace() {
        return mNamespace;
    }

    /**
     * Event Name
     *
     * @return Event Name
     */
    public String getEventName() {
        return mEventName;
    }

    /**
     * The raw payload of the message data property
     *
     * @return Message Data as String
     */
    public String getData() {
        return mData;
    }

    /**
     * The JSONObject representation of the message data property
     *
     * @return Message Data as JSONObject
     */
    public JSONObject getDataObject() {
        return mDataObject;
    }

    ///

    /**
     * Determines whether the message format is OK
     *
     * @return boolean
     */
    public boolean isValid() {
        return mIsValid;
    }

    /**
     * The original message content. Useful for debugging and logging.
     *
     * @return Raw Message Content
     */
    public String getRawMessage() {
        return mRawMessage;
    }
}
