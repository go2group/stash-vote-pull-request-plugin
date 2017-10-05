package com.go2group.stash.pullReqVote.ao;

import net.java.ao.Entity;

public interface Vote extends Entity {

	String getPullReqKey();
	
	void setPullReqKey(String key);
	
	String getTime();

	void setTime(String time);

	String getUsername();

	void setUsername(String username);

	String getVote();

	void setVote(String vote);
}
