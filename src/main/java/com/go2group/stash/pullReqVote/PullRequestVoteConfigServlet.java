package com.go2group.stash.pullReqVote;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;

public final class PullRequestVoteConfigServlet extends javax.servlet.http.HttpServlet {

    private final PluginSettingsFactory pluginSettingsFactory;
    private final UserManager userManager;
    private final LoginUriProvider loginUriProvider;
    private final TemplateRenderer renderer;
	private static final Logger log = LoggerFactory.getLogger(PullRequestVoteConfigServlet.class);

	public PullRequestVoteConfigServlet(final PluginSettingsFactory pluginSettingsFactory, final UserManager userManager,
			final LoginUriProvider loginUriProvider, final TemplateRenderer renderer) {
        log.debug("in ctor");
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.renderer = renderer;
    } // ctor

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
    	//String username = userManager.getRemoteUsername(request);
		String username = userManager.getRemoteUser().getUsername();
		//if (username == null || !userManager.isSystemAdmin(username)) {
		if (username == null || !userManager.isSystemAdmin(userManager.getRemoteUserKey())) {
            redirectToLogin(req, resp);
            return;
        }

		Map<String, Object> context = new HashMap<String, Object>();
		
		Permission[] permissions = Permission.values();
		context.put("permissions", permissions);

        resp.setContentType("text/html;charset=utf-8");
		renderer.render("admin.vm", context, resp.getWriter());
	}

	private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }

    private URI getUri(HttpServletRequest request)
    {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null)
        {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }

    public String getServletName() {
        return "PullRequestVoteConfigServlet";
    }
} // class PullRequestVoteConfigServlet

