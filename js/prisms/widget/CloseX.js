
__dojo.provide("prisms.widget.CloseX");
__dojo.declare("prisms.widget.CloseX", [__dijit._Widget, __dijit._Contained], {

	blurredSrc: "__webContentRoot/rsrc/icons/windowsXBlur.png",
	blurredSrcAlt: "__webContentRoot/rsrc/icons/windowsXSmall.png",

	focusedSrc: "__webContentRoot/rsrc/icons/windowsXFocus.png",

	useSmall: false,

	postCreate: function(){
		this.inherited(arguments);
		if(this.useSmall)
			this.blurredSrc=this.blurredSrcAlt;
		this.domNode.style.position="absolute";
		this.img=document.createElement("img");
		this.img.src=this.blurredSrc;
		this.domNode.appendChild(this.img);
		__dojo.connect(this.img, "onmouseenter", this, this._mouseEnter);
		__dojo.connect(this.img, "onmouseout", this, this._mouseOut);
		__dojo.connect(this.img, "onmousedown", this, this._mouseDown);
		__dojo.connect(this.img, "onmouseup", this, this._mouseUp);
	},

	_mouseEnter: function(){
		this.img.src=this.focusedSrc;
	},

	_mouseOut: function(){
		this.img.src=this.blurredSrc;
		this.isPressed=false;
	},

	_mouseDown: function(){
		this.isPressed=true;
	},

	_mouseUp: function(){
		if(this.isPressed)
			this.onClick();
		this.isPressed=false;
	},

	onClick: function(){
	}
});
