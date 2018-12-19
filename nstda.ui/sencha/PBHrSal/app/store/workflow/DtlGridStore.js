Ext.define('PBHrSal.store.workflow.DtlGridStore', {
    extend: 'Ext.data.Store',
    model: 'PBHrSal.model.workflow.DtlGridModel',
    autoLoad:false,
    pageSize:PAGE_SIZE,

    proxy: {
        type: 'ajax',
        timeout: GRID_TIME_OUT,
        api: {
        	read: ALF_CONTEXT+'/hr/sal/wf/dtl/list'
        },
        reader: {
            type: 'json',
            root: 'data',
            successProperty: 'success'
        }
    }
});