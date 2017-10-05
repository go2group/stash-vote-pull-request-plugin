AJS.toInit(function() {
	var baseUrl = AJS.$("input[name='application-base-url']").attr("value");

    function populateForm() {
        AJS.$.ajax({
			async: false,
			cache: false,
            url: baseUrl + "/rest/pullReqVoteConfig-admin/1.0/",
            dataType: "json",
            success: function(config) {
				AJS.$('#workflows').find("tr:gt(0)").remove();
				
                AJS.$("#projects").attr("value", config.projects);

                var roleOpts = ['ADMIN', 'LICENSED_USER', 'PROJECT_ADMIN', 'PROJECT_CREATE', 'PROJECT_READ', 'PROJECT_VIEW', 'PROJECT_WRITE', 'REPO_ADMIN', 'REPO_READ', 'REPO_WRITE', 'SYS_ADMIN'];
                var roleVals = ['Administrator', 'Any licensed user', 'Project administrator', 'Project creator', 'Read access', 'View access', 'Write access', 'Repository administrator', 'Repository read access', 'Repository write access', 'System administrator'];
                var workflows = config.workflows.split(',');

                for(var ii=0; ii< workflows.length; ii++) {
                    if(workflows[ii] && workflows[ii].length > 0) {
                        var elem = workflows[ii].split("|");

                        var plist = config.projects.split(",");
                        var poptions = [];
                        var projects = elem[1].split("~");
                        for(var jj=0; jj< plist.length; jj++) {
                            if(plist[jj].length > 0) {
                                poptions.push('<option value="' + plist[jj] + '" ' + (projects.indexOf(plist[jj]) > -1 ? "selected" : "") + ' >' + plist[jj] + '</option>');
                            }
                        }

						var rawhtml = '<tr wfname="' + elem[0] + '"><td width="15%"><input type="text" class="text" id="wfname" name="wfname" value="' + elem[0] + '"></td>';
						rawhtml = rawhtml + '<td width="15%"><select multiple name="wfproj" id="wfproj" class="multi-select">' + poptions.join('') + '</select></td>';

                        rawhtml = rawhtml + '<td width="60%"><table class="box-style" width="100%" name="aworkflow" id="aworkflow"><tr><th>Voting level</th><th>Allowed Roles</th><th>Allowed Groups/Users</th><th>Must have at least</th><th>Must not have at least</th></tr>';
                        var wfentries = elem[2].split("~");
                        for(var jj=0; jj< wfentries.length; jj++) {
                            if(wfentries[jj] && wfentries[jj].length > 0) {
                                var wfrow = wfentries[jj].split("^");
                                rawhtml = rawhtml + '<tr style="vertical-align:top;"><td><input type="number" class="text" value="' + wfrow[0] + '" name="wflevel" id="wflevel"></td>';
                                rawhtml = rawhtml + '<td><select multiple class="multi-select" name="wfroles" id="wfroles">';
                                var selectedRoles = wfrow[1].split("=");
                                for(var kk=0; kk < roleOpts.length; kk++) {
                                    rawhtml = rawhtml + '<option value="' + roleOpts[kk] + '" ' + (selectedRoles.indexOf(roleOpts[kk]) > -1 ? "selected" : "") + ' >' + roleVals[kk] + '</option>';
                                }
                                rawhtml = rawhtml + '</select></td>';
                                rawhtml = rawhtml + '<td><input type="text" class="text" name="wfgroups" id="wfgroups" value="' + wfrow[2] + '"/></td>';
                                rawhtml = rawhtml + '<td><input type="number" class="text" value="' + wfrow[3] + '" name="wfmusthave" id="wfmusthave"></td>';
                                rawhtml = rawhtml + '<td><input type="number" class="text" value="' + wfrow[4] + '" name="wfmustnothave" id="wfmustnothave"></td>';
                                rawhtml = rawhtml + '</tr>';
                            }
                        }
                        rawhtml = rawhtml + '</table><br><br><input type="button" value="Add Level" class="button" onclick="addrow(this);"></td><td width="10%"><button id="delete-workflow" wfname="' + elem[0] + '" class="aui-button">Delete Workflow</button></td></tr>';
                        AJS.$('#workflows').append(rawhtml);
                    } // has data
                } // all workflow entries

                AJS.$('button').click(function(e) {
					e.preventDefault();
					var wfname = AJS.$(this).attr('wfname');
					deleteWf(wfname);
				});
            }
        });
    }

    function addInitWorkflow() {
		addWorkflow();
		// Set first WF Name
		AJS.$('#wfname').attr('value', 'Default Workflow');
		// Get the init workflow button
		var button = AJS.$('input[type="button"][value="Add Level"]').get(0);
		addrow(button);
		addrow(button);
		addrow(button);
		addrow(button);
		
		// Set WF Levels
		var initWFLevels = AJS.$('input[name="wflevel"]');
		AJS.$(initWFLevels[0]).attr('value', '-2');
		AJS.$(initWFLevels[1]).attr('value', '-1');
		AJS.$(initWFLevels[2]).attr('value', '1');
		AJS.$(initWFLevels[3]).attr('value', '2');
		
		// Set WF Permission
		AJS.$('select[name="wfroles"]').each(function() {
			AJS.$(this).children().each(function() {
				var initWFRolesValue = AJS.$(this).attr('value');
				if (initWFRolesValue === 'PROJECT_VIEW') {
					AJS.$(this).attr('selected', 'selected');
				}
			});
		});
		
		// Set WF Must Have
		var initWFMustHave = AJS.$('input[name="wfmusthave"]');
		AJS.$(initWFMustHave[0]).attr('value', '0');
		AJS.$(initWFMustHave[1]).attr('value', '0');
		AJS.$(initWFMustHave[2]).attr('value', '1');
		AJS.$(initWFMustHave[3]).attr('value', '1');
		
		// Set WF Must Not Have
		var initWFMustHave = AJS.$('input[name="wfmustnothave"]');
		AJS.$(initWFMustHave[0]).attr('value', '1');
		AJS.$(initWFMustHave[1]).attr('value', '1');
		AJS.$(initWFMustHave[2]).attr('value', '0');
		AJS.$(initWFMustHave[3]).attr('value', '0');
		
		updateConfig(true);
	}

	function getConfig() {
        var wlist = '';
        var rows = AJS.$("#workflows tr");
        rows.each(function(index) {
            var wrow = '';
            var wname = AJS.$(this).find('input[name="wfname"]').val();
            if(wname && wname.length > 0) {
                wrow = wrow + wname + '|';
                var wproj = AJS.$(this).find('select[name="wfproj"]').val() || [];
                wrow = wrow + wproj.join('~') + '|';
                var wtable = AJS.$(this).find('tr');
                wtable.each(function(idx) {
                    var wcell = '';
                    var wlevel = AJS.$(this).find('input[name="wflevel"]').val();
                    if(wlevel && wlevel.length > 0) {
                        wcell = wcell + wlevel + '^';
                        var wroles = AJS.$(this).find('select[name="wfroles"]').val() || [];
                        wcell = wcell + wroles.join('=') + '^';
                        var wmg = AJS.$(this).find('input[name="wfgroups"]').val();
                        wcell = wcell + wmg + '^';
                        var wmh = AJS.$(this).find('input[name="wfmusthave"]').val();
                        wcell = wcell + wmh + '^';
                        var wmnh = AJS.$(this).find('input[name="wfmustnothave"]').val();
                        wcell = wcell + wmnh;
                        wrow = wrow + wcell + '~';
                    }
                });
                wlist = wlist + wrow + ',';
            }
        });
        
		return wlist;
	}
	
	function repopulateAfterDeleteOrUpdate(flag) {
		if (flag || flag == null || flag == undefined) {
			location.reload();
		}
	}
	
	function deleteWf(wfname) {
		AJS.$.ajax({
			async: false,
			url: baseUrl + "/rest/pullReqVoteConfig-admin/1.0/",
			type: "DELETE",
			contentType: "application/json",
			data: '{ "workflows": "' + getConfig() + '","delete":"' + wfname + '"}',
			processData: false,
			success: repopulateAfterDeleteOrUpdate(true)
		});
	}

	function updateConfig(flag) {
        AJS.$.ajax({
			async: false,
            url: baseUrl + "/rest/pullReqVoteConfig-admin/1.0/",
            type: "PUT",
            contentType: "application/json",
			data: '{ "workflows": "' + getConfig() + '" }',
			processData: false,
			success: repopulateAfterDeleteOrUpdate(flag)
        });
    }

    populateForm();

	AJS.$.ajax({
		async: false,
		cache: false,
		url: baseUrl + "/rest/pullReqVoteConfig-admin/1.0/",
		dataType: "json",
		success: function(config) {
			var workflows = config.workflows.split(',');
			var isItReallyEmpty = workflows[0];
			if (workflows.length === 1 && isItReallyEmpty == '') {
				addInitWorkflow();
			}
		}
	});
	
    AJS.$("#admin").submit(function(e) {
        e.preventDefault();
        updateConfig();
    });
	
	AJS.InlineDialog(AJS.$("#header-wfname"), "header-wfname", function(content, trigger, showPopup) {
		content.css({
			"padding" 	: "20px",
			"width"		: "200px",
			"justify-content" : "space-between"
		}).html(AJS.$('#header-wfname-content').html().trim());
		showPopup();
		return false;
	});
	
	AJS.InlineDialog(AJS.$("#header-projects"), "header-projects", function(content, trigger, showPopup) {
		content.css({
			"padding" 	: "20px",
			"justify-content" : "space-between"
		}).html(AJS.$('#header-projects-content').html().trim());
		showPopup();
		return false;
	});
	
	AJS.InlineDialog(AJS.$("#header-config"), "header-config", function(content, trigger, showPopup) {
		content.css({
			"padding"	: "20px",
			"width"		: "600px",
			"justify-content" : "space-between"
		}).html(AJS.$('#header-config-content').html().trim());
		showPopup();
		return false;
	});
});
