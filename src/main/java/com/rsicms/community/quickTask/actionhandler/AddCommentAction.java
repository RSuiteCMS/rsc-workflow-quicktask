package com.rsicms.community.quickTask.actionhandler;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;

/**
 * Assigns the task being created to specified user name.
 */
public class AddCommentAction extends AbstractBaseNonLeavingActionHandler {
	private static final long serialVersionUID = 1L;

	public void execute(WorkflowExecutionContext context) throws RSuiteException {
		Log wfLog = context.getWorkflowLog();
		logActionHandlerParameters(wfLog);

		wfLog.info("Starting AddCommentAction");

		String commentText = context.getVariable("taskInstructions");
		String commenterUserId = context.getVariable("returnTo");

		User user = null;
		try {
			user = context.getAuthorizationService().findUser(commenterUserId);
			if (null == commenterUserId || commenterUserId.isEmpty() || user == null) {
				wfLog.error("Unable to determine user for comment");
				commenterUserId = getSystemUser().getUserId();
			}
		} catch (Exception e) {
			wfLog.error("Unable to determine user for comment");
			commenterUserId = getSystemUser().getUserId();
		}

		wfLog.info("Adding comment: " + commentText);
        String streamName = AbstractBaseNonLeavingActionHandler.RSUITE_COMMENT_STREAM_GLOBAL;
        createWorkflowComment(context, commenterUserId, streamName, commentText);

		wfLog.info("Finished AddCommentAction");
	}

}
