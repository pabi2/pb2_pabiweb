Ext.define('PBExpUse.model.GridModel', {
    extend: 'Ext.data.Model',
    fields : [ {name : 'id'}
    		 , {name : 'req_by'}
    		 , {name : 'req_ou'}
    		 , {name : 'objective_type'}
    		 , {name : 'objective'}
    		 , {name : 'total'}
    		 , {name : 'workflow_ins_id'}
    		 , {name : 'requested_time_show'}
    		 , {name : 'origin_pr_number'}
    		 , {name : 'is_reason'}
    		 , {name : 'is_small_amount'}
    		 , {name : 'reason'}
    		 , {name : 'note'}
    		 , {name : 'budget_cc'}
    		 , {name : 'budget_cc_name'}
    		 , {name : 'budget_cc_type'}
    		 , {name : 'budget_cc_type_name'}
    		 , {name : 'fund_id'}
    		 , {name : 'fund_name'}
    		 , {name : 'pay_type'}
    		 , {name : 'pay_type_name'}
    		 , {name : 'doc_ref'}
    		 , {name : 'folder_ref'}
    		 , {name : 'status'}
    		 , {name : 'wfstatus'}
    		 , {name : 'created_by'}
    		 , {name : 'updated_by'}
    		 , {name : 'updated_time'}
    		 , {name : 'overDue'}
    		 , {name : 'action'}
    ]
});