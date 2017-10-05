package com.go2group.stash.pullReqVote;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bitbucket.project.ProjectService;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.base.Joiner;

@Path("/")
public class PullRequestVoteConfig {
	private final UserManager userManager;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final TransactionTemplate transactionTemplate;
	private final ProjectService projectService;
	private static final Logger log = LoggerFactory.getLogger(PullRequestVoteConfig.class);

	public PullRequestVoteConfig(UserManager userManager, PluginSettingsFactory pluginSettingsFactory, TransactionTemplate transactionTemplate, ProjectService projectService) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
		this.projectService = projectService;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class Config {
		
		@XmlElement private String workflows;
		@XmlElement private String projects;
		@XmlElement private String delete;
		
		public Config() {}
		
		public Config(String workflows) {
			this.workflows = workflows;
		}

		public String getWorkflows() {
			return workflows;
		}

		public void setWorkflows(String workflows) {
			this.workflows = workflows;
		}

		public String getProjects() {
			return projects;
		}

		public void setProjects(String projects) {
			this.projects = projects;
		}

		public String getDelete() {
			return delete;
		}

		public void setDelete(String delete) {
			this.delete = delete;
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) {
		return Response.ok(transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
				Config config = new Config();
				Object o = settings.get(PullRequestVoteServlet.SETTINGS_KEY + ".workflows");
				log.info("Read stored config workflows " + o);
				if (o != null) {
					config.setWorkflows(o.toString());
				} else {
					config.setWorkflows("");
				}

				// load all projects
				List<String> prKeys = projectService.findAllKeys();
				StringBuilder sb = new StringBuilder();
				for (String prKey : prKeys) {
					sb.append(prKey);
					sb.append(",");
				}
				config.setProjects(sb.toString());

				if (config.projects.equals("")) {
					return null;
				}

				return config;
			}
		})).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(final Config config, @Context HttpServletRequest request) {
		//String username = userManager.getRemoteUsername(request);
		String username = userManager.getRemoteUser().getUsername();
		//if (username == null || !userManager.isSystemAdmin(username)) {
		if (username == null || !userManager.isSystemAdmin(userManager.getRemoteUserKey())) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
				log.info("Storing workflows " + config.getWorkflows());
				pluginSettings.put(PullRequestVoteServlet.SETTINGS_KEY + ".workflows", config.getWorkflows());
				return null;
			}
		});
		return Response.noContent().build();
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	public Response delete(final Config config, @Context HttpServletRequest request) {
		//String username = userManager.getRemoteUsername(request);
		String username = userManager.getRemoteUser().getUsername();
		//if (username == null || !userManager.isSystemAdmin(username)) {
		if (username == null || !userManager.isSystemAdmin(userManager.getRemoteUserKey())) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String wfnameToDelete = config.getDelete();
		if (wfnameToDelete.equals("") || wfnameToDelete == null) {
			log.error("Unable to delete workflow as the name is not found..");
		}

		// Get existing configs
		PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
		Config configgg = new Config();
		Object o = settings.get(PullRequestVoteServlet.SETTINGS_KEY + ".workflows");
		log.info("Read stored config workflows " + o);
		if (o != null) {
			configgg.setWorkflows(o.toString());
		} else {
			configgg.setWorkflows("");
		}

		// load all projects
		List<String> prKeys = projectService.findAllKeys();
		StringBuilder sb = new StringBuilder();
		for (String prKey : prKeys) {
			sb.append(prKey);
			sb.append(",");
		}
		configgg.setProjects(sb.toString());

		List<String> newList = new ArrayList<String>();
		String[] workflows = ((String) o).split(",");
		for (int i = 0; i < workflows.length; i++) {
			// String workflowName = workflows[i].split("|")[0];
			String workflow = workflows[i];
			String[] workflowCombination = workflow.split("\\|");
			String workflowName = workflowCombination[0];

			if (!workflowName.equals(wfnameToDelete)) {
				newList.add(workflows[i]);
			}
		}

		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		log.info("Storing undeleted workflows " + config.getWorkflows());
		if (!newList.isEmpty())
			pluginSettings.put(PullRequestVoteServlet.SETTINGS_KEY + ".workflows", Joiner.on(",").join(newList));
		else
			pluginSettings.put(PullRequestVoteServlet.SETTINGS_KEY + ".workflows", "");

		return Response.noContent().build();
	}
}
