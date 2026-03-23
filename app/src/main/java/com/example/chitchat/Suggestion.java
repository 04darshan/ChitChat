package com.example.chitchat;

import com.google.firebase.Timestamp;

/**
 * Suggestion model
 * Stored at: suggestions/{myUid}/list/{suggestedUid}
 *
 * The suggestion engine (ConnectionManager) writes these docs.
 * Score is based on mutual friend count — higher = shown first.
 */
public class Suggestion {

    private String    suggestedUid;
    private long      mutualCount;   // number of mutual friends
    private double    score;         // computed: mutualCount + recency bonus
    private String    reason;        // e.g. "3 mutual friends"
    private Timestamp createdAt;

    public Suggestion() {} // Firestore

    public Suggestion(String suggestedUid, long mutualCount) {
        this.suggestedUid = suggestedUid;
        this.mutualCount  = mutualCount;
        this.score        = mutualCount;
        this.reason       = mutualCount == 1
                ? "1 mutual friend"
                : mutualCount + " mutual friends";
    }

    public String    getSuggestedUid() { return suggestedUid; }
    public long      getMutualCount()  { return mutualCount; }
    public double    getScore()        { return score; }
    public String    getReason()       { return reason; }
    public Timestamp getCreatedAt()    { return createdAt; }

    public void setSuggestedUid(String v)  { suggestedUid = v; }
    public void setMutualCount(long v)     { mutualCount  = v; }
    public void setScore(double v)         { score        = v; }
    public void setReason(String v)        { reason       = v; }
    public void setCreatedAt(Timestamp v)  { createdAt    = v; }
}