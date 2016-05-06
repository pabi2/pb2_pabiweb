package pb.repo.pcm.service;

import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TemplateProcessor;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.QName;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pb.common.constant.CommonConstant;
import pb.common.model.FileModel;
import pb.common.util.FileUtil;
import pb.common.util.FolderUtil;
import pb.repo.admin.constant.MainHrEmployeeConstant;
import pb.repo.admin.constant.MainMasterConstant;
import pb.repo.admin.constant.MainWkfConfigDocTypeConstant;
import pb.repo.admin.constant.MainWorkflowConstant;
import pb.repo.admin.dao.MainWorkflowReviewerDAO;
import pb.repo.admin.model.MainHrEmployeeModel;
import pb.repo.admin.model.MainMasterModel;
import pb.repo.admin.model.MainWorkflowNextActorModel;
import pb.repo.admin.model.MainWorkflowReviewerModel;
import pb.repo.admin.model.SubModuleModel;
import pb.repo.admin.service.AdminHrEmployeeService;
import pb.repo.admin.service.AdminMasterService;
import pb.repo.admin.service.AdminUserGroupService;
import pb.repo.admin.service.AdminWkfConfigService;
import pb.repo.admin.service.AlfrescoService;
import pb.repo.admin.service.MainSrcUrlService;
import pb.repo.admin.service.MainWorkflowService;
import pb.repo.admin.service.SubModuleService;
import pb.repo.admin.util.MainUserGroupUtil;
import pb.repo.pcm.constant.PcmOrdConstant;
import pb.repo.pcm.constant.PcmOrdWorkflowConstant;
import pb.repo.pcm.constant.PcmReqConstant;
import pb.repo.pcm.dao.PcmOrdDAO;
import pb.repo.pcm.dao.PcmOrdDtlDAO;
import pb.repo.pcm.model.PcmOrdDtlModel;
import pb.repo.pcm.model.PcmOrdModel;
import pb.repo.pcm.util.PcmOrdUtil;
import pb.repo.pcm.util.PcmUtil;

@Service
public class PcmOrdService implements SubModuleService {

	private static Logger log = Logger.getLogger(PcmOrdService.class);

	@Autowired
	DataSource dataSource;
	
	@Autowired
	AuthenticationService authService;
	
	@Autowired
	WorkflowService workflowService;
	
	@Autowired
	PersonService personService;
	
	@Autowired
	NodeService nodeService;
	
	@Autowired
	AuthorityService authorityService;
	
	@Autowired
	FileFolderService fileFolderService;

	@Autowired
	ContentService contentService;
	
	@Autowired
	AdminMasterService masterService;

	@Autowired
	TemplateService templateService;
	
	@Autowired
	SearchService searchService;
	
	@Autowired
	AdminUserGroupService userGroupService;
	
	@Autowired
	AlfrescoService alfrescoService;
	
	@Autowired
	CheckOutCheckInService checkOutCheckInService;	
	
	@Autowired
	MainSrcUrlService mainSrcUrlService;
	
	@Autowired
	MainWorkflowService mainWorkflowService;
	
	@Autowired
	AdminWkfConfigService adminWkfConfigService;
	
	@Autowired
	AdminHrEmployeeService adminHrEmployeeService;
	
	public PcmOrdModel save(PcmOrdModel model,Map<String, Object> docMap, List<Map<String, Object>> attList) throws Exception {
		
        SqlSession session = PcmUtil.openSession(dataSource);
        
        try {
            PcmOrdDAO pcmOrdDAO = session.getMapper(PcmOrdDAO.class);
//            PcmOrdDtlDAO pcmOrdDtlDAO = session.getMapper(PcmOrdDtlDAO.class);
            
            setUserGroupFields(model);
            
    		model.setUpdatedBy(model.getCreatedBy());
    		
        	doCommonSaveProcess(model, docMap, attList);
        	
    		/*
    		 * Add DB
    		 */
        	pcmOrdDAO.add(model);
            
//            List<PcmOrdDtlModel> dtlList = PcmOrdDtlUtil.convertJsonToList(dtls, model.getId());
//            for(PcmOrdDtlModel dtlModel : dtlList) {
//	        	dtlModel.setCreatedBy(model.getUpdatedBy());
//	        	dtlModel.setUpdatedBy(model.getUpdatedBy());
//	        	
//            	pcmOrdDtlDAO.add(dtlModel);
//            }
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        	throw ex;
        } finally {
        	session.close();
        }

        return model;
	}
	
	public void updateStatus(PcmOrdModel model) throws Exception {
		
        SqlSession session = PcmUtil.openSession(dataSource);
        
        try {
            PcmOrdDAO dao = session.getMapper(PcmOrdDAO.class);
          
            dao.updateStatus(model);
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        	throw ex;
        } finally {
        	session.close();
        }

	}
	
	public JSONObject validateAssignee(PcmOrdModel model) throws Exception {
		
		JSONObject result = new JSONObject();
		
        try {
            setUserGroupFields(model);
            
            Set<String> invalidUsers = new HashSet<String>();
            Set<String> invalidGroups = new HashSet<String>();
            
//           	List<MainApprovalMatrixDtlModel> listDtl = adminApprovalMatrixService.listDtl(model.getApprovalMatrixId());
//           	for (MainApprovalMatrixDtlModel dtlModel : listDtl) {
//                invalidUsers.addAll(userGroupService.listInvalidUser(dtlModel.getReviewerUser()));
//                invalidGroups.addAll(userGroupService.listInvalidGroup(dtlModel.getReviewerGroup()));
//           	}
            
            log.info("invalidUsers:"+invalidUsers);
            log.info("invalidGroups:"+invalidGroups);
            
        	if (invalidUsers.size()>0 || invalidGroups.size()>0) {
				result.put("valid", false);
        		result.put("users", invalidUsers);
        		result.put("groups", invalidGroups);
        	} else {
        		result.put("valid", true);
        	}
        	
        } catch (Exception ex) {
			log.error("", ex);
        	throw ex;
        } finally {
        }

        return result;
	}
	
	private void doCommonSaveProcess(PcmOrdModel model, Map<String, Object> docMap, List<Map<String, Object>> attList) throws Exception {
    	/*
    	 * Create ECM Folder
    	 */
		createEcmFolder(model);
		
		NodeRef folderNodeRef = new NodeRef(model.getFolderRef());
		
		/*
		 * Save Doc
		 */
		model.setDocRef(genDoc(model, folderNodeRef, docMap));
    	
    	/*
    	 * Save Attachments
    	 */
    	for(Map<String, Object> attMap : attList) {
    		log.info(" + "+attMap.get("name"));
		    model.setAttachDoc(genDoc(model, folderNodeRef, attMap));
    	}
	}
	
	public String genDoc(PcmOrdModel model, NodeRef folderNodeRef, Map<String, Object> docMap) throws Exception {
		/*
		 * Convert Base64 String to InputStream 
		 */
    	FileUtil fileUtil = new FileUtil();
    	InputStream is = fileUtil.base64InputStream((String)docMap.get("content"));
    	
    	String ecmFileName = (String)docMap.get("name");
    	
    	log.info("Gen Doc : "+ecmFileName);
    	/*
    	 * Put Doc in ECM
    	 */
    	NodeRef oldDocRef = alfrescoService.searchSimple(folderNodeRef, ecmFileName);
    	if (oldDocRef != null) {
	    	log.info("  is checked out:"+oldDocRef.toString());
		    if (checkOutCheckInService.isCheckedOut(oldDocRef)) {
		    	log.info("    true");
		    	final NodeRef wNodeRef = alfrescoService.getWorkingCopyNodeRef(oldDocRef.toString());
		    	log.info("    cancel check out:"+wNodeRef);
				AuthenticationUtil.runAs(new RunAsWork<String>()
				{
					public String doWork() throws Exception
					{

				    	checkOutCheckInService.cancelCheckout(wNodeRef);
						return null;
					}
				}, AuthenticationUtil.getAdminUserName());
		    }
		    else {
		    	log.info("    false");
		    }

    		alfrescoService.deleteFileFolder(oldDocRef.toString());
    	}
    	NodeRef docRef = alfrescoService.createDoc(folderNodeRef, is, ecmFileName);
    	
    	return docRef.toString();
	}
	
	public List<FileModel> listFile(String id) throws Exception {

		final PcmOrdModel model = get(id);
		log.info("list file : memoId:"+model.getId());
		
		final NodeRef folderNodeRef = new NodeRef(model.getFolderRef());
		log.info("            folderRef:"+model.getFolderRef());
		
		List<FileModel> files = AuthenticationUtil.runAs(new RunAsWork<List<FileModel>>()
	    {
			public List<FileModel> doWork() throws Exception
			{
				List<FileModel> files = new ArrayList<FileModel>();
				
		    	Set<QName> qnames = new HashSet<QName>();
		    	qnames.add(ContentModel.TYPE_CONTENT);
		    	List<ChildAssociationRef> docs = nodeService.getChildAssocs(folderNodeRef, qnames);
		    	for(ChildAssociationRef doc : docs) {
		    		log.info("doc:"+doc.toString());
		    		log.info("   childRef:"+doc.getChildRef().toString());
		    		log.info("   qname:"+doc.getQName().getLocalName());
		    		
		    		if (!doc.getQName().getLocalName().equals(model.getId()+".pdf")) {
		    			FileModel fileModel = new FileModel();
		    			fileModel.setName(doc.getQName().getLocalName());
		    			fileModel.setNodeRef(doc.getChildRef().toString());
		    			fileModel.setAction("D");
		    			files.add(fileModel);
		    		}
		    	}
		    	return files;
			}
	    }, AuthenticationUtil.getAdminUserName());
		
		return files;
	}
	
	public JSONArray listCriteria() throws Exception {
		
		JSONArray jsArr = new JSONArray();
		
		List<MainMasterModel> criList = masterService.listSystemConfig(MainMasterConstant.SCC_PCM_ORD_CRITERIA);

		for(MainMasterModel model : criList) {
			JSONObject jsObj = new JSONObject();
			
			jsObj.put("emptyText", model.getFlag1());
			
			String[] fields = model.getFlag2().split(",");
			jsObj.put("field", fields[0]);
			if (fields.length > 1) {
				jsObj.put("width", Integer.parseInt(fields[1]));
				if (fields.length > 2) {
					jsObj.put("listWidth", Integer.parseInt(fields[2]));
				}
			}
			
			jsObj.put("url", model.getFlag3());
			jsObj.put("param", model.getFlag4());
			jsObj.put("trigger", model.getFlag5());
			
			jsArr.put(jsObj);
		}
		
		return jsArr;
	}	
	
	public JSONArray listGridField() throws Exception {
		
		JSONArray jsArr = new JSONArray();
		
		List<MainMasterModel> list = masterService.listSystemConfig(MainMasterConstant.SCC_PCM_ORD_GRID_FIELD);

		for(MainMasterModel model : list) {
			JSONObject jsObj = new JSONObject();
			
			jsObj.put("label", model.getFlag1());
			
			String[] fields = model.getFlag2().split(",");
			jsObj.put("field", fields[0]);
			if (fields.length > 1) {
				jsObj.put("width", fields[1]);
				if(fields.length > 2) {
					jsObj.put("align", fields[2]);
					if(fields.length > 3) {
						jsObj.put("wrap", fields[3]);
					}
				}
			}
			
			if (model.getFlag3()!=null && !model.getFlag3().equals("")) {
				jsObj.put("type", model.getFlag3());
			}
			
			jsArr.put(jsObj);
		}
		
		return jsArr;
	}
	
	private void createEcmFolder(PcmOrdModel model) throws Exception {
		
		boolean exists = (model.getFolderRef()!=null) && fileFolderService.exists(new NodeRef(model.getFolderRef()));
		
		if (!exists) {
			JSONObject map = PcmOrdUtil.convertToJSONObject(model);
			Calendar cal = Calendar.getInstance();
    		if (cal.get(Calendar.MONTH) >= 9) { // >= October (Thai Start Budget Year)
    			cal.add(Calendar.YEAR, 1);
    		}
    		Timestamp timestampValue = new Timestamp(cal.getTimeInMillis());
    		map.put(PcmReqConstant.JFN_FISCAL_YEAR, timestampValue);
			
			
			Iterator it = map.keys();
			while(it.hasNext()) {
				Object obj = it.next();
				log.info("--"+obj.toString());
			}
	
			Writer w = null;
			TemplateProcessor pc = templateService.getTemplateProcessor("freemarker");
			
			MainMasterModel pathFormatModel = masterService.getSystemConfig(MainMasterConstant.SCC_PCM_ORD_PATH_FORMAT);
			String pathFormat = pathFormatModel.getFlag1();
			
			List<Object> paths = new ArrayList<Object>();
			
			String[] formats = pathFormat.split("/");
			for(String format : formats) {
				Map<String, Object> folderMap = new HashMap<String, Object>();
				paths.add(folderMap);
				
				int pos;
				if (format.indexOf(PcmReqConstant.JFN_FISCAL_YEAR) >= 0
					|| format.indexOf(PcmReqConstant.JFN_CREATED_TIME) >= 0
					|| format.indexOf(PcmReqConstant.JFN_UPDATED_TIME) >= 0
					) {
					
					String dFormat = CommonConstant.RDF_DATE;
					pos = format.indexOf("[");
					if (pos >= 0) {
						int pos2 = format.indexOf("]");
						dFormat = format.substring(pos+1, pos2);
					}
					
					format = format.replace("["+dFormat+"]","");
					int rpos = format.indexOf(CommonConstant.REPS_PREFIX);
					int rpos2 = format.indexOf(CommonConstant.REPS_SUFFIX);
					String fieldName = format.substring(rpos+CommonConstant.REPS_PREFIX.length(), rpos2);
					
					SimpleDateFormat df = new SimpleDateFormat(dFormat);
					folderMap.put("name", df.format(map.get(fieldName)));
				}	
				else {
					pos = format.indexOf("[");
					if (pos >= 0) {
						int pos2 = format.indexOf("]");
						
						String descFieldName = format.substring(pos+1, pos2);
						w = new StringWriter();
						pc.processString("${"+descFieldName+"}", map, w);
						folderMap.put("desc", w.toString());
						w.close();
						
						format = format.replace("["+descFieldName+"]","");
					}
					
					w = new StringWriter();
					pc.processString(format, map, w);
					folderMap.put("name", FolderUtil.getValidFolderName(w.toString()));
					w.close();
				}
			} // for
			
			
			MainMasterModel siteModel = masterService.getSystemConfig(MainMasterConstant.SCC_PCM_ORD_SITE_ID);
			String siteId = siteModel.getFlag1();
			
			log.info("site : "+siteId);
			
			FolderUtil folderUtil = new FolderUtil();
			folderUtil.setSearchService(searchService);
			folderUtil.setFileFolderService(fileFolderService);
			folderUtil.setNodeService(nodeService);
			
			NodeRef folderRef = folderUtil.createFolderStructure(paths, siteId);
			
			if (!folderRef.toString().equals(model.getFolderRef())) {
				model.setFolderRef(folderRef.toString());
			}
		}
	}
	
	private void setUserGroupFields(PcmOrdModel model) throws Exception {
		/*
		 * Requester Group
		 */
//		MainApprovalMatrixModel apModel = adminApprovalMatrixService.get(model.getApprovalMatrixId());
//		model.setRequesterUser(apModel.getRequesterUser());
//		model.setRequesterGroup(apModel.getRequesterGroup());
	}
	
	public List<PcmOrdModel> list(Map<String, Object> params) {
		
		List<PcmOrdModel> list = null;
		
		SqlSession session = PcmUtil.openSession(dataSource);
        try {
            PcmOrdDAO dao = session.getMapper(PcmOrdDAO.class);
            log.info("pcm req list param:"+params);
    		list = dao.list(params);
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        	throw ex;
        } finally {
        	session.close();
        }
        
        return list;
	}
	
	public void deleteReviewerByMasterId(String id) throws Exception {
		SqlSession session = PcmUtil.openSession(dataSource);
        try {
            MainWorkflowReviewerDAO pcmReqReviewerDAO = session.getMapper(MainWorkflowReviewerDAO.class);
            pcmReqReviewerDAO.deleteByMasterId(id);
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        } finally {
        	session.close();
        }
	}
	
	public PcmOrdModel get(String id) {
		
		PcmOrdModel model = null;
		
		SqlSession session = PcmUtil.openSession(dataSource);
        try {
            PcmOrdDAO pcmOrdDAO = session.getMapper(PcmOrdDAO.class);
            
    		model = pcmOrdDAO.get(id);
    		model.setTotalRowCount(1l);
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        } finally {
        	session.close();
        }
        
        return model;
	}
	
	public List<PcmOrdDtlModel> listDtlByMasterId(String masterId) {
		
		List<PcmOrdDtlModel> list = null;
		
		SqlSession session = PcmUtil.openSession(dataSource);
        try {
            PcmOrdDtlDAO dtlDAO = session.getMapper(PcmOrdDtlDAO.class);
            
    		Map<String, Object> map = new HashMap<String, Object>();
    		map.put("masterId", masterId);

    		list = dtlDAO.list(map);
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        	throw ex;
        } finally {
        	session.close();
        }
        
        return list;
	}
	
	public List<PcmOrdDtlModel> listDtlByMasterIdAndFieldName(String masterId, String fieldName) {
		
		List<PcmOrdDtlModel> list = null;
		
		SqlSession session = PcmUtil.openSession(dataSource);
        try {
            PcmOrdDtlDAO dtlDAO = session.getMapper(PcmOrdDtlDAO.class);
            
    		Map<String, Object> map = new HashMap<String, Object>();
    		map.put("masterId", masterId);
    		map.put("fieldName", fieldName);

    		list = dtlDAO.list(map);
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        	throw ex;
        } finally {
        	session.close();
        }
        
        return list;
	}
	
	public void update(PcmOrdModel model) throws Exception {
		
        SqlSession session = PcmUtil.openSession(dataSource);
        
        try {
            PcmOrdDAO pcmReqDAO = session.getMapper(PcmOrdDAO.class);
            
        	/*
        	 * Update DB
        	 */
            model.setUpdatedTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
            
        	pcmReqDAO.update(model);
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        	throw ex;
        } finally {
        	session.close();
        }

	}
	
	public void addReviewer(MainWorkflowReviewerModel pcmReqReviewerModel) throws Exception {
		
		SqlSession session = PcmUtil.openSession(dataSource);
        try {
           
            MainWorkflowReviewerDAO pcmReqReviewerDAO = session.getMapper(MainWorkflowReviewerDAO.class);

    		pcmReqReviewerDAO.add(pcmReqReviewerModel);

            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        } finally {
        	session.close();
        }
	}
	
	public MainWorkflowReviewerModel getReviewer(MainWorkflowReviewerModel pcmReqReviewerModel) throws Exception {
		
		MainWorkflowReviewerModel model = null;
		
		SqlSession session = PcmUtil.openSession(dataSource);
        try {
           
            MainWorkflowReviewerDAO pcmReqReviewerDAO = session.getMapper(MainWorkflowReviewerDAO.class);

    		List<MainWorkflowReviewerModel> list = pcmReqReviewerDAO.listByLevel(pcmReqReviewerModel);
    		if (list.size()>0) {
    			model = list.get(0);
    		}
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        } finally {
        	session.close();
        }
        
        return model;
	}	
	
	public List<Map<String, Object>> listWorkflowPath(String id) {
		
		List<Map<String, Object>> list = null;
		
		SqlSession session = PcmUtil.openSession(dataSource);
        try {
           
            PcmOrdDAO dao = session.getMapper(PcmOrdDAO.class);

    		list = dao.listWorkflowPath(id);
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        } finally {
        	session.close();
        }
        
        return list;
	}

	@Override
	public void update(SubModuleModel subModuleModel) throws Exception {
		PcmOrdModel model = (PcmOrdModel)subModuleModel;
		
        SqlSession session = PcmUtil.openSession(dataSource);
        
        try {
            PcmOrdDAO dao = session.getMapper(PcmOrdDAO.class);
            
        	/*
        	 * Update DB
        	 */
            model.setUpdatedTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
            
        	dao.update(model);
            
            session.commit();
        } catch (Exception ex) {
			log.error("", ex);
        	session.rollback();
        	throw ex;
        } finally {
        	session.close();
        }		
	}

	@Override
	public String getWorkflowName() throws Exception {
		return PcmOrdConstant.WF_NAME;
	}

	@Override
	public String getWorkflowDescription(SubModuleModel paramModel)
			throws Exception {
		PcmOrdModel pcmOrdModel = (PcmOrdModel)paramModel;
		
		MainMasterModel descFormatModel = masterService.getSystemConfig(MainMasterConstant.SCC_PCM_ORD_WF_DESC_FORMAT);
		String descFormat = descFormatModel.getFlag1();
		
		JSONObject map = PcmOrdUtil.convertToJSONObject(pcmOrdModel);
		
		Writer w = null;
		TemplateProcessor pc = templateService.getTemplateProcessor("freemarker");
		w = new StringWriter();
		pc.processString(descFormat, map, w);
		
		return w.toString();
	}

	@Override
	public Map<String, Object> convertToMap(SubModuleModel model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSubModuleType() {
		return CommonConstant.SUB_MODULE_PCM_ORD;	
	}

	@Override
	public void setWorkflowParameters(Map<QName, Serializable> parameters, SubModuleModel paramModel, List<NodeRef> docList, List<NodeRef> attachDocList) {
		PcmOrdModel model = (PcmOrdModel)paramModel;
		
		/*
		 * Common Attribute
		 */
        parameters.put(PcmOrdWorkflowConstant.PROP_ID, model.getId());
        parameters.put(PcmOrdWorkflowConstant.PROP_FOLDER_REF, model.getFolderRef());

        parameters.put(PcmOrdWorkflowConstant.PROP_DOCUMENT, (Serializable)docList);
        parameters.put(PcmOrdWorkflowConstant.PROP_ATTACH_DOCUMENT, (Serializable)attachDocList);
        parameters.put(PcmOrdWorkflowConstant.PROP_COMMENT_HISTORY, "");
        
        parameters.put(PcmOrdWorkflowConstant.PROP_TASK_HISTORY, "");	
        
        /*
         * Special Attribute
         */
		parameters.put(PcmOrdWorkflowConstant.PROP_OBJECTIVE, model.getObjective());
		parameters.put(PcmOrdWorkflowConstant.PROP_TOTAL, model.getTotal());
		
		Map<String, Object> docType = adminWkfConfigService.getDocType(model.getDocType());
		parameters.put(PcmOrdWorkflowConstant.PROP_METHOD, (String)docType.get(MainWkfConfigDocTypeConstant.TFN_DESCRIPTION));
		
		MainHrEmployeeModel empModel = adminHrEmployeeService.get(model.getAppBy());
		parameters.put(PcmOrdWorkflowConstant.PROP_REQUESTER, empModel.getFirstName()+" "+empModel.getLastName());
	}

	@Override
	public String getActionCaption(String action) {
		Map<String, String> WF_TASK_ACTIONS = new HashMap<String, String>();
		
    	WF_TASK_ACTIONS.put(MainWorkflowConstant.TA_START, "ขออนุมัติ");
		
    	WF_TASK_ACTIONS.put(MainWorkflowConstant.TA_APPROVE, "อนุมัติ");
    	WF_TASK_ACTIONS.put(MainWorkflowConstant.TA_REJECT, "ไม่อนุมัติ");
    	WF_TASK_ACTIONS.put(MainWorkflowConstant.TA_CONSULT, "ขอคำปรึกษา");
    	
    	WF_TASK_ACTIONS.put(MainWorkflowConstant.TA_COMMENT, "ให้ความเห็น");
    	
    	WF_TASK_ACTIONS.put(MainWorkflowConstant.TA_RESUBMIT, "ขออนุมัติใหม่");
    	WF_TASK_ACTIONS.put(MainWorkflowConstant.TA_CANCEL, "ยกเลิก");
    	
    	WF_TASK_ACTIONS.put(MainWorkflowConstant.TA_COMPLETE, "PO");
		
		return WF_TASK_ACTIONS.get(action);
	}

	@Override
	public List<MainWorkflowNextActorModel> listNextActor(SubModuleModel model) {
		List<MainWorkflowNextActorModel> list = new ArrayList<MainWorkflowNextActorModel>();
		
		PcmOrdModel realModel = (PcmOrdModel)model;
		
		List<Map<String, Object>> superList = adminWkfConfigService.listSupervisor(realModel.getSectionId());
		if(superList.size()>0) {
			Map<String, Object> map = superList.get(0);
			
			MainWorkflowNextActorModel actorModel = new MainWorkflowNextActorModel();
			
			actorModel.setMasterId(model.getId());
			actorModel.setLevel(1);
			actorModel.setActor(PcmReqConstant.NA_BOSS);
			actorModel.setActorUser(MainUserGroupUtil.code2login((String)map.get(MainHrEmployeeConstant.TFN_EMPLOYEE_CODE)));
			actorModel.setCreatedBy(model.getUpdatedBy());
			
			list.add(actorModel);
		}
		
		return list;
	}

	@Override
	public String getFirstComment(SubModuleModel model) {
		PcmOrdModel realModel = (PcmOrdModel)model;
		
		return realModel.getObjective() + " " + realModel.getDocType();
	}

	@Override
	public String getNextActionInfo() {
		return "ฝ่ายพัสดุ";
	}

	@Override
	public QName getPropNextReviewers() {
		return PcmOrdWorkflowConstant.PROP_NEXT_REVIEWERS;
	}
	
	@Override
	public String getModelUri() {
		return PcmOrdWorkflowConstant.MODEL_URI;
	}

	@Override
	public String getWfUri() {
		return PcmOrdWorkflowConstant.WF_URI;
	}

	@Override
	public String getModelPrefix() {
		return PcmOrdWorkflowConstant.MODEL_PREFIX;
	}	
}