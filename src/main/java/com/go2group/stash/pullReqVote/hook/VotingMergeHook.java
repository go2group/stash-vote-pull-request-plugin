package com.go2group.stash.pullReqVote.hook;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.scm.pull.MergeRequest;
import com.atlassian.bitbucket.scm.pull.MergeRequestCheck;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.go2group.stash.pullReqVote.PullRequestVoteServlet;
import com.go2group.stash.pullReqVote.ao.Vote;

import net.java.ao.Query;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Implements default Gerrit workflow: merge requests must have a '+2' and no
 * '-2' votes.
 */
public class VotingMergeHook implements MergeRequestCheck {

	private static final Logger log = LoggerFactory.getLogger(VotingMergeHook.class);

	final private static String[] units = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
		"thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen" };
final private static String[] tens = { "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety" };
	
	private final ActiveObjects ao;
	private final PluginSettingsFactory pluginSettingsFactory;

	public VotingMergeHook(ActiveObjects ao, PluginSettingsFactory pluginSettingsFactory) {
		this.ao = ao;
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	@Override
	public void check(@Nonnull MergeRequest request) {
		PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
		PullRequest pr = request.getPullRequest();
		String pkey = pr.getToRef().getRepository().getProject().getKey();
		String key = "go2group.vote.pullrequest." + pkey + '/' + pr.getToRef().getRepository().getSlug() + '/' + pr.getId();
		//Object o = settings.get(PullRequestVoteServlet.SETTINGS_KEY + key);
		Object o = getVote(pkey + '/' + pr.getToRef().getRepository().getSlug() + '/' + pr.getId());
		log.debug("Found object " + o);
		Object oWorkflow = settings.get(PullRequestVoteServlet.SETTINGS_KEY + ".workflows");
		log.debug("Relevant workflows: " + oWorkflow);
		if (null != oWorkflow && false == oWorkflow.toString().isEmpty()) {
			String sWorkflows = oWorkflow.toString();
			String sVotes = (o == null) ? "" : o.toString();
			log.debug("Found votes: " + sVotes);

			// See if this project has any workflows
			String[] workflows = sWorkflows.split(",");
			for (String wflow : workflows) {
				String[] wparts = wflow.split("\\|");
				String[] wproj = wparts[1].split("~");
				if (hasProject(wproj, pkey)) {
					log.debug("Found workflow " + wparts[0] + " for project " + pkey);
					String[] restrictions = wparts[2].split("~");
					for (String restriction : restrictions) {
						String[] rparts = restriction.split("\\^");
						String level = rparts[0];
						int musthave = safeToInt(rparts[3]);
						int nothave = safeToInt(rparts[4]);
						int levelhas = levelCount(sVotes, level);
						if (musthave > 0) {
							if (levelhas < musthave) {
								request.veto("Insufficient votes", "According to workflow <b>" + wparts[0] + "</b>, the Pull Request must have <b>" + convert(musthave) + "</b> " + level + " vote(s)");
							} // did not have enough
						} // check for must have levels

						if (nothave > 0) {
							if (levelhas >= nothave) {
								request.veto("Invalid votes", "According to workflow <b>" + wparts[0] + "</b>, the Pull Request must not have <b>" + convert(nothave) + "</b> " + level + " vote(s)");
							} // did not have enough
						} // check for not have levels
					} // all possible restrictions

					break;
				} // found the project
			} // all workflows

		} // has workflows - need to check rules
		else {
			log.debug("No workflows, not checking any rules for merge request");
		} // no workflows
	} // check
	
	private String getVote(String key) {
		//pkey + '/' + pr.getToRef().getRepository().getSlug() + '/' + pr.getId()
		JSONArray jsonArray = new JSONArray();
		for (Vote vote : ao.find(Vote.class, Query.select().where("PULL_REQ_KEY = ?", key))) {
			JSONObject jsonobj = new JSONObject();
			jsonobj.put("id", vote.getID());
			jsonobj.put("pullReqKey", vote.getPullReqKey());
			jsonobj.put("author", vote.getUsername());
			jsonobj.put("text", vote.getVote());
			jsonobj.put("created", vote.getTime());
			jsonArray.add(jsonobj);
		}
		
		return jsonArray.toString();
	}

	private int safeToInt(String s) {
		try {
			int i = Integer.parseInt(s);
			if (i < 0) {
				log.warn("Invalid number stored in workflow: " + s);
				return 0;
			} else {
				return i;
			}
		} catch (NumberFormatException nfe) {
			log.warn("Invalid number stored in workflow: " + s);
			return 0;
		}
	}

	private int levelCount(String votes, String level) {
		String l = level.replace("+", "\\+");
		l = l.replace("-", "\\-");
		Pattern p = Pattern.compile("\"text\":\\s*\"" + l + "\"");
		Matcher m = p.matcher(votes);

		int cnt = 0;
		while (m.find()) {
			cnt++;
		}

		return cnt;
	}

	private boolean hasProject(String[] plist, String pkey) {
		boolean has = false;
		for (String p : plist) {
			if (p.equalsIgnoreCase(pkey)) {
				has = true;
				break;
			}
		}

		return has;
	}

	/**
	 * Method to convert an number (e.g. 1) into the text value (e.g. one)
	 * Taken from http://code.activestate.com/recipes/577312-number-to-words-converter-100-one-hundred/
	 * @param i Number to convert to text
	 * @return
	 */
	public static String convert(Integer i) {
		//
		if( i < 20)  return units[i];
		if( i < 100) return tens[i/10] + ((i % 10 > 0)? " " + convert(i % 10):"");
		if( i < 1000) return units[i/100] + " Hundred" + ((i % 100 > 0)?" and " + convert(i % 100):"");
		if( i < 1000000) return convert(i / 1000) + " Thousand " + ((i % 1000 > 0)? " " + convert(i % 1000):"") ;
		return convert(i / 1000000) + " Million " + ((i % 1000000 > 0)? " " + convert(i % 1000000):"") ;
	}
}
