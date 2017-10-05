package com.go2group.stash.pullReqVote;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;

public class PullRequestVoteConfigSchemeServlet extends javax.servlet.http.HttpServlet {

	private static final long serialVersionUID = -1568412967999977272L;
	
	private final LoginUriProvider loginUriProvider;
	private final TemplateRenderer renderer;
	private final UserManager userManager;

	public PullRequestVoteConfigSchemeServlet(final LoginUriProvider loginUriProvider, final UserManager userManager, TemplateRenderer renderer) {
		this.loginUriProvider = loginUriProvider;
		this.renderer = renderer;
		this.userManager = userManager;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// String username = userManager.getRemoteUsername(request);
		String username = userManager.getRemoteUser().getUsername();
		// if (username == null || !userManager.isSystemAdmin(username)) {
		if (username == null || !userManager.isSystemAdmin(userManager.getRemoteUserKey())) {
			redirectToLogin(req, resp);
			return;
		}

		Map<String, Object> context = new HashMap<String, Object>();

		resp.setContentType("text/html;charset=utf-8");
		renderer.render("templates/scheme/gerrit-workflow-scheme.vm", context, resp.getWriter());
	}
	
	private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
	}
	
	private URI getUri(HttpServletRequest request) {
		StringBuffer builder = request.getRequestURL();
		if (request.getQueryString() != null) {
			builder.append("?");
			builder.append(request.getQueryString());
		}
		return URI.create(builder.toString());
	}
}
