package com.go2group.stash.pullReqVote;

import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.project.ProjectService;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.user.UserManager;
import com.go2group.stash.pullReqVote.ao.Vote;

import net.java.ao.Query;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public final class PullRequestVoteServlet extends javax.servlet.http.HttpServlet {

	private final ActiveObjects ao;

	private final EventPublisher eventPublisher;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final PermissionService permissionService;
	private final ProjectService projectService;
	private final RepositoryService repoService;
	private final UserManager userManager;
	private final UserService userService;
	
	public static final String SETTINGS_KEY = "g2g." + PullRequestVoteServlet.class.getName();

	private static final Logger log = LoggerFactory.getLogger(PullRequestVoteServlet.class);

    public PullRequestVoteServlet(ActiveObjects ao, final PluginSettingsFactory pluginSettingsFactory, final PermissionService permissionService,
            final ProjectService projectService, final RepositoryService repoService, final EventPublisher eventPublisher, final UserManager userManager, 
            final UserService userService) {
        log.debug("in ctor");
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.permissionService = permissionService;
        this.projectService = projectService;
        this.repoService = repoService;
        this.eventPublisher = eventPublisher;
        this.userManager = userManager;
        this.userService = userService;
        this.ao = ao;
    } // ctor

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        String json = req.getParameter("jsondata");
        String op = req.getParameter("op");
        String key = req.getParameter("key");
        //String username = userManager.getRemoteUsername(request);
  		String username = userManager.getRemoteUser().getUsername();

        final PrintWriter w = resp.getWriter();
		resp.setContentType("application/json");
		
        if(null != json && null != op && null != key) {
            log.info("Performing " + op + " on key " + key + " for data " + json);
            if(op.equals("put")) {
                putVotes(key, json, username);
            }
            else if(op.equals("levels")) {
                Object o = pluginSettingsFactory.createGlobalSettings().get(SETTINGS_KEY + ".workflows");
                log.info("Found workflows " + o);
                if (o == null || o.toString().isEmpty()) {
                    log.info("No workflow levels found");
                } // now workflows
                else {
                    String pkey = req.getParameter("pkey");
                    String rkey = req.getParameter("rkey");
                    String sWorkflows = o.toString();
                    String[] workflows = sWorkflows.split(",");
                    for(String wflow : workflows) {
                        String[] wparts = wflow.split("\\|");
                        String[] wproj = wparts[1].split("~");
                        log.debug("Checking if project " + pkey + " is in array " + wparts[1]);
                        if(PullRequestVoteServlet.hasProject(wproj, pkey)) {
                            log.debug("Found workflow " + wparts[0] + " for project " + pkey);
                            String[] restrictions = wparts[2].split("~");
                            StringBuffer ljson = new StringBuffer("[");
                            for(int ii=0; ii< restrictions.length; ii++) {
                                String restriction = restrictions[ii];
                                String[] rparts = restriction.split("\\^");
                                String level = rparts[0];
                                String[] roles = rparts[1].split("=");
                                boolean hasProjectPerm = false;
                                boolean hasUserOrGroupPerm = false;
                                for(String r : roles) {
                                    if(r.startsWith("PROJECT")) {
                                        if(permissionService.hasProjectPermission(projectService.getByKey(pkey),
                                                    Permission.valueOf(r))) {
                                        	hasProjectPerm = true;
                                            break;
                                        }
                                    } // project permission
                                    else if(r.startsWith("REPO")) {
                                        if(permissionService.hasRepositoryPermission(repoService.getBySlug(pkey, rkey),
                                                    Permission.valueOf(r))) {
                                        	hasProjectPerm = true;
                                            break;
                                        }
                                    } // repo permission
                                    else {
                                        if(permissionService.hasGlobalPermission(Permission.valueOf(r))) {
                                        	hasProjectPerm = true;
                                            break;
                                        }
                                    } // global permission
                                }
                                String[] groupsAndUsers = rparts[2].trim().split(";");
                                for(String gu : groupsAndUsers) {
                                    log.debug("Checking group/user " + gu);
                                    if(!gu.isEmpty() && (userService.isUserInGroup(username, gu) || username.equals(gu))) {
                                    	hasUserOrGroupPerm = true;
                                        break;
                                    }
                                }
                                if(ii > 0) {
                                    ljson.append(",");
                                }
                                ljson.append("{\"id\":\"");
                                ljson.append(level);
                                ljson.append("\",\"hasPerm\":\"");
                                if (hasProjectPerm && hasUserOrGroupPerm) {
                                    ljson.append("");
                                } else if (!hasProjectPerm && hasUserOrGroupPerm) {
                                	ljson.append("disabled");
                                } else if (hasProjectPerm && !hasProjectPerm) {
                                	ljson.append("disabled");
                                } else {
                                    ljson.append("disabled");
                                }
                                ljson.append("\"}");
                            } // all restrictions
                            ljson.append("]");
                            resp.setContentType("application/json");
                            resp.getWriter().write(ljson.toString());
                            log.info("Found levels json " + ljson.toString());
                            break;
                        } // found a match
                    } // all workflows

                } // has workflows
            } // find levels
            else {
                final String actualKey = key.replaceFirst("go2group.vote.pullrequest.", "");

				ao.executeInTransaction(new TransactionCallback<Void>() {
					@Override
					public Void doInTransaction() {
						JSONArray jsonArray = new JSONArray();
						for (Vote vote : ao.find(Vote.class, Query.select().where("PULL_REQ_KEY = ?", actualKey))) {
							// for (Vote vote: ao.find(Vote.class)) {
							JSONObject jsonobj = new JSONObject();
							jsonobj.put("id", vote.getID());
							jsonobj.put("pullReqKey", vote.getPullReqKey());
							jsonobj.put("author", vote.getUsername());
							jsonobj.put("text", vote.getVote());
							jsonobj.put("created", vote.getTime());
							jsonArray.add(jsonobj);
						}
						w.printf(jsonArray.toString());
						return null;
					}
				});
            }
        }
        else if (json == null && op.equals("remove")) {
			removeVotes(key, json, req.getParameter("voteId"));
		}
        else {
            log.warn("No params!");
        }
    } // doGet

    public String getServletName() {
        return "PullRequestVoteServlet";
    }

    private void putVotes(final String key, final String json, final String username) {
		JSONArray array = JSONArray.fromObject(json);
		final JSONObject jsonObject = (JSONObject) array.get(array.size() - 1);

		final String actualKey = key.replaceFirst("go2group.vote.pullrequest.", "");

		ao.executeInTransaction(new TransactionCallback<Vote>() {
			@Override
			public Vote doInTransaction() {
				Vote[] votes = ao.find(Vote.class, Query.select().where("PULL_REQ_KEY = ? and USERNAME = ?", actualKey, jsonObject.getString("author")));
				if (votes.length > 1) {
					log.error("More than one vote found.. Something is wrong..");
					return null;
				}

				Vote vote = null;
				if (votes.length > 0)
					vote = votes[0];
				else
					vote = ao.create(Vote.class);

				vote.setPullReqKey(actualKey);
				vote.setTime(jsonObject.getString("created"));
				vote.setUsername(jsonObject.getString("author"));
				vote.setVote(jsonObject.getString("text"));
				vote.save();

				// publish to audit log
                VoteAuditEvent auditEvent = new VoteAuditEvent(key, json, userService.findUserByNameOrEmail(username));
                log.debug("About to publish audit event " + auditEvent);
                eventPublisher.publish(auditEvent);
                
				return vote;
			}
		});
	}

	private void removeVotes(String key, String json, final String voteId) {
		ao.executeInTransaction(new TransactionCallback<Vote>() {
			@Override
			public Vote doInTransaction() {
				Vote[] votes = ao.find(Vote.class, Query.select().where("ID = ?", Long.parseLong(voteId)));
				if (votes.length > 0) {
					Vote vote = votes[0];
					ao.delete(vote);

					return null;
				}

				return null;
			}
		});
	}

    static boolean hasProject(String[] plist, String pkey) {
        boolean has = false;
        for(String p : plist) {
            if(p.equalsIgnoreCase(pkey)) {
                has = true;
                break;
            }
        }

        return has;
    }
}
