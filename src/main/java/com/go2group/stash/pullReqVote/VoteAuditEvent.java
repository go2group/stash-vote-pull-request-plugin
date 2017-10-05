package com.go2group.stash.pullReqVote;

import com.atlassian.bitbucket.audit.Priority;
import com.atlassian.bitbucket.event.annotation.Audited;
import com.atlassian.bitbucket.user.ApplicationUser;

@Audited(converter=VoteAuditEventConverter.class, priority = Priority.LOW)
public class VoteAuditEvent {

    public String key;
    public String value;
    public ApplicationUser user;

    public String getKey() { return key; }
    public String getValue() { return value; }
    public ApplicationUser getUser() { return user; }
    public void setKey(String k) { this.key = k; }
    public void setValue(String v) { this.value = v; }
    public void setUser(ApplicationUser u) { this.user = u; }

    public String toString() {
        return "VoteAuditEvent:[key:" + key + ",value:" + value + ",user:" + user.getSlug() + "]";
    }

    public VoteAuditEvent() {}
    public VoteAuditEvent(String k, String v, ApplicationUser u) {
        this.key = k;
        this.value = v;
        this.user = u;
    }
}
