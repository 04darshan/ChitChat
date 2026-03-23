package com.example.chitchat;

import com.google.firebase.Timestamp;

/**
 * ConnectionRequest model
 * Stored at: connections/{recipientUid}/requests/{senderUid}
 *
 * Fields:
 *  senderUid    — who sent it
 *  recipientUid — who receives it
 *  message      — optional personal note (max 200 chars)
 *  status       — "pending" | "accepted" | "declined"
 *  mutualCount  — number of mutual friends at time of sending (for display)
 *  timestamp    — server timestamp
 */
public class ConnectionRequest {

    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_DECLINED = "declined";

    private String    senderUid;
    private String    recipientUid;
    private String    message;       // optional note
    private String    status;
    private long      mutualCount;
    private Timestamp timestamp;

    public ConnectionRequest() {} // Firestore

    public ConnectionRequest(String senderUid, String recipientUid,
                             String message, long mutualCount) {
        this.senderUid    = senderUid;
        this.recipientUid = recipientUid;
        this.message      = message != null ? message : "";
        this.status       = STATUS_PENDING;
        this.mutualCount  = mutualCount;
    }

    public String    getSenderUid()    { return senderUid; }
    public String    getRecipientUid() { return recipientUid; }
    public String    getMessage()      { return message != null ? message : ""; }
    public String    getStatus()       { return status; }
    public long      getMutualCount()  { return mutualCount; }
    public Timestamp getTimestamp()    { return timestamp; }

    public void setSenderUid(String v)    { senderUid    = v; }
    public void setRecipientUid(String v) { recipientUid = v; }
    public void setMessage(String v)      { message      = v; }
    public void setStatus(String v)       { status       = v; }
    public void setMutualCount(long v)    { mutualCount  = v; }
    public void setTimestamp(Timestamp v) { timestamp    = v; }
}