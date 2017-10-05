package com.go2group.stash.pullReqVote;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bitbucket.audit.AuditEntry;
import com.atlassian.bitbucket.audit.AuditEntryBuilder;
import com.atlassian.bitbucket.audit.AuditEntryConverter;

public class VoteAuditEventConverter implements AuditEntryConverter<VoteAuditEvent> {
    private static final Logger log = LoggerFactory.getLogger(AuditEntryConverter.class);

    public VoteAuditEventConverter() {}

    
    @Nonnull
	@Override
	public AuditEntry convert(VoteAuditEvent event, AuditEntryBuilder builder) {
		log.debug("event: " + event);
        log.debug("builder: " + builder);
          return builder.
              action(event.getClass()).
              timestamp(System.currentTimeMillis()).
              details(event.getValue()).
              target(event.getKey()).
              user(event.getUser()).build();
	}
}

