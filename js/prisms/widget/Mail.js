
__dojo.require("dijit.Toolbar");
__dojo.require("dijit.form.Button");
__dojo.require("dijit.form.TextBox");

__dojo.require("prisms.widget.FillPane");
__dojo.require("prisms.widget.SortTable");
__dojo.require("prisms.widget.TabWidget");
__dojo.require("prisms.widget.MessageComposer");

__dojo.provide("prisms.widget.Mail");
__dojo.declare("prisms.widget.Mail", [prisms.widget.TabWidget, __dijit._Templated, __dijit._Container], {

	prisms: null,

	pluginName: "No pluginName specified",

	templatePath: "__webContentRoot/view/prisms/templates/mail.html",
	
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
		if(event.method=="setMessages")
		{
			if(event.data.metadata.total==0)
			{
				this.sortTable.clear();
				PrismsUtils.setTableRowVisible(this.noMailRow, true);
				PrismsUtils.setTableRowVisible(this.mailTableRow, false);
			}
			else
			{
//				this.sortTable.setData(event.data);
				PrismsUtils.setTableRowVisible(this.noMailRow, false);
				PrismsUtils.setTableRowVisible(this.mailTableRow, true);
			}
		}
		else if(event.method=="setMessageCount")
			this.setMessageCount(event);
		else if(event.method=="setSearchError")
			this.setSearchError(event.error);
		else
			throw new Error("Unrecognized "+this.pluginName +" method: " + event.method);
	},

	shutdown: function(){
		this.sortTable.clear();
		this.inboxButton.setLabel("Inbox");
		this.draftsButton.setLabel("Drafts");
	},

	setMessageCount: function(event){
		if(event.inboxCount)
			this.inboxButton.setLabel("Inbox ("+event.inboxCount+")");
		else
			this.inboxButton.setLabel("Inbox");
		if(event.draftCount)
			this.draftsButton.setLabel("Drafts ("+event.draftCount+")");
		else
			this.draftsButton.setLabel("Drafts");
	},

	setSearchError: function(error){
		this.searchBox.style.backgroundColor=(error ? "red" : "white");
	},

	_inboxClicked: function(){
		this.prisms.callApp(this.pluginName, "goToBox", {box: "inbox"});
	},

	_sentMailClicked: function(){
		this.prisms.callApp(this.pluginName, "goToBox", {box: "sent"});
	},

	_draftsClicked: function(){
		this.prisms.callApp(this.pluginName, "goToBox", {box: "drafts"});
	},

	_allMailClicked: function(){
		this.prisms.callApp(this.pluginName, "goToBox", {box: "all"});
	},

	_trashClicked: function(){
		this.prisms.callApp(this.pluginName, "goToBox", {box: "trash"});
	},

	_searchTyped: function(event){
		var keyCode=event.keyCode;
		if(keyCode==__dojo.keys.ENTER)
			this._searchClicked();
	},

	_searchClicked: function(){
		var searchText=this.searchBox.value;
		if(searchText=="")
			this._allMail();
		else
			this.prisms.callApp(this.pluginName, "search", {searchText: searchText});
	},

	_archiveClicked: function(){
		this.prisms.callApp(this.pluginName, "archive");
	},

	_deleteClicked: function(){
		this.prisms.callApp(this.pluginName, "delete");
	},

	_markAsReadClicked: function(){
		this.prisms.callApp(this.pluginName, "markAsRead");
	},

	_refreshClicked: function(){
		this.prisms.callApp(this.pluginName, "refresh");
	},

	_composeClicked: function(){
		this.prisms.callApp(this.pluginName, "compose");
	},

	_sendClicked: function(){
	},

	_saveClicked: function(){
	},

	_discardClicked: function(){
	},

	_sortBy: function(columnLabel, ascending){
		//this.prisms.callApp(this.pluginName, "setSort", {column: columnLabel, ascending: ascending});
		//No table sorting for mail
	},

	_goTo: function(linkID){
		this.prisms.callApp(this.pluginName, "goToLink", {id: linkID});
	},

	_navTo: function(start){
		this.prisms.callApp(this.pluginName, "navigate", {start: start});
	},

	_selectChanged: function(start, end, selected){
		this.prisms.callApp(this.pluginName, "setSelected", {start: start, end: end, selected: selected});
	}
});
