Ext.define('PBPcm.model.GridModel', {
    extend: 'Ext.data.Model',
    fields : [ {name : 'id'}
    		 , {name : 'req_by'}
    		 , {name : 'req_ou'}
    		 , {name : 'objective_type'}
    		 , {name : 'objective'}
    		 , {name : 'total'}
    		 , {name : 'workflow_ins_id'}
    		 , {name : 'created_time_show'}
    		 , {name : 'budget_cc'}
    		 , {name : 'budget_cc_name'}
    		 , {name : 'doc_ref'}
    		 , {name : 'folder_ref'}
    		 , {name : 'status'}
    		 , {name : 'wfStatus'}
    		 , {name : 'created_by'}
    		 , {name : 'updated_by'}
    		 , {name : 'updated_time'}
    		 , {name : 'overDue'}
    		 , {name : 'action'}
    ]
});