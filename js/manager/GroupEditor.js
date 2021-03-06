
__dojo.require("dijit.form.TextBox");

__dojo.provide("manager.GroupEditor");
__dojo.declare("manager.GroupEditor", [__dijit._Widget, __dijit._Contained, __dijit._Templated, __dijit._Container],
{
	templatePath: "__webContentRoot/view/manager/templates/groupEditor.html",

	widgetsInTemplate: true,

	prisms: null,

	pluginName: "No pluginName specified",

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
		this.prisms.loadPlugin(this);
	},

	processEvent: function(event){
		if(event.method=="setVisible")
			this.setVisible(event.visible);
		else if(event.method=="setEnabled")
			this.setEnabled(event.enabled);
		else if(event.method=="setValue")
			this.setValue(event.value);
		else
			throw new Error("event "+event.plugin+"."+event.method+" not recognized");
	},

	shutdown: function(){
		this.setVisible(false);
	},

	setVisible: function(visible){
		PrismsUtils.setTableVisible(this.domNode, visible);
	},

	setEnabled: function(enabled){
		var self=this;
		//TODO: This is important for subtabs later
//		if(!this.initialized)
//		{
//			this.taskEditorTimeoutCount=0;
//			this.initTaskEditorTimeout=setTimeout(function(){
//				var tabs=__dijit.byId("managerUserTabContainer");
//				var taskTab=__dijit.byId("managerUserEditorTab");
//				var doClear=false;
//				if(tabs && taskTab)
//				{
//					tabs.selectChild(taskTab);
//					doClear=true;
//				}
//				self.taskEditorTimeoutCount++;
//				doClear|=self.taskEditorTimeoutCount>=10;
//				if(doClear)
//				{
//					clearTimeout(self.initTaskEditorTimeout);
//					delete self.initTaskEditorTimeout;
//					delete self.taskEditorTimeoutCount;
//					self.initialized=true;
//				}
//			}, 250);
//		}
		this.nameField.setAttribute("disabled", !enabled);
		this.descripField.disabled=!enabled;
	},

	setValue: function(value){
		this.dataLock=true;
		this.value = value;
		try{
			if(this.nameField.getValue()!=value.name)
				this.nameField.setValue(value.name);
			if(this.descripField.value!=value.descrip)
				this.descripField.value=value.descrip;
		} finally{
			this.dataLock=false;
		}
	},

	_nameChanged: function(){
		if(this.dataLock)
			return;
		var newName=this.nameField.getValue();
		this.prisms.callApp(this.pluginName, "nameChanged", {name: newName});
	},

	_descripChanged: function(){
		if(this.dataLock)
			return;
		var newDescrip=this.descripField.value;
		this.prisms.callApp(this.pluginName, "descripChanged", {descrip: newDescrip});
	}
});
