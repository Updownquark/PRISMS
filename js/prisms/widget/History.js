
__dojo.require("dijit.form.Button");
__dojo.require("dijit.form.NumberSpinner");

__dojo.require("prisms.widget.TabWidget");
__dojo.require("prisms.widget.SortTable");
__dojo.require("prisms.widget.TimeAmountEditor");

__dojo.provide("prisms.widget.History");
__dojo.declare("prisms.widget.History", [prisms.widget.TabWidget, __dijit._Templated, __dijit._Container], {

	prisms: null,

	pluginName: "No pluginName specified",

	templatePath: "__webContentRoot/view/prisms/templates/history.html",
	
	widgetsInTemplate: true,

	visible: true,

	postCreate: function(){
		this.inherited("postCreate", arguments);

		if(!this.prisms)
		{
			var prisms=PrismsUtils.getPrisms(this);
			if(prisms)
				this.setPrisms(prisms);
			else
				console.error("No prisms parent for plugin "+this.pluginName);
		}
		this.ageEditor.setValue({months: 0, seconds: 30*24*60*60});
	},

	setPrisms: function(prisms){
		this.prisms=prisms;
		this.sortTable.prisms=this.prisms;
		this.prisms.loadPlugin(this);
		if(!this.visible)
			this.setVisible(false);
		delete this["visible"];
	},

	processEvent: function(event){
		if(event.method=="hide")
			this.setVisible(false);
		else if(event.method=="setItem")
			this.setItem(event.item);
		else if(event.method=="setSnapshotTime")
			this.setSnapshot(event.time);
		else if(event.method=="setPurgeEnabled")
			this.setPurgeEnabled(event.manualEnabled, event.autoEnabled);
		else if(event.method=="setAutoPurge")
			this.setAutoPurge(event.autoPurge);
		else if(event.method=="setContent")
			this.setContent(event.content, event.show);
		else if(event.method=="setFilter")
			this.setFilter(event.filter);
		else
			throw new Error("Unrecognized "+this.pluginName +" method: " + event.method);
	},

	shutdown: function(){
		this.setItem(null);
		this.setSnapshot("");
		this.sortTable.clear();
		this.setAutoPurge({
			allUsers: [],
			allTypes: []
		});
	},

	setPurgeEnabled: function(manual, auto){
		this.manualPurgeEnabled=manual;
		this.autoPurgeEnabled=auto;
		if(this.autoPurge)
		{
			this.entryCountCheck.disabled=!auto;
			this.entryCountEditor.setAttribute("disabled", !(auto && typeof this.autoPurge.entryCount=="number"));
			this.ageCheck.disabled=!auto;
			this.ageEditor.setDisabled(!(auto && typeof this.autoPurge.age=="number"));
		}
		else
		{
			this.entryCountCheck.disabled=true;
			this.entryCountEditor.setAttribute("disabled", true);
			this.ageCheck.disabled=true;
			this.ageEditor.setDisabled(true);
		}
		this.excludeUsers.disabled=!auto;
		if (this.excludeTypes != null && typeof this.excludeTypes == "object")
			this.excludeTypes.disabled=!auto;
		this.autoPurgeButton.setAttribute("disabled", !auto);
		this.purgeSelected.domNode.style.visibility=manual ? "visible" : "hidden";
	},

	setAutoPurge: function(purge){
		this.autoPurgeNotificationCell.style.visibility="hidden";
		this.dataLock=true;
		try{
		
		this.autoPurge=purge;
		if(this.autoPurge)
		{
			this.entryCountEditor.setAttribute("disabled",
				!(this.autoPurgeEnabled && typeof this.autoPurge.entryCount=="number"));
			this.ageEditor.setDisabled(!(this.autoPurgeEnabled && typeof this.autoPurge.age=="number"));
		}
		else
		{
			this.entryCountCheck.disabled=true;
			this.entryCountEditor.setAttribute("disabled", true);
			this.ageCheck.disabled=true;
			this.ageEditor.setDisabled(true);
		}
		if(typeof purge.entryCount=="number")
		{
			this.entryCountCheck.checked=true;
			if(this.entryCountEditor.getValue()!=purge.entryCount)
				this.entryCountEditor.setValue(purge.entryCount);
		}
		else
			this.entryCountCheck.checked=false;
		if(typeof purge.age=="number")
		{
			this.ageCheck.checked=true;
			var age=this.ageEditor.getValue();
			if(age.months!=0 || age.seconds!=purge.age)
				this.ageEditor.setValue({months: 0, seconds: purge.age});
		}
		else
			this.ageCheck.checked=false;
		while(this.excludeUsers.options.length>0)
			this.excludeUsers.remove(this.excludeUsers.options.length-1);
		for(var i=0;i<purge.allUsers.length;i++)
		{
			var option=document.createElement("option");
			option.text=purge.allUsers[i];
			for(var j=0;j<purge.excludeUsers.length;j++)
				if(purge.excludeUsers[j]==purge.allUsers[i])
				{
					option.selected=true;
					break;
				}
			if(__dojo.isIE > 6)
				this.excludeUsers.add(option);
			else
				this.excludeUsers.add(option, null);
		}
		
		if (this.excludeTypes != null && typeof this.excludeTypes == "object") {
			while(this.excludeTypes.options.length>0)
					this.excludeTypes.remove(this.excludeTypes.options.length-1);
			
			for(var i=0;i<purge.allTypes.length;i++)
			{
				var option=document.createElement("option");
				option.text=purge.allTypes[i];
				for(var j=0;j<purge.excludeTypes.length;j++)
					if(purge.excludeTypes[j]==purge.allTypes[i])
					{
						option.selected=true;
						break;
					}
				if(__dojo.isIE > 6)
					this.excludeTypes.add(option);
				else
					this.excludeTypes.add(option, null);
			}
		}	
		
		} finally{
			this.dataLock=false;
		}
	},

	setSnapshot: function(snapshot){
		this.snapshotTime.innerHTML=PrismsUtils.fixUnicodeString(snapshot);
	},

	setItem: function(item){
		if(item)
		{
			if(item.isUserActivity)
				this.titleHeader.innerHTML="Actions of "+PrismsUtils.fixUnicodeString(item.text);
			else if(item.isCenterActivity || item.isSyncRecordActivity)
			{
				if(item.isImport)
					this.titleHeader.innerHTML="Modifications imported from "+PrismsUtils.fixUnicodeString(item.text);
				else
					this.titleHeader.innerHTML="Modifications exported to "+PrismsUtils.fixUnicodeString(item.text);
			}
			else
				this.titleHeader.innerHTML="History Of "+PrismsUtils.fixUnicodeString(item.text);
			this.showAllHistoryDiv.style.display="block";
		}
		else
		{
			this.titleHeader.innerHTML=PrismsUtils.fixUnicodeString(this.pluginName);
			this.showAllHistoryDiv.style.display="none";
		}
	},

	setContent: function(content, show){
		var oldSelect=this.isSelected();
		this.setVisible(true);
		if(!show && !oldSelect)
			this.setSelected(false);
		this.sortTable.setData(content);
		if(show)
			this.setSelected(true, true);
	},

	_showAllHistory: function(){
		this.prisms.callApp(this.pluginName, "showAllHistory");
	},

	_refresh: function(){
		this.prisms.callApp(this.pluginName, "refresh");
	},

	_sortBy: function(columnLabel, ascending){
		this.prisms.callApp(this.pluginName, "setSort", {column: columnLabel, ascending: ascending});
	},

	_goTo: function(linkID){
		this.prisms.callApp(this.pluginName, "goToLink", {id: linkID});
	},

	_navTo: function(start){
		this.prisms.callApp(this.pluginName, "navigate", {start: start});
	},

	_selectChanged: function(start, end, selected){
		this.prisms.callApp(this.pluginName, "setSelected", {start: start, end: end, selected: selected});
	},

	_undoClicked: function(){
		this.prisms.callApp(this.pluginName, "undoSelected");
	},

	_purgeClicked: function(){
		this.prisms.callApp(this.pluginName, "purgeSelected");
	},

	_entryCountChanged: function(){
		if(this.dataLock)
			return;
		if(!this.autoPurgeEnabled)
			return;
		if(this.entryCountCheck.checked)
			this.autoPurge.entryCount=this.entryCountEditor.getValue();
		else
			this.autoPurge.entryCount=null;
		this.entryCountEditor.setAttribute("disabled",
				!(this.autoPurgeEnabled && typeof this.autoPurge.entryCount=="number"));
		this.autoPurgeNotificationCell.style.visibility="visible";
	},

	_ageChanged: function(){
		if(this.dataLock)
			return;
		if(!this.autoPurgeEnabled)
			return;
		if(this.ageCheck.checked)
			this.autoPurge.age=this.ageEditor.getValue().seconds;
		else
			this.autoPurge.age=null;
		this.ageEditor.setDisabled(!(this.autoPurgeEnabled && typeof this.autoPurge.age=="number"));
		this.autoPurgeNotificationCell.style.visibility="visible";
	},

	_excludeUsersChanged: function(index, selected){
		if(this.dataLock)
			return;
		if(typeof index=="number")
		{ // The prisms.widget.MultiSelect widget
			var name=this.excludeUsers.options[index].text;
			if(selected)
				this.autoPurge.excludeUsers.push(name);
			else
			{
				for(var i=0;i<this.autoPurge.excludeUsers.length;i++)
					if(this.autoPurge.excludeUsers[i]==name)
					{
						this.autoPurge.excludeUsers.splice(i, 1);
						break;
					}
			}
		}
		else
		{
			this.autoPurge.excludeUsers.splice(0, this.autoPurge.excludeUsers.length);
			for(var i=0;i<this.excludeUsers.options.length;i++)
				if(this.excludeUsers.options[i].selected)
					this.autoPurge.excludeUsers.push(this.excludeUsers.options[i].text);
		}
		this.autoPurgeNotificationCell.style.visibility="visible";
	},

	_excludeTypesChanged: function(index, selected){
		if(this.dataLock)
			return;
		
		if (this.excludeTypes == null || typeof this.excludeTypes != "object")
			 return;	
		 
		if(typeof index=="number")
		{ // The prisms.widget.MultiSelect widget
			var name=this.excludeTypes.options[index].text;
			if(selected)
				this.autoPurge.excludeTypes.push(name);
			else
			{
				for(var i=0;i<this.autoPurge.excludeTypes.length;i++)
					if(this.autoPurge.excludeTypes[i]==name)
					{
						this.autoPurge.excludeTypes.splice(i, 1);
						break;
					}
			}
		}
		else
		{
			this.autoPurge.excludeTypes.splice(0, this.autoPurge.excludeTypes.length);
			for(var i=0;i<this.excludeTypes.options.length;i++)
				if(this.excludeTypes.options[i].selected)
					this.autoPurge.excludeTypes.push(this.excludeTypes.options[i].text);
		}
		this.autoPurgeNotificationCell.style.visibility="visible";
	},

	_sendAutoPurge: function(){
		var ap={};
		ap.entryCount=this.autoPurge.entryCount;
		ap.age=this.autoPurge.age;
		ap.excludeUsers=this.autoPurge.excludeUsers;
		ap.excludeTypes=this.autoPurge.excludeTypes;
		this.prisms.callApp(this.pluginName, "setAutoPurge", {autoPurge: ap});
	}
});

