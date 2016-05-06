Ext.define('PBPcm.controller.file.Main', {
    extend: 'Ext.app.Controller',
    
	refs:[{
	    ref: 'main',
	    selector:'pcmReqMain'
	},{
	    ref: 'cmbMp',
	    selector:'pcmReqFileTab uploadGrid field[name=mp]'
	}],
	
	init:function() {
		var me = this;
		
		me.control({
			'grid [action=download]': {
				click : me.download
			},
			'combo': {
				selectMiddlePrice : me.selectMiddlePrice
			}
		});
	
	},

	MSG_KEY : 'DELETE_REQ_DETAIL',
	URL : ALF_CONTEXT+'/pcm/req/dtl',
	ADMIN_URL : ALF_CONTEXT+'/admin/pcm/dtl',
	MSG_URL : ALF_CONTEXT+'/pcm/message',
	
	download:function() {
		var me = this;

		var nodeRef = me.rec.data.FLAG1;
		var pos = nodeRef.lastIndexOf("/");
		var id = nodeRef.substring(pos+1);
		window.open(Alfresco.constants.PROXY_URI_RELATIVE+"api/node/content/workspace/SpacesStore/"+id+"/a.odt?a=true");
	},
	
	selectMiddlePrice:function(cmb, rec) {
		this.rec = rec[0].data;
	}

});