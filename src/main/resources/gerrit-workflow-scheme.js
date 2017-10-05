AJS.toInit(function() {
	var baseUrl = AJS.$("input[name='application-base-url']").attr("value");
	
	function populateTable() {
		//AJS.$('#workflows').find("tr:gt(0)").remove();
		AJS.$.ajax({
			async: false,
			url: baseUrl + "/rest/pullReqVoteConfig-admin/1.0/",
			dataType: "json",
			success: function(config) {
				var workflows = config.workflows.split(',');
				for(var ii=0; ii < workflows.length - 1; ii++) {
					var workflow = workflows[ii];
					var elem = workflows[ii].split("|");
					
					var workflowRow = '<tr><td><b>' + elem[0] + '</b></td><td><a href="#" id="associatedProj" name="associatedProj" wfname="' + elem[0] + '">Associated Projects</a></td><td><a href="' + baseUrl + '/plugins/servlet/pullReqVoteConfig">Edit Workflow</a></td></tr>';
					AJS.$('#workflows > tbody').append(workflowRow);
				}
			}
		});
	}
	
	function saveWfAssociation(wfname, selectedPrj) {
		console.log(wfname);
		console.log(selectedPrj);
	}
	
	populateTable();
	
	AJS.$('a[name="associatedProj"]').click(function(e) {
		e.preventDefault();
		
		var allProjects = AJS.$.ajax({
			async:false,
			url: baseUrl + "/rest/api/1.0/projects?limit=1000",
			dataType: "json",
			success: function(projects) {
				return projects.values;
			}
		});
		//console.log(allProjects.responseJSON);
		var options = '';
		
		for (var i = 0; i < allProjects.responseJSON.values.length; i++) {
			var project = allProjects.responseJSON.values[i];
			//console.log(project);
			options = options + '<option value="' + project.key + '">' + project.name + '</option>';
		}
		//console.log(options);
		
		var wfname = AJS.$(this).attr('wfname');
		
		var dialog = new AJS.Dialog({
		    width: 800, 
		    height: 500, 
		    id: "associatedProjDialog", 
		    closeOnOutsideClick: true
		});
		dialog.addHeader("Associaetd Projects for " + wfname);
		var panelContent = '<p>This workflow is associated to the following projects:</p>';
		panelContent += '<form class="aui">' +
				'<div class="field-group">' +
					'<label for="projects">Projects <span class="aui-icon icon-required">(required)</span></label>' +
					'<select class="select" id="select-example" name="select-example" size="10" multiple="multiple">' +
						options +
					'</select>' +
				'</div>' +
			'</form>';
		dialog.addPanel("Mail Panel", panelContent, "panel-body");
		dialog.addSubmit("Save", function(){
			var selectedPrj = [];
			AJS.$('#select-example :selected').each(
				function(i, selected) {
					selectedPrj[i] = AJS.$(selected).text();
				}
			);

			saveWfAssociation(wfname, selectedPrj);
		});
		dialog.addLink("Cancel", function (dialog) {
		    dialog.hide();
		}, "#");
		
		dialog.show();
	});
});