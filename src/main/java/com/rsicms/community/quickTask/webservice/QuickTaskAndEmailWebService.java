package com.rsicms.community.quickTask.webservice;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.InvokeWebServiceAction;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.NotificationResult;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.reallysi.rsuite.api.workflow.ProcessInstanceSummaryInfo;
import com.rsicms.community.quickTask.QuickTaskConstants;
import com.rsicms.pluginUtilities.FormsUtils;
import com.rsicms.pluginUtilities.uiAction.RefreshInboxAction;
import com.rsicms.rsuite.helpers.webservice.RemoteApiHandlerBase;
import com.rsicms.teamEdition.TEUtils;
import com.rsicms.teamEdition.TEWorkflowConstants;

/**
 * Call quick task and email attachments
 */
public class QuickTaskAndEmailWebService extends RemoteApiHandlerBase {

	private static Log log = LogFactory.getLog(QuickTaskAndEmailWebService.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.reallysi.rsuite.api.remoteapi.RemoteApiHandler#execute(com.reallysi
	 * .rsuite.api.remoteapi.RemoteApiExecutionContext,
	 * com.reallysi.rsuite.api.remoteapi.CallArgumentList)
	 */
	public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args) throws RSuiteException {
		log.info("execute(): args=" + args.getValuesMap());

		String workflowName = QuickTaskConstants.WORKFLOW_NAME;

		String rsuiteId = args.getFirstValue("rsuiteId");
		String sendEmail = args.getFirstValue("sendEmail", "no");
		String descriptionList = args.getFirstValue("taskDescriptionList");
		String description = args.getFirstValue("taskDescription");
		String instructions = args.getFirstValue("taskInstructions").trim().replaceAll("\n", "\n\n");
		String instructionsFirstLine = instructions.split("\n")[0];
		String dueDateValue = args.getFirstValue("dueDate");
		String sendToUser = args.getFirstValue("sendTo");
		String roleNames = args.getFirstString("roleNames");
		List<String> sendToRoleList = args.getStrings( "sendToRole");

		if (descriptionList.equalsIgnoreCase("Other") && description.isEmpty()) {
			RestResult result = new RestResult();
	        UserInterfaceAction action = new InvokeWebServiceAction("team-edition-workflow-quick:QuickTaskAndEmail", "team-edition-workflow-quick:form.startWorkflowWithEmail");
	        Map<String, String> params = new HashMap<String, String>();
	        params.put("rsuiteId", rsuiteId);
	        params.put("roleNames", roleNames);
	        params.put("selectedSendToUser", sendToUser);
	        params.put("selectedDescriptionList", descriptionList);
	        params.put("selectedInstructions", instructions);
	        params.put("selectedDueDate", dueDateValue);
	        params.put("selectedSendEmail", sendEmail);
	        action.addProperty("formParams", params); //make the properties available as arguments in the form handler
	        action.addProperty("serviceParams", params); //make the properties available as arguments in the web service
	        result.addAction(action);
	        return result;
		}
		
		if (description == null || description.isEmpty()) {
			description = descriptionList;
		}
		if (dueDateValue != null) {
			// Need to add a day to the selected date to actually get it. 
			log.info("dueDateValue = " + dueDateValue);
		    Calendar dueDate = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdfAlt = new SimpleDateFormat("yyyy-MMM-d");
			try {
				dueDate.setTime(sdf.parse(dueDateValue));
				//dueDate.add(Calendar.DATE, 1);
				dueDateValue = sdf.format(dueDate.getTime());
			} catch (ParseException e) {
				log.info("Couldn't parse due date, trying alternate date form.");				
	            try {
	                dueDate.setTime(sdfAlt.parse(dueDateValue));
	                //dueDate.add(Calendar.DATE, 1);
	                dueDateValue = sdf.format(dueDate.getTime());
	            } catch (ParseException eAlt) {
	                log.info("Couldn't parse due date with alternate format either.");                
	            }
			}
            log.info("Adjusted date value = " + dueDateValue);
		}
		String sendToRole = StringUtils.join(sendToRoleList, ",");
		log.info("roleNames is = " + roleNames);
		log.info("sendToUser is = " + sendToUser);
		log.info("sendToRole is = " + sendToRole);
		if ( StringUtils.isBlank( sendToRole)) {
			sendToRole = roleNames;
		}

		try {
			User user = getUser(context);

			Map<String, Object> instArgs = new HashMap<String, Object>();
			instArgs.put(TEWorkflowConstants.TASK_DESCRIPTION_VAR_NAME_PARAM, description + ": " + instructionsFirstLine);
			instArgs.put(TEWorkflowConstants.NEXT_STEP_TASK_DESCRIPTION_FULL_VAR_NAME_PARAM, description + ": " + instructionsFirstLine);
			instArgs.put("taskInstructions", instructions);
			if ( !sendToUser.isEmpty()) {
				instArgs.put("sendTo", sendToUser);
			}
			if ( !sendToRole.isEmpty()) {
				instArgs.put("sendToRole", sendToRole);
			}
			instArgs.put("rsuite contents", rsuiteId);
			instArgs.put("returnTo", user.getUserId());
            instArgs.put(TEWorkflowConstants.START_WORKFLOW_USERID_PARAM, user.getUserId());
			if (dueDateValue!= null && !dueDateValue.isEmpty()) instArgs.put("dtDue", dueDateValue);
			instArgs.put(TEWorkflowConstants.WORKFLOW_CONFIGURATION_VAR_NAME_PARAM, "Quick Task");

			ProcessInstanceSummaryInfo info = context.getProcessInstanceService().createAndStart(
					context.getAuthorizationService().getSystemUser(), workflowName, instArgs);
			log.info("Started workflow \"" + workflowName + "\": " + info.getProcessInstanceId());

			NotificationResult result = new NotificationResult("Start QuickTask", "QuickTask started.");

			if ("yes".equals(sendEmail) && !sendToUser.isEmpty()) {
				User sendUser = null;
				try {
					sendUser = context.getAuthorizationService().findUser(sendToUser);
					if (null != sendUser) {
				        String sendToUserEmail = sendUser.getEmail();
						// TODO enable configuration of fromMail address. Although this value is being sent in, right now fromMail is always set to the mail_username rsuite property 
						String fromMail = "noreply@rsuitecms.com";
				        String mailBody = "You've been assigned an RSuite Quick Task! \n\n" + instructions + "\n\nClick here to complete your task: "
								+ TEUtils.getRsuiteUrl(context) + "tasks";
				        // TODO Enable configuration of email text.
						EmailAttachmentWebService.sendEmailWithAttachment(context, 
							fromMail, sendToUserEmail, description, mailBody, rsuiteId);
					}
				} catch (Exception e) {
					log.error("Unable to determine user to send Quick Task notification.");
				}
			}
			result.addAction(new RefreshInboxAction());
			return result;

		} catch (Exception e) {
			log.error("QuickTaskAndEmail: error: " + e.getMessage(), e);
			MessageDialogResult result = new MessageDialogResult(MessageType.ERROR, "QuickTask with Attachment", e.getMessage());
			return result;
		}
	}

}
