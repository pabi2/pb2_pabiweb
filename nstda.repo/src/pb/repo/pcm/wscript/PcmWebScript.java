package pb.repo.pcm.wscript;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import pb.common.constant.CommonConstant;
import pb.common.model.FileModel;
import pb.common.model.ResultModel;
import pb.common.util.CommonUtil;
import pb.common.util.FileUtil;
import pb.common.util.FolderUtil;
import pb.repo.admin.constant.MainMasterConstant;
import pb.repo.admin.model.MainMasterModel;
import pb.repo.admin.service.AdminHrEmployeeService;
import pb.repo.admin.service.AdminMasterService;
import pb.repo.admin.service.AdminTestSystemService;
import pb.repo.admin.service.AdminUserGroupService;
import pb.repo.admin.service.MainWorkflowService;
import pb.repo.pcm.constant.PcmReqConstant;
import pb.repo.pcm.model.PcmReqCmtDtlModel;
import pb.repo.pcm.model.PcmReqCmtModel;
import pb.repo.pcm.model.PcmReqDtlModel;
import pb.repo.pcm.model.PcmReqModel;
import pb.repo.pcm.service.InterfaceService;
import pb.repo.pcm.service.PcmReqService;
import pb.repo.pcm.util.PcmReqCmtDtlUtil;
import pb.repo.pcm.util.PcmReqCmtUtil;
import pb.repo.pcm.util.PcmReqDtlUtil;
import pb.repo.pcm.util.PcmReqUtil;

import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;

@Component
@WebScript
public class PcmWebScript {
	
	private static Logger log = Logger.getLogger(PcmWebScript.class);
	
	private static final String URI_PREFIX = CommonConstant.GLOBAL_URI_PREFIX + "/pcm";
	
	@Autowired
	private PcmReqService pcmReqService;
	
	@Autowired
	TemplateService templateService;

	@Autowired
	private MainWorkflowService mainWorkflowService;
	
	@Autowired
	private AdminMasterService masterService;
	
	@Autowired
	private ContentService contentService;
	
	@Autowired
	private AuthenticationService authService;
	
	@Autowired
	private AdminUserGroupService userGroupService;
	
	@Autowired
	private AdminTestSystemService adminTestSystemService;
	
	@Autowired
	private AdminHrEmployeeService adminHrEmployeeService;
	
	@Autowired
	InterfaceService interfaceService;
	
  @Uri(URI_PREFIX+"/req/list")
  public void handleList(@RequestParam(required=false) final String s
		  	  , @RequestParam(required=false) final String fields
			  , @RequestParam(required=false) final Integer start
			  , @RequestParam(required=false) final Integer limit
			  , final WebScriptResponse response)  throws Exception {

	  	/*
	  	 * Prepare Criteria
	  	 */
		Map<String, Object> params = new HashMap<String, Object>();
		
		String searchTerm = null;
		
		if (s != null && !s.equals("")) {
			searchTerm = "%" + s + "%";
		}
		params.put("searchTerm", searchTerm);
		params.put("start", start);
		params.put("limit", limit);
		
		String curUser = authService.getCurrentUserName();
		params.put("loginE", curUser);
//		params.put("loginL", "%,"+curUser+",%");
		params.put("loginL", curUser);
		
		String userRoles = userGroupService.getAuthoritiesForUser(curUser)
						.replace("GROUP_", "")
						.replace("','", ",")
						;
		if (userRoles.startsWith("'")) {
			userRoles = userRoles.substring(1);
		}
		if (userRoles.endsWith("'")) {
			userRoles = userRoles.substring(0, userRoles.length()-1);
		}
		
		String[] roles = userRoles.split(",");
		List<String> roleList = new ArrayList<String>();
		for(int i=0; i<roles.length; i++) {
			if (!roles[i].startsWith("site_")) {
//				roleList.add("%,"+roles[i]+",%");
				roleList.add(roles[i]);
			}
		}
		params.put("roleList",  roleList);
		
		
		if (fields != null) {
			JSONObject jsObj = new JSONObject(fields);
			
			putOneParam(params, jsObj, PcmReqConstant.JFN_STATUS);
			putOneParam(params, jsObj, PcmReqConstant.JFN_OBJECTIVE_TYPE);
		}		
		
		params.put("orderBy", "ORDER_FIELD, updated_time DESC");		
	  
		/*
		 * Search
		 */
		String json = null;
		
		try {
			List<PcmReqModel> list = pcmReqService.list(params);
			MainMasterModel showDelBtnCfg = masterService.getSystemConfig(MainMasterConstant.SCC_PCM_REQ_DELETE_BUTTON);
			json = PcmReqUtil.jsonSuccess(list, showDelBtnCfg!=null && showDelBtnCfg.getFlag1()!=null && showDelBtnCfg.getFlag1().equals("1"));
			
		} catch (Exception ex) {
			log.error("", ex);
			json = CommonUtil.jsonFail(ex.toString());
			throw ex;
			
		} finally {
			CommonUtil.responseWrite(response, json);
			
		}
	  
  }
  
  private void putOneParam(Map<String, Object> params, JSONObject jsObj, String fieldName) {
	  try {
		String field = jsObj.getString(fieldName);  
		if (field!=null && !field.equals("") && !field.equals("null")) {
			params.put(fieldName,  field);
		}
	  } catch (Exception ex) {
		  // do nothing
	  }
  }
  
  @Uri(method=HttpMethod.POST, value=URI_PREFIX+"/save")
  public void handleSave(@RequestParam(required=false) final String id
						,@RequestParam(required=false) final String reqBy
		  				,@RequestParam(required=false) final String reqOu
		  				,@RequestParam(required=false) final String objectiveType
		  				,@RequestParam(required=false) final String objective
		  				,@RequestParam(required=false) final String reason
		  				,@RequestParam(required=false) final String currency
		  				,@RequestParam(required=false) final String currencyRate
						,@RequestParam(required=false) final String budgetCc
						,@RequestParam(required=false) final String budgetCcType
						,@RequestParam(required=false) final String isStock
						,@RequestParam(required=false) final String stockOu
						,@RequestParam(required=false) final String isPrototype
						,@RequestParam(required=false) final String prototype
						,@RequestParam(required=false) final String pttContractNo
						,@RequestParam(required=false) final String costControlTypeId
						,@RequestParam(required=false) final String costControlId
						,@RequestParam(required=false) final String pcmOu
						,@RequestParam(required=false) final String location
						,@RequestParam(required=false) final String isAcrossBudget
						,@RequestParam(required=false) final String acrossBudget
						,@RequestParam(required=false) final String isRefId
						,@RequestParam(required=false) final String refId
						,@RequestParam(required=false) final String method
						,@RequestParam(required=false) final String methodCond2Rule
						,@RequestParam(required=false) final String methodCond2
						,@RequestParam(required=false) final String methodCond2Dtl
						,@RequestParam(required=false) final String vat
						,@RequestParam(required=false) final String vatId
						,@RequestParam(required=false) final String total
		  				,@RequestParam(required=false) final String items
		  				,@RequestParam(required=false) final String files
		  				,@RequestParam(required=false) final String cmts
		  				,final WebScriptResponse response) throws Exception {
	
	String json = null;
	
	try {
		PcmReqModel model = null;
		
		if (CommonUtil.isValidId(id)) {
			model = pcmReqService.get(id);
		}
		
		if (model==null) {
			model = new PcmReqModel();
		}
		
		if (model.getId() == null) {
			model.setStatus(PcmReqConstant.ST_DRAFT);
		}
		
		model.setReqBy(reqBy);
		if (reqOu != null && !reqOu.equals("")) {
			model.setReqSectionId(Integer.parseInt(reqOu));
		}
		model.setObjectiveType(objectiveType);
		model.setObjective(objective);
		model.setReason(reason);
		model.setCurrency(currency);
		model.setCurrencyRate(Double.parseDouble(currencyRate));
		if (budgetCc != null && !budgetCc.equals("")) {
			model.setBudgetCc(Integer.parseInt(budgetCc));
		}
		model.setBudgetCcType(budgetCcType);
		model.setIsStock(isStock);
		if (stockOu != null && !stockOu.equals("")) {
			model.setStockSectionId(Integer.parseInt(stockOu));
		}
		model.setIsPrototype(isPrototype);
		model.setPrototype(prototype);
		model.setPrototypeContractNo(pttContractNo);
		model.setCostControlTypeId((costControlTypeId != null && !costControlTypeId.equals("")) ? Integer.parseInt(costControlTypeId) : null);
		model.setCostControlId((costControlId != null && !costControlId.equals("")) ? Integer.parseInt(costControlId) : null);
		if (pcmOu != null && !pcmOu.equals("")) {
			model.setPcmSectionId(Integer.parseInt(pcmOu));
		}
		model.setLocation(location);
		model.setIsAcrossBudget(isAcrossBudget);
		if (acrossBudget != null && !acrossBudget.equals("")) {
			model.setAcrossBudget(Double.parseDouble(acrossBudget));
		}
		model.setIsRefId(isRefId);
		model.setRefId(refId);
		model.setMethod(method);
		model.setMethodCond2Rule(methodCond2Rule);
		model.setMethodCond2(methodCond2);
		model.setMethodCond2Dtl(methodCond2Dtl);
		model.setVat(Double.parseDouble(vat));
		model.setVatId(Integer.parseInt(vatId));
		model.setTotal(Double.parseDouble(total));
		model.setStatus(PcmReqConstant.ST_DRAFT);
		
		model = pcmReqService.save(model, items, files, cmts, false);
		
		JSONObject jsObj = new JSONObject();
		jsObj.put("id", model.getId());
		jsObj.put("status", model.getStatus());
		
		json = CommonUtil.jsonSuccess(jsObj);
	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
	} finally {
		CommonUtil.responseWrite(response, json);
	}
	  
  }
  
  @Uri(method=HttpMethod.POST, value=URI_PREFIX+"/send")
  public void handleSendToReview(@RequestParam(required=false) final String id
								,@RequestParam(required=false) final String reqBy
								,@RequestParam(required=false) final String reqOu
								,@RequestParam(required=false) final String objectiveType
								,@RequestParam(required=false) final String objective
								,@RequestParam(required=false) final String reason
								,@RequestParam(required=false) final String currency
								,@RequestParam(required=false) final String currencyRate
								,@RequestParam(required=false) final String budgetCc
								,@RequestParam(required=false) final String budgetCcType
								,@RequestParam(required=false) final String isStock
								,@RequestParam(required=false) final String stockOu
								,@RequestParam(required=false) final String isPrototype
								,@RequestParam(required=false) final String prototype
								,@RequestParam(required=false) final String pttContractNo
								,@RequestParam(required=false) final String costControlTypeId
								,@RequestParam(required=false) final String costControlId
								,@RequestParam(required=false) final String pcmOu
								,@RequestParam(required=false) final String location
								,@RequestParam(required=false) final String isAcrossBudget
								,@RequestParam(required=false) final String acrossBudget
								,@RequestParam(required=false) final String isRefId
								,@RequestParam(required=false) final String refId
								,@RequestParam(required=false) final String method
								,@RequestParam(required=false) final String methodCond2Rule
								,@RequestParam(required=false) final String methodCond2
								,@RequestParam(required=false) final String methodCond2Dtl
								,@RequestParam(required=false) final String vat
								,@RequestParam(required=false) final String vatId
								,@RequestParam(required=false) final String total
								,@RequestParam(required=false) final String status
								,@RequestParam(required=false) final String items
								,@RequestParam(required=false) final String files
				  				,@RequestParam(required=false) final String cmts
		  				,final WebScriptResponse response) throws Exception {

	String json = null;
	
	try {
		PcmReqModel model = null;
		
		if (CommonUtil.isValidId(id)) {
			model = pcmReqService.get(id);
		}
		
		if (model==null) {
			model = new PcmReqModel();
		}
		model.setWaitingLevel(0);
		
		model.setReqBy(reqBy);
		if (reqOu != null && !reqOu.equals("")) {
			model.setReqSectionId(Integer.parseInt(reqOu));
		}
		model.setObjectiveType(objectiveType);
		model.setObjective(objective);
		model.setReason(reason);
		model.setCurrency(currency);
		model.setCurrencyRate(Double.parseDouble(currencyRate));
		if (budgetCc!=null && !budgetCc.equals("")) {
			model.setBudgetCc(Integer.parseInt(budgetCc));
		}
		model.setBudgetCcType(budgetCcType);
		model.setIsStock(isStock);
		if (stockOu != null && !stockOu.equals("")) {
			model.setStockSectionId(Integer.parseInt(stockOu));
		}
		model.setIsPrototype(isPrototype);
		model.setPrototype(prototype);
		model.setPrototypeContractNo(pttContractNo);
		model.setCostControlTypeId((costControlTypeId != null && !costControlTypeId.equals("")) ? Integer.parseInt(costControlTypeId) : null);
		model.setCostControlId((costControlId != null && !costControlId.equals("")) ? Integer.parseInt(costControlId) : null);
		if (pcmOu!=null && !pcmOu.equals("")) {
			model.setPcmSectionId(Integer.parseInt(pcmOu));
		}
		model.setLocation(location);
		model.setIsAcrossBudget(isAcrossBudget);
		if (acrossBudget!=null && !acrossBudget.equals("")) {
			model.setAcrossBudget(Double.parseDouble(acrossBudget));
		}
		model.setIsRefId(isRefId);
		model.setRefId(refId);
		model.setMethod(method);
		model.setMethodCond2Rule(methodCond2Rule);
		model.setMethodCond2(methodCond2Dtl);
		model.setMethodCond2Dtl(methodCond2Dtl);
		model.setVat(Double.parseDouble(vat));
		model.setVatId(Integer.parseInt(vatId));
		model.setTotal(Double.parseDouble(total));
		model.setStatus(status);	
		
		if (model.getId() == null || (status!=null && status.equals(PcmReqConstant.ST_DRAFT))) {
			model.setStatus(PcmReqConstant.ST_WAITING);
			model.setWaitingLevel(1);
		}
		
		JSONObject validateResult = pcmReqService.validateAssignee(model);
		if (!(Boolean)validateResult.get("valid")) {
			json = CommonUtil.jsonFail(validateResult);
		}
		else {
			model = pcmReqService.save(model, items, files, cmts, true);
			
			mainWorkflowService.setModuleService(pcmReqService);
			mainWorkflowService.startWorkflow(model);
			
			JSONObject jsObj = new JSONObject();
			jsObj.put("id", model.getId());
			json = CommonUtil.jsonSuccess(jsObj);
		}
		
	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
	} finally {
		CommonUtil.responseWrite(response, json);
	}

	  
  }


  @Uri(method=HttpMethod.POST, value=URI_PREFIX+"/req/delete")
  public void handleDelete(@RequestParam final String id, final WebScriptResponse response) throws Exception {
	String json = null;
	
	try {
		pcmReqService.delete(id);
		
		json = CommonUtil.jsonSuccess();
	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
	} finally {
		CommonUtil.responseWrite(response, json);
	}
  }

  @Uri(URI_PREFIX+"/req/get")
  public void handleGet(@RequestParam final String id, final WebScriptResponse response)
      throws Exception {
		
	String json = null;

	try {
	  PcmReqModel model = pcmReqService.get(id);

	  List<PcmReqModel> list = new ArrayList<PcmReqModel>();
	  list.add(model);

	  List<PcmReqDtlModel> dtlList = pcmReqService.listDtlByMasterId(id);  

	  json = PcmReqUtil.jsonSuccess(list, dtlList);

	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
		
	} finally {
		CommonUtil.responseWrite(response, json);
	}
	
  }
  
  @Uri(URI_PREFIX+"/req/userDtl")
  public void handleInitForm(@RequestParam(required=false) final String r,
		  					 @RequestParam(required=false) final String c,
		  					final WebScriptResponse response)
      throws Exception {
		
	String json = null;

	try {
	  List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();

	  Map<String, Object> map = new HashMap<String, Object>();
	  
	  String reqUser = (r!=null) ? r : authService.getCurrentUserName();
	  
	  Map<String,Object> dtl = adminHrEmployeeService.getWithDtl(reqUser);
	  
	  map.put(PcmReqConstant.JFN_REQ_BY, reqUser);
	  
	  String ename = dtl.get("first_name") + " " + dtl.get("last_name");
	  
	  map.put(PcmReqConstant.JFN_REQ_BY_NAME, ename);
	  map.put(PcmReqConstant.JFN_REQ_BY_DEPT, dtl.get("div_name"));
	  
	  map.put(PcmReqConstant.JFN_REQ_BU, dtl.get("org_desc"));
	  
	  map.put(PcmReqConstant.JFN_REQ_SECTION_ID, dtl.get("section_id"));
	  map.put(PcmReqConstant.JFN_REQ_SECTION_NAME, dtl.get("section_name"));
	  
	  
	  String createdUser = (c!=null) ? c : authService.getCurrentUserName();
	  if (!createdUser.equals(reqUser)) {
		  dtl = adminHrEmployeeService.getWithDtl(createdUser);
		  ename = dtl.get("first_name") + " " + dtl.get("last_name");
	  }
	  
	  map.put(PcmReqConstant.JFN_CREATED_BY_SHOW, ename);
	  
	  String mphone = StringUtils.defaultIfEmpty((String)dtl.get("mobile_phone"),"");
	  String wphone = StringUtils.defaultIfEmpty((String)dtl.get("work_phone"),"");
	  String comma = (!mphone.equals("") && !wphone.equals("")) ? "," : "";
	  
	  map.put(PcmReqConstant.JFN_TEL_NO, wphone+comma+mphone);
	  
	  list.add(map);

	  json = CommonUtil.jsonSuccess(list);

	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
		
	} finally {
		CommonUtil.responseWrite(response, json);
	}
	
  }
  
  
  @Uri(URI_PREFIX+"/file/list")
  public void handleFileList(@RequestParam final String id, final WebScriptResponse response)
      throws Exception {
		
	String json = null;
	 
	try {
		
	  List<FileModel> files = pcmReqService.listFile(id, false);
		
	  json = FileUtil.jsonSuccess(files);
		
	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
		
	} finally {
		CommonUtil.responseWrite(response, json);
	}
	
  }
  
  @Uri(URI_PREFIX+"/req/item/list")
  public void handleItemList(@RequestParam final String id, final WebScriptResponse response)
      throws Exception {
		
	String json = null;
	 
	try {
		List<PcmReqDtlModel> list = pcmReqService.listDtlByMasterId(id);
		PcmReqDtlUtil.addAction(list);
		
		json = CommonUtil.jsonSuccess(list);
	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
		
	} finally {
		CommonUtil.responseWrite(response, json);
	}
	
  }  
  
  @Uri(method=HttpMethod.GET, value=URI_PREFIX+"/preview")
  public void handlePreview(@RequestParam final String id
		  					,final WebScriptResponse response) throws Exception {
	  
	  String tmpDir = FolderUtil.getTmpDir();
	  String fullName = tmpDir+File.separator+id+".pdf";
	  Path path = Paths.get(fullName);
	  byte[] data = Files.readAllBytes(path);
		
	  response.setContentType("application/pdf");
	  java.io.OutputStream out = response.getOutputStream();
	    
	  out.write(data);
	  out.flush();
	  out.close();
	  
	  File file = new File(fullName);
	  file.delete();
  }
  
  @Uri(method=HttpMethod.POST, value=URI_PREFIX+"/preview")
  public void handlePreviewGen(@RequestParam(required=false) final String id
							,@RequestParam(required=false) final String reqBy
							,@RequestParam(required=false) final String reqOu
							,@RequestParam(required=false) final String objectiveType
							,@RequestParam(required=false) final String objective
							,@RequestParam(required=false) final String reason
							,@RequestParam(required=false) final String currency
							,@RequestParam(required=false) final String currencyRate
							,@RequestParam(required=false) final String budgetCcType
							,@RequestParam(required=false) final String budgetCc
							,@RequestParam(required=false) final String isStock
							,@RequestParam(required=false) final String stockOu
							,@RequestParam(required=false) final String isPrototype
							,@RequestParam(required=false) final String prototype
							,@RequestParam(required=false) final String pttContractNo
							,@RequestParam(required=false) final String costControlTypeId
							,@RequestParam(required=false) final String costControlId
							,@RequestParam(required=false) final String pcmOu
							,@RequestParam(required=false) final String location
							,@RequestParam(required=false) final String isAcrossBudget
							,@RequestParam(required=false) final String acrossBudget
							,@RequestParam(required=false) final String isRefId
							,@RequestParam(required=false) final String refId
							,@RequestParam(required=false) final String method
							,@RequestParam(required=false) final String methodCond2Rule
							,@RequestParam(required=false) final String methodCond2
							,@RequestParam(required=false) final String methodCond2Dtl
							,@RequestParam(required=false) final String vat
							,@RequestParam(required=false) final String vatId
							,@RequestParam(required=false) final String total
							,@RequestParam(required=false) final String status
							,@RequestParam(required=false) final String dtls
							,@RequestParam(required=false) final String files,
							final WebScriptResponse response) throws Exception {
	
	String json = null;
	
	try {
		PcmReqModel model = null;
		
		if (CommonUtil.isValidId(id)) {
			model = pcmReqService.get(id);
		}
		
		if (model==null) {
			model = new PcmReqModel();
		}
		model.setWaitingLevel(0);
		
		model.setReqBy(reqBy);
		if (reqOu != null && !reqOu.equals("")) {
			model.setReqSectionId(Integer.parseInt(reqOu));
		}
		model.setObjectiveType(objectiveType);
		model.setObjective(objective);
		model.setReason(reason);
		model.setCurrency(currency);
		model.setCurrencyRate(Double.parseDouble(currencyRate));
		model.setBudgetCcType(budgetCcType);
		if (budgetCc != null && !budgetCc.equals("")) {
			model.setBudgetCc(Integer.parseInt(budgetCc));
		}
		model.setIsStock(isStock);
		if (stockOu != null && !stockOu.equals("")) {
			model.setStockSectionId(Integer.parseInt(stockOu));
		}
		model.setIsPrototype(isPrototype);
		model.setPrototype(prototype);
		model.setPrototypeContractNo(pttContractNo);
		model.setCostControlTypeId((costControlTypeId != null && !costControlTypeId.equals("")) ? Integer.parseInt(costControlTypeId) : null);
		model.setCostControlId((costControlId != null && !costControlId.equals("")) ? Integer.parseInt(costControlId) : null);
		if (pcmOu != null && !pcmOu.equals("")) {
			model.setPcmSectionId(Integer.parseInt(pcmOu));
		}
		model.setLocation(location);
		model.setIsAcrossBudget(isAcrossBudget);
		if (acrossBudget!=null && !acrossBudget.equals("")) {
			model.setAcrossBudget(Double.parseDouble(acrossBudget));
		}
		model.setIsRefId(isRefId);
		model.setRefId(refId);
		model.setMethod(method);
		model.setMethodCond2Rule(methodCond2Rule);
		model.setMethodCond2(methodCond2Dtl);
		model.setMethodCond2Dtl(methodCond2Dtl);
		model.setVat(Double.parseDouble(vat));
		model.setVatId(Integer.parseInt(vatId));
		model.setTotal(Double.parseDouble(total));
		model.setStatus(status);
		
		if (model.getId() == null || (status!=null && status.equals(PcmReqConstant.ST_DRAFT))) {
			model.setStatus(PcmReqConstant.ST_WAITING);
			model.setWaitingLevel(1);
		}
		
		JSONObject validateResult = pcmReqService.validateAssignee(model);
		if (!(Boolean)validateResult.get("valid")) {
			json = CommonUtil.jsonFail(validateResult);
		}
		else {		
			String fileName = pcmReqService.doGenDoc("pr", model);
			
			Map<String, Object> map = new HashMap<String, Object>();
			map.put(fileName, "");
			
			json = CommonUtil.jsonSuccess(map);
		}
	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
	} finally {
		CommonUtil.responseWrite(response, json);
	}
	  
  }
  
  @Uri(URI_PREFIX+"/req/criteria/list")
  public void handleCriteriaList(final WebScriptResponse response)  throws Exception {

		String json = null;
		
		try {
			JSONArray jsArr = pcmReqService.listCriteria();
			json = CommonUtil.jsonSuccess(jsArr);
			
		} catch (Exception ex) {
			log.error("", ex);
			json = CommonUtil.jsonFail(ex.toString());
			throw ex;
			
		} finally {
			CommonUtil.responseWrite(response, json);
			
		}
    
  }
  
  
  @Uri(URI_PREFIX+"/req/gridfield/list")
  public void handleGridFieldList(final WebScriptResponse response)  throws Exception {

		String json = null;
		
		try {
			JSONArray jsArr = pcmReqService.listGridField();
			json = CommonUtil.jsonSuccess(jsArr);
			
		} catch (Exception ex) {
			log.error("", ex);
			json = CommonUtil.jsonFail(ex.toString());
			throw ex;
			
		} finally {
			CommonUtil.responseWrite(response, json);
			
		}
    
  }
  
  @Uri(method=HttpMethod.POST, value=URI_PREFIX+"/finish")
  public void handleFinish(@RequestParam(required=false) final String id
						,@RequestParam(required=false) final String reqBy
						,@RequestParam(required=false) final String reqOu
						,@RequestParam(required=false) final String objectiveType
						,@RequestParam(required=false) final String objective
						,@RequestParam(required=false) final String reason
						,@RequestParam(required=false) final String currency
						,@RequestParam(required=false) final String currencyRate
						,@RequestParam(required=false) final String budgetCc
						,@RequestParam(required=false) final String budgetCcType
						,@RequestParam(required=false) final String isStock
						,@RequestParam(required=false) final String stockOu
						,@RequestParam(required=false) final String isPrototype
						,@RequestParam(required=false) final String prototype
						,@RequestParam(required=false) final String pttContractNo
						,@RequestParam(required=false) final String costControlTypeId
						,@RequestParam(required=false) final String costControlId
						,@RequestParam(required=false) final String pcmOu
						,@RequestParam(required=false) final String location
						,@RequestParam(required=false) final String isAcrossBudget
						,@RequestParam(required=false) final String acrossBudget
						,@RequestParam(required=false) final String isRefId
						,@RequestParam(required=false) final String refId
						,@RequestParam(required=false) final String method
						,@RequestParam(required=false) final String methodCond2Rule
						,@RequestParam(required=false) final String methodCond2
						,@RequestParam(required=false) final String methodCond2Dtl
						,@RequestParam(required=false) final String vat
						,@RequestParam(required=false) final String vatId
						,@RequestParam(required=false) final String total
						,@RequestParam(required=false) final String status
						,@RequestParam(required=false) final String items
						,@RequestParam(required=false) final String files
						,@RequestParam(required=false) final String cmts
		  				,final WebScriptResponse response) throws Exception {
	
	String json = null;
	
	try {
		PcmReqModel model = null;
		
		if (CommonUtil.isValidId(id)) {
			model = pcmReqService.get(id);
		}
		
		if (model==null) {
			model = new PcmReqModel();
		}
		model.setWaitingLevel(0);
		
		model.setReqBy(reqBy);
		if (reqOu != null && !reqOu.equals("")) {
			model.setReqSectionId(Integer.parseInt(reqOu));
		}
		model.setObjectiveType(objectiveType);
		model.setObjective(objective);
		model.setReason(reason);
		model.setCurrency(currency);
		model.setCurrencyRate(Double.parseDouble(currencyRate));
		if (budgetCc!=null && !budgetCc.equals("")) {
			model.setBudgetCc(Integer.parseInt(budgetCc));
		}
		model.setBudgetCcType(budgetCcType);
		model.setIsStock(isStock);
		if (stockOu != null && !stockOu.equals("")) {
			model.setStockSectionId(Integer.parseInt(stockOu));
		}
		model.setIsPrototype(isPrototype);
		model.setPrototype(prototype);
		model.setPrototypeContractNo(pttContractNo);
		model.setCostControlTypeId((costControlTypeId != null && !costControlTypeId.equals("")) ? Integer.parseInt(costControlTypeId) : null);
		model.setCostControlId((costControlId != null && !costControlId.equals("")) ? Integer.parseInt(costControlId) : null);
		if (pcmOu!=null && !pcmOu.equals("")) {
			model.setPcmSectionId(Integer.parseInt(pcmOu));
		}
		model.setLocation(location);
		model.setIsAcrossBudget(isAcrossBudget);
		if (acrossBudget!=null && !acrossBudget.equals("")) {
			model.setAcrossBudget(Double.parseDouble(acrossBudget));
		}
		model.setIsRefId(isRefId);
		model.setRefId(refId);
		model.setMethod(method);
		model.setMethodCond2Rule(methodCond2Rule);
		model.setMethodCond2(methodCond2Dtl);
		model.setMethodCond2Dtl(methodCond2Dtl);
		model.setVat(Double.parseDouble(vat));
		model.setVatId(Integer.parseInt(vatId));
		model.setTotal(Double.parseDouble(total));
		model.setStatus(status);	
		
		if (model.getId() == null || (status!=null && status.equals(PcmReqConstant.ST_DRAFT))) {
			model.setStatus(PcmReqConstant.ST_WAITING);
			model.setWaitingLevel(1);
		}
		
		JSONObject validateResult = pcmReqService.validateAssignee(model);
		if (!(Boolean)validateResult.get("valid")) {
			json = CommonUtil.jsonFail(validateResult);
		}
		else {
			model = pcmReqService.save(model, items, files, cmts, true);
			mainWorkflowService.setModuleService(pcmReqService);
			mainWorkflowService.updateWorkflow(model, files, cmts);
			
			JSONObject jsObj = new JSONObject();
			jsObj.put("id", model.getId());
			json = CommonUtil.jsonSuccess(jsObj);
		}
		
	} catch (Exception ex) {
		log.error("", ex);
		json = CommonUtil.jsonFail(ex.toString());
		throw ex;
	} finally {
		CommonUtil.responseWrite(response, json);
	}
	  
  }
  
	@Uri(URI_PREFIX + "/userTask")
	public void handleUserTask(final WebScriptResponse response)
								throws Exception {

		String json = null;

		try {
			MainMasterModel rptModel = masterService.getSystemConfig(MainMasterConstant.SCC_PCM_REQ_RPT_TAB);
			
			JSONObject tasks = new JSONObject();
			tasks.put("pcmReqRptTab", rptModel!=null && rptModel.getFlag1()!=null && rptModel.getFlag1().equals("1"));
			
			JSONObject jsObj = new JSONObject();
			jsObj.put("tasks", tasks);
			
			json = CommonUtil.jsonSuccess(jsObj);
		} catch (Exception ex) {
			log.error("", ex);
			json = CommonUtil.jsonFail(ex.toString());
			throw ex;
		} finally {
			CommonUtil.responseWrite(response, json);
		}
	}
	
	@Uri(URI_PREFIX+"/req/cmt/list")
	public void handleCmtList(@RequestParam final String objType,
			  					final WebScriptResponse response)  throws Exception {
	
		String json = null;
			
		try {
			List<PcmReqCmtModel> list = pcmReqService.listCmt(objType);
				
			json = PcmReqCmtUtil.jsonSuccess(list);
		} catch (Exception ex) {
			log.error("", ex);
			json = CommonUtil.jsonFail(ex.toString());
			throw ex;
				
		} finally {
			CommonUtil.responseWrite(response, json);
		}
	    
	}
	
	
	@Uri(URI_PREFIX+"/req/cmt/dtl/list")
	public void handleCmtHdrList(@RequestParam final String id,
								 @RequestParam final String cmt,
			  					final WebScriptResponse response)  throws Exception {
	
		String json = null;
			
		try {
			List<Map<String,Object>> list = pcmReqService.listCmtDtl(id, cmt);
			PcmReqCmtDtlUtil.addAction(list);
				
			json = CommonUtil.jsonSuccess(list);
		} catch (Exception ex) {
			log.error("", ex);
			json = CommonUtil.jsonFail(ex.toString());
			throw ex;
				
		} finally {
			CommonUtil.responseWrite(response, json);
		}
	    
	}	
	  
	public void genRpt(byte[] file, String fileName, WebScriptResponse response, JasperPrint jasperPrint) throws IOException, JRException {
		
		file = JasperExportManager.exportReportToPdf(jasperPrint);
			
		InputStream is = new ByteArrayInputStream(file);
		response.setContentType("application/pdf");
		
		OutputStream out = response.getOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = is.read(buffer)) > 0){
            out.write(buffer, 0, length);
        }
      
        is.close();
        out.flush();	
        out.close();
	}
	
	@Uri(method=HttpMethod.POST, value=URI_PREFIX+"/req/copy")
	public void handleCopy(@RequestParam final String id, final WebScriptResponse response)
	      throws Exception {
			
		String json = null;
	
		try {
		  String newId = pcmReqService.copy(id);
	
		  json = CommonUtil.jsonSuccess(newId);
	
		} catch (Exception ex) {
			log.error("", ex);
			json = CommonUtil.jsonFail(ex.toString());
			throw ex;
			
		} finally {
			CommonUtil.responseWrite(response, json);
		}
		
	}	
  
}