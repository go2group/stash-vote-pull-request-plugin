package com.go2group.stash.pullReqVote;

import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.mail.MailMessage;
import com.atlassian.bitbucket.mail.MailService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionAdminService;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.permission.PermittedGroup;
import com.atlassian.bitbucket.permission.PermittedUser;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.bitbucket.user.DetailedUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserAdminService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class PullRequestVoterNotify
{
    private static final Logger log = LoggerFactory.getLogger(PullRequestVoterNotify.class);
    private final MailService mailService;
    private final ApplicationPropertiesService applicationPropertiesService;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final PermissionService permissionService;
    private final UserService userService;
    private final PermissionAdminService permissionAdminService;
    private final UserAdminService userAdminService;
    private final SecurityService securityService;

    public PullRequestVoterNotify(final MailService mailService, final ApplicationPropertiesService applicationPropertiesService, final PluginSettingsFactory pluginSettingsFactory, final PermissionService permissionService, final UserService userService, final PermissionAdminService permissionAdminService, final UserAdminService userAdminService, final SecurityService securityService) {
        this.mailService = mailService;
        this.applicationPropertiesService = applicationPropertiesService;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.permissionService = permissionService;
        this.userService = userService;
        this.userAdminService = userAdminService;
        this.permissionAdminService = permissionAdminService;
        this.securityService = securityService;
    }

    @EventListener
    public void mylistener(PullRequestOpenedEvent prEvent) {
        try {
            //securityService.doWithPermission("Notifying PR voters", Permission.ADMIN, new NotifyOperation(prEvent));
        	//securityService.withPermission(Permission.ADMIN, new NotifyOperation(prEvent), "Notifying PR voters");
        }
        catch(Exception e) {
            log.warn("Unable to run PR voter notification as admin: " + e.getMessage(), e);
        }
    }

    private final class NotifyOperation implements Operation<Void, Exception> {
        private final PullRequestOpenedEvent prEvent;

        NotifyOperation(PullRequestOpenedEvent prEvent) {
            this.prEvent = prEvent;
        }

        public Void perform() throws Exception {
            PullRequestVoterNotify.this.notify(prEvent);
            return null;
        }
    }

    private void notify(PullRequestOpenedEvent prEvent) {
        PullRequest pr = prEvent.getPullRequest();
        Repository r = pr.getFromRef().getRepository();
        Project p = r.getProject();
        log.info("A pull request was made in project " + p.getKey() + " and repo " + r.getSlug()); 

        String msg = "Pull request " + pr.getTitle() + " in project " + p.getKey() + "/repository " + r.getSlug() + " is now available for voting";

        // get all roles for this project
        Object o = pluginSettingsFactory.createGlobalSettings().get(PullRequestVoteServlet.SETTINGS_KEY + ".workflows");
        log.debug("Found workflows " + o);
        if (o == null || o.toString().isEmpty()) {
            log.debug("No workflow levels found");
        } // no workflows
        else {
            String sWorkflows = o.toString();
            String[] workflows = sWorkflows.split(",");
            for(String wflow : workflows) {
                String[] wparts = wflow.split("\\|");
                String[] wproj = wparts[1].split("~");
                log.debug("Checking if project " + p.getKey() + " is in array " + wparts[1]);
                if(PullRequestVoteServlet.hasProject(wproj, p.getKey())) {
                    log.debug("Found workflow " + wparts[0] + " for project " + p.getKey());
                    String[] restrictions = wparts[2].split("~");
                    StringBuffer ljson = new StringBuffer("[");
                    for(int ii=0; ii< restrictions.length; ii++) {
                        String restriction = restrictions[ii];
                        String[] rparts = restriction.split("\\^");
                        String level = rparts[0];
                        String[] roles = rparts[1].split("=");
                        String[] groupsAndUsers = rparts[2].split("\n");

                        for(String rl : roles) {
                            if(rl.startsWith("PROJECT")) {
                                Page<PermittedGroup> page = permissionAdminService.findGroupsWithProjectPermission(p, null, new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT));
                                Iterator<PermittedGroup> pageIt = page.getValues().iterator();
                                while(pageIt.hasNext()) {
                                    PermittedGroup pg = pageIt.next();
                                    if(pg.getPermission().equals(Permission.valueOf(rl))) {
                                        Page<DetailedUser> pageU = userAdminService.findUsersWithGroup(pg.getGroup(), null, new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT));
                                        Iterator<DetailedUser> pageUIt = pageU.getValues().iterator();
                                        while(pageUIt.hasNext()) {
                                            DetailedUser du = pageUIt.next();
                                            this.sendEmail(du.getEmailAddress(),applicationPropertiesService.getServerEmailAddress(),"New Pull Request open for voting",msg);
                                        }
                                    }
                                }

                                Page<PermittedUser> pageUser = permissionAdminService.findUsersWithProjectPermission(p, null, new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT));
                                Iterator<PermittedUser> pageUserIt = pageUser.getValues().iterator();
                                while(pageUserIt.hasNext()) {
                                    PermittedUser pu = pageUserIt.next();
                                    if(pu.getPermission().equals(Permission.valueOf(rl))) {
                                        this.sendEmail(pu.getUser().getEmailAddress(),applicationPropertiesService.getServerEmailAddress(),"New Pull Request open for voting",msg);
                                    }
                                }
                            } // project permission
                            else if(rl.startsWith("REPO")) {
                                Page<PermittedGroup> page = permissionAdminService.findGroupsWithRepositoryPermission(r, null, new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT));
                                Iterator<PermittedGroup> pageIt = page.getValues().iterator();
                                while(pageIt.hasNext()) {
                                    PermittedGroup pg = pageIt.next();
                                    if(pg.getPermission().equals(Permission.valueOf(rl))) {
                                         Page<DetailedUser> pageU = userAdminService.findUsersWithGroup(pg.getGroup(), null, new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT));
                                        Iterator<DetailedUser> pageUIt = pageU.getValues().iterator();
                                        while(pageUIt.hasNext()) {
                                            DetailedUser du = pageUIt.next();
                                            this.sendEmail(du.getEmailAddress(),applicationPropertiesService.getServerEmailAddress(),"New Pull Request open for voting",msg);
                                        }
                                    }
                                }

                                Page<PermittedUser> pageUser = permissionAdminService.findUsersWithRepositoryPermission(r, null, new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT));
                                Iterator<PermittedUser> pageUserIt = pageUser.getValues().iterator();
                                while(pageUserIt.hasNext()) {
                                    PermittedUser pu = pageUserIt.next();
                                    if(pu.getPermission().equals(Permission.valueOf(rl))) {
                                        this.sendEmail(pu.getUser().getEmailAddress(),applicationPropertiesService.getServerEmailAddress(),"New Pull Request open for voting",msg);
                                    }
                                }
                            } // repo permission
                            else {
                                Set<String> users = permissionService.getUsersWithPermission(Permission.valueOf(rl));
                                for(String u : users) {
                                    this.sendEmail(userService.getUserByName(u).getEmailAddress(),applicationPropertiesService.getServerEmailAddress(),"New Pull Request open for voting",msg);
                                }
                            } // global permission
                        } // all roles
                        for(String gu : groupsAndUsers) {
                            if(userService.existsGroup(gu)) {
                                Page<DetailedUser> pageU = userAdminService.findUsersWithGroup(gu, null, new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT));
                                Iterator<DetailedUser> pageUIt = pageU.getValues().iterator();
                                while(pageUIt.hasNext()) {
                                    DetailedUser du = pageUIt.next();
                                    this.sendEmail(du.getEmailAddress(),applicationPropertiesService.getServerEmailAddress(),"New Pull Request open for voting",msg);
                                }
                            }
                            else if(null != userService.getUserByName(gu)) {
                                this.sendEmail(userService.getUserByName(gu).getEmailAddress(),applicationPropertiesService.getServerEmailAddress(),"New Pull Request open for voting",msg);
                            }
                        }
                    } // all restrictions
                } // correct project
            } // all workflows
        } // has workflows

    }

    private void sendEmail(String to, String from, String subject, String msg) {
        if(mailService.isHostConfigured()) {
            log.debug("Mail host configured, sending pull request voting mail to " + to + ": " + subject);  
            mailService.submit(new MailMessage.Builder().to(to).from(from).subject(subject).text(msg).build());
        }
        else {
            log.warn("No mail host configured, not sending pull request voting mail to " + to + ": " + subject); 
        }
    }
}
