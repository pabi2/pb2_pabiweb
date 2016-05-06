package pb.repo.admin.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pb.common.constant.CommonConstant;
import pb.common.constant.JsonConstant;
import pb.common.util.CommonUtil;

public class MainUtil {
	
	public static JSONArray convertToJSONArray(List<Map<String,Object>> inList) throws Exception {
		JSONArray jsArr = new JSONArray();
		
		JSONObject jsObj;
		
		for(Map<String,Object> map : inList) {
			jsObj = new JSONObject();
			
			for(String k: map.keySet()) {
				jsObj.put(k, map.get(k));
			}
			
			jsObj.put("action", "ED");
			
			jsArr.put(jsObj);
		}
		
		return jsArr;
	}
	
	public static String jsonSuccess(JSONArray jsArr) throws JSONException {
		
		Long total = jsArr.length() > 0 ? jsArr.getJSONObject(0).has("totalrowcount")== true? Long.valueOf((Long)jsArr.getJSONObject(0).get("totalrowcount")): 0 : 0;
		JSONObject jsonObj = new JSONObject();

		jsonObj.put(JsonConstant.SUCCESS,  true);
		jsonObj.put(JsonConstant.TOTAL,  total);
		jsonObj.put(JsonConstant.DATA, jsArr);
		
		return jsonObj.toString();
	}
	
	public static String getMessage(String key, Locale locale) {
		return CommonUtil.getMessage(CommonConstant.MODULE_ADMIN, key, locale);
	}
	
	public static String trimComma(String s) {
		
		if (s != null) {
		
			if (s.startsWith(",")) {
				s = s.substring(1);
			}
			
			if (s.endsWith(",")) {
				s = s.substring(0, s.length()-1);
			}
		
		}
		
		return s;
	}
	
	public static String excludeString(String ori, String exc) {
		String[] a = ori.split(",");
		
		StringBuffer excString = new StringBuffer("");
		
		for(String s : a) {
			if (!s.equals(exc)) {
				if (excString.length() > 0) {
					excString.append(",");
				}
				excString.append(s);
			}
		}
		
		return excString.toString();
	}

}