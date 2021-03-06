package org.gooru.insights.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.util.IOUtils;
import org.gooru.insights.constants.InsightsOperationConstants;
import org.gooru.insights.models.ResponseParamDTO;
import org.gooru.insights.security.AuthorizeOperations;
import org.gooru.insights.services.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ItemController extends BaseController{
	
	@Autowired
	private ItemService itemService;
	
	/**
	 * This will check the tomcat service availability
	 * @param request HttpServlet Request 
	 * @param response HttpServlet Response
	 * @return Model view object 
	 */
	@RequestMapping(value = "/query/server/status", method = RequestMethod.GET)
	public ModelAndView checkAPiStatus(HttpServletRequest request, HttpServletResponse response) {
		return getModel(getItemService().serverStatus());
	}

	/**
	 * This performs Elastic search query operation
	 * @param request HttpServlet Request 
	 * @param data This will hold the client request data
	 * @param sessionToken This is an Gooru sessionToken for validation
	 * @param response HttpServlet Response
	 * @return Model view object
	 * @throws Exception
	 */
	@RequestMapping(value = "/query", method = { RequestMethod.GET, RequestMethod.POST })
	@AuthorizeOperations(operations = InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView generateQuery(HttpServletRequest request, @RequestBody String data,
			HttpServletResponse response) throws Exception {

		return getModel(itemService.generateQuery(getTraceId(request),getRequestData(request,data), getSessionToken(request), null));
	}
	
	/**
	 * This will list the manageable report
	 * @param request
	 * @param action
	 * @param reportName
	 * @param sessionToken
	 * @param data
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/query/{action}/report", method = RequestMethod.POST)
	@AuthorizeOperations(operations = InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView manageReports(HttpServletRequest request, @PathVariable(value = "action") String action, @RequestParam(value = "reportName", required = true) String reportName,
			@RequestBody String data, HttpServletResponse response) throws Exception {

		return getModel(itemService.manageReports(getTraceId(request),action, reportName, data));
	}
	
	/**
	 * This will provide the party related reports
	 * @param request HttpServlet Request 
	 * @param reportType 
	 * @param sessionToken
	 * @param response HttpServlet Response
	 * @return Model view object
	 * @throws Exception
	 */
	@RequestMapping(value = "/query/report/{reportType}", method = { RequestMethod.GET, RequestMethod.POST })
	@AuthorizeOperations(operations = InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView getPartyReports(HttpServletRequest request, @PathVariable(value = "reportType") String reportType,
			HttpServletResponse response) throws Exception {

		return getModel(itemService.getPartyReport(getTraceId(request),request, reportType, getSessionToken(request)));
	}
	
	/**
	 * This will clear the stored query in redis
	 * @param request is the client HTTPRequest
	 * @param queryId is the unique query id for each query
	 * @param response is the client HTTPResponse
	 * @return Model view object
	 * @throws Exception 
	 */
	@RequestMapping(value="/query/clear/id",method =RequestMethod.DELETE)
	public ModelAndView clearRedisCache(HttpServletRequest request,@RequestParam(value="queryId",required = true) String queryId,HttpServletResponse response) throws Exception{
	
		return getModel(itemService.clearQuery(getTraceId(request),queryId));
	}
	
	/**
	 * This will provide the query result for the given query id
	 * @param request is the client HTTPRequest
	 * @param queryId is the unique query id for each query
	 * @param sessionToken is the Gooru user token
	 * @param response is the client HTTPResponse
	 * @return Model view object
	 * @throws Exception 
	 */
	@RequestMapping(value="/query/{id}",method =RequestMethod.GET)
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView getRedisCache(HttpServletRequest request,@PathVariable("id") String queryId,HttpServletResponse response) throws Exception{
		
		return getModel(itemService.getQuery(getTraceId(request),queryId,getSessionToken(request)));
	}

	/**
	 * This will provide the list of query result for the given query id or the whole item
	 * @param request is the client HTTPRequest
	 * @param queryId is the unique query id for each query
	 * @param sessionToken is the Gooru user token
	 * @param response is the client HTTPResponse
	 * @return Model view object
	 * @throws Exception 
	 */
	@RequestMapping(value="/query/list",method =RequestMethod.GET)
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView getRedisCacheList(HttpServletRequest request,@RequestParam(value="queryId",required = false) String queryId,HttpServletResponse response) throws Exception{
		 
		return getModel(getItemService().getCacheData(getTraceId(request),queryId,getSessionToken(request)));
	}
	
	/**
	 * 
	 * @param request is the client HTTPRequest
	 * @param data is the API query to store in redis
	 * @param response is the client HTTPResponse
	 * @return Model view object
	 * @throws Exception 
	 */
	@RequestMapping(value="/query/keys",method =RequestMethod.PUT)
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView putRedisData(HttpServletRequest request,@RequestBody String data ,HttpServletResponse response) throws Exception{
		
		return getModel(getItemService().insertKey(getTraceId(request),data));
	}
	
	/**
	 * This will clear the cached data
	 * @return Model view object
	 */
	@RequestMapping(value="/query/clear/data",method =RequestMethod.DELETE)
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView clearDataCache(){
		
		return getModel(getItemService().clearDataCache());
	}
	
	/**
	 * This will combine the two API call and project it as single
	 * @param request is the client HTTPRequest
	 * @param data is the API query to fetch data from ELS
	 * @param sessionToken is the Gooru user token
	 * @param response response is the client HTTPResponse
	 * @return Model view object
	 * @throws Exception
	 */
	@RequestMapping(value="/query/combine",method ={RequestMethod.GET,RequestMethod.POST})
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView getItems(HttpServletRequest request,@RequestParam(value="data",required = false) String data,HttpServletResponse response) throws Exception{
		
		return getModel(getItemService().processApi(getTraceId(request),data,getSessionToken(request)));
	}
	
	/**
	 * This will clear the data connection of cassandra and ELS
	 * @return Model view object
	 */
	@RequestMapping(value="/query/clear/connection",method =RequestMethod.DELETE)
	public ModelAndView clearConnectionCache(){
		return getModel(getItemService().clearConnectionCache());
	}

	public ItemService getItemService() {
		return itemService;
	}
	
	@RequestMapping(value="/export/report", method = RequestMethod.GET)
	@AuthorizeOperations(operations = InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView generateQueryReport(HttpServletRequest request, @RequestParam(value = "data", required = true) String data, @RequestParam(value = "fileFormat", required = false, defaultValue = "csv") String fileFormat, 
			HttpServletResponse response) throws Exception {
		ResponseParamDTO<Map<String, Object>> responseDTO = itemService.exportReport(getTraceId(request),data, getSessionToken(request), fileFormat);
		if(responseDTO.getMessage().containsKey(FILE_PATH)) {
			generateFileOutput(response, new File(responseDTO.getMessage().get(FILE_PATH).toString()), XLSX_CONTENT_TYPE);
			return null;
		}
		return getModel(responseDTO);
	}
	
	public void generateFileOutput(HttpServletResponse response, File excelFile, String contentType) throws IOException {
		InputStream sheet = new FileInputStream(excelFile);
		response.setHeader("Content-Disposition", "inline; filename=" + excelFile.getName());
		response.setContentType(contentType);
		IOUtils.copy(sheet, response.getOutputStream());
		response.getOutputStream().flush();
		excelFile.delete();
		response.getOutputStream().close();
	}
	
	@RequestMapping(value = "/stats/data", method = RequestMethod.GET)
	@AuthorizeOperations(operations = InsightsOperationConstants.OPERATION_INSIGHTS_REPORTS_VIEW)
	public ModelAndView getStatisticsData(HttpServletRequest request, @RequestParam(value = "gooruOId", required = true) String gooruOId, @RequestParam(value = "gooruUId", required = false) String gooruUId, @RequestParam(value = "fields", required = true) String fields, HttpServletResponse response) throws Exception {
		return getModel(getItemService().getLiveDashboardData(getTraceId(request), gooruOId, gooruUId, fields));
	}
}
