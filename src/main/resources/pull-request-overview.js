(function($) {
    // Set up our namespace
    window.Go2Group = window.Go2Group || {};
    Go2Group.Vote = Go2Group.Vote || {};

    // Deal with the nitty-gritty of localStorage
    function storageKey(pullRequestJson) {
        var repo = pullRequestJson.toRef.repository;
        var proj = repo.project;
        return 'go2group.vote.pullrequest.' + proj.key + '/' + repo.slug + '/' + pullRequestJson.id;
    }
    function projKey(pullRequestJson) {
        var repo = pullRequestJson.toRef.repository;
        var proj = repo.project;
        return proj.key;
    }
    function repoKey(pullRequestJson) {
        var repo = pullRequestJson.toRef.repository;
        return repo.slug;
    }
    var storage = window.localStorage ? {
        getVotes : function(pullRequestJson) {

            //var item = localStorage.getItem(storageKey(pullRequestJson));
            var key = storageKey(pullRequestJson);
            var item = '';
            AJS.$.ajax({
                url: AJS.contextPath() + "/plugins/servlet/pullReqVote?op=get&key=" + encodeURIComponent(key) + "&jsondata=foo", 
                type: "get",
                dataType: "json",
                async: false, 
                success: function(data) { item = data; console.log("Got " + JSON.stringify(data) + " into " + JSON.stringify(item)); }
            });
            if(item != '') {
                try {
                    var parsed = JSON.parse(JSON.stringify(item)) || [];
                    parsed.sort(function(a,b) {
                        var d1 = a.id;
                        var d2 = b.id;
                        return (d1 < d2) ? 1 : (d1 > d2) ? -1 : 0;
                    });
                    console.log("Returning " + item + " as " + parsed);
                    return (parsed);
                } catch(e) {
                    console.log(JSON.stringify(e) + " - " + e.message);
                    return [];
                }
            }
            else {
                console.log("Got nothing back from servlet");
                return [];
            }
        },
        putVotes : function(pullRequestJson, votes) {
            var key = storageKey(pullRequestJson);
            var jsondata = JSON.stringify(votes);
            console.log("Storing " + jsondata);
            AJS.$.ajax({
                url: AJS.contextPath() + "/plugins/servlet/pullReqVote?op=put&key=" + encodeURIComponent(key) + "&jsondata=" + encodeURIComponent(jsondata), 
                type: "get",
                dataType: "json",
                async: false, 
                success: function(data) { }
            });
            //localStorage.setItem(storageKey(pullRequestJson), JSON.stringify(votes));
        },
        removeVotes : function(pullRequestJson, voteId) {
			var key = storageKey(pullRequestJson);
			//var jsondata = JSON.stringify(votes);
			AJS.$.ajax({
				url : AJS.contextPath() + "/plugins/servlet/pullReqVote?op=remove&key=" + encodeURIComponent(key) + "&voteId=" + voteId,
				type : "get",
				dataType : "json",
				async : false,
				success : function(data) {
				}
			});
		},
        getLevels : function(pullRequestJson) {
        	console.log('Timothy was here!~!');
            var item = '';
            var pkey = projKey(pullRequestJson);
            var rkey = repoKey(pullRequestJson);
            AJS.$.ajax({
                url: AJS.contextPath() + "/plugins/servlet/pullReqVote?op=levels&key=foo&jsondata=foo&pkey=" + encodeURIComponent(pkey) + "&rkey=" + encodeURIComponent(rkey),
                type: "get",
                dataType: "json",
                async: false, 
                success: function(data) { item = data; console.log("Got " + JSON.stringify(data) + " into " + JSON.stringify(item)); }
            });
            if(item != '') {
                try {
                    var parsed = JSON.parse(JSON.stringify(item)) || [];
                    console.log("Returning " + item + " as " + parsed);
                    return (parsed);
                } catch(e) {
                    console.log(JSON.stringify(e) + " - " + e.message);
                    return [];
                }
            }
            else {
                console.log("Got no levels back from servlet");
                return [];
            }
        }
    } : {
        getVotes : function() {},
        getLevels : function() {},
        putVotes : function() {}
    };

    // Stash 2.4.x and 2.5.x incorrectly provided a Brace/Backbone model here, but should have provided raw JSON.
    function coerceToJson(pullRequestOrJson) {
        return pullRequestOrJson.toJSON ? pullRequestOrJson.toJSON() : pullRequestOrJson;
    }

    /**
     * The client-condition function takes in the context
     * before it is transformed by the client-context-provider.
     * If it returns a truthy value, the panel will be displayed.
     */
    function hasAnyVotes(context) {
        var votes = storage.getVotes(coerceToJson(context['pullRequest']));
        return votes.length;
    }

    /**
     * The client-context-provider function takes in context and transforms
     * it to match the shape our template requires.
     */
    function getVoteStats(context) {
        var votes = storage.getVotes(coerceToJson(context['pullRequest']));
		var sum = 0;
		for (var i = 0, len = votes.length; i < len; i++) {
			var nextVal = votes[i].text;
			sum += parseInt(votes[i].text, 10);
		}
		
        return {
			count : votes.length,
			sum : sum
        };
    }

    function getLevels(context) {
        var jsondata = coerceToJson(context['pullRequest']);
        return {
            levels: storage.getLevels(jsondata)
        }
    }

    function addVote(pullRequestJson, author, vote) {
        var votes = storage.getVotes(pullRequestJson);
        votes.push({
			//id : new Date().getTime() + ":" + Math.random(),
			author : author,
            text : vote,
            created : new Date().toLocaleString()
        });
        storage.putVotes(pullRequestJson, votes);
    }

    function removeVote(pullRequestJson, voteId) {
        var votes = storage.getVotes(pullRequestJson).filter(function(vote) {
            return vote.id != voteId;
        });
		storage.removeVotes(pullRequestJson, voteId);
    }


    /* Expose the client-condition function */
    Go2Group.Vote._pullRequestIsOpen = function(context) {
        var pr = coerceToJson(context['pullRequest']);
        return pr.state === 'OPEN';
    };

    /* Expose the client-context-provider function */
    Go2Group.Vote.getVoteStats = getVoteStats;
    Go2Group.Vote.getLevels = getLevels;

    Go2Group.Vote.addVote = addVote;

    Go2Group.Vote.removeVote = removeVote;

    function showDialog(votes) {
        var dialog = showDialog._dialog;
        if (!dialog) {
            dialog = showDialog._dialog = new AJS.Dialog()
                .addHeader("Votes")
                .addPanel("Votes")
                .addCancel("Close", function() {
                    dialog.hide();
                });
        }

		// If there are votes, we need to check whether the pull request is closed to hide the remove button
		var remove = false;
		if (votes.length > 0) {
			var pr = require('bitbucket/internal/model/page-state').getPullRequest();
			var prState = pr.attributes.state;
			if (prState !== "MERGED") {
				remove = true;
			}
		}

		dialog.getCurrentPanel().body.html(com.go2group.vote.voteList({
			votes : votes,	
			remove : remove
		}));
        dialog.show().updateHeight();
    }

    function renderVotesLink() {
        var pr = require('bitbucket/internal/model/page-state').getPullRequest();
        var newStats = Go2Group.Vote.getVoteStats({ pullRequest : pr.toJSON() });
        AJS.$('.go2group-votes-link').replaceWith(com.go2group.vote.prOverviewPanel(newStats));

        var levels = Go2Group.Vote.getLevels({ pullRequest : pr.toJSON() });
        AJS.$('.create-vote').replaceWith(com.go2group.vote.voteForm({levels: levels}));
    }

    /* use a live event to handle the link being clicked. */
    AJS.$(document).on('click', '.go2group-votes-link', function(e) {
        e.preventDefault();

        var pr = require('bitbucket/internal/model/page-state').getPullRequest();

        showDialog(storage.getVotes(pr.toJSON()));
    });

    AJS.$(document).on('submit', "#create-vote", function(e) {
        //e.preventDefault();
        var pr = require('bitbucket/internal/model/page-state').getPullRequest();

        var $input = AJS.$(this).find("select");
        var text = $input.val();

        var cuser = require('bitbucket/internal/model/page-state').getCurrentUser().id;

        Go2Group.Vote.addVote(pr.toJSON(), cuser, text);
        renderVotesLink();
    });

    AJS.$(document).on('click', '.remove', function(e) {
        e.preventDefault();
        var voteId = $(this).attr('data-vote-id');

        var prJSON = require('bitbucket/internal/model/page-state').getPullRequest().toJSON();

        Go2Group.Vote.removeVote(prJSON, voteId);

        showDialog(storage.getVotes(prJSON));
        renderVotesLink();
    })
}(AJS.$));
