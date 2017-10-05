package com.go2group.stash.pullReqVote;

import javax.ws.rs.Path;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

@Path("/GerritWorkflowScheme")
public class GerritWorkflowSchemeConfig {

	private final PluginSettingsFactory pluginSettingsFactory;
	
	public GerritWorkflowSchemeConfig(PluginSettingsFactory pluginSettingsFactory) {
		this.pluginSettingsFactory = pluginSettingsFactory;
	}
}

