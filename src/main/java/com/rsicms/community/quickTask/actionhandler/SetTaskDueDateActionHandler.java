package com.rsicms.community.quickTask.actionhandler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;

/**
 * Sets due date for the task being created to specified date.
 */
public class SetTaskDueDateActionHandler extends AbstractBaseNonLeavingActionHandler
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog(SetTaskDueDateActionHandler.class);

    public static final String PARAM_DUE_DATE = "dtDue";
    public static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @SuppressWarnings("unchecked")
    @Override
    public void execute( WorkflowExecutionContext context)
    		throws RSuiteException
    {
        Log wfLog = context.getWorkflowLog();
        logActionHandlerParameters(wfLog);

        wfLog.info("Starting SetTaskDueDateActionHandler");
        
        TaskInstance task =
            (TaskInstance)context.getAttribute(
            "org.jbpm.taskmgmt.exe.TaskInstance");

        String dueDate = resolveVariables( context,
                	                       getParameter( PARAM_DUE_DATE));

        if (StringUtils.isBlank( dueDate)) {
            wfLog.error("Unable to determine dueDate");
            return;
        }

        wfLog.info( "Setting task duedate to \"" + dueDate + "\"");
        try {
			task.setDueDate( DATE_FORMATTER.parse( dueDate));
		} catch (ParseException e) {
			wfLog.error( "Unable to parse date: " + e.getLocalizedMessage());
		}
    }

    public void setDueDate( String duedate)
    {
        setParameter( PARAM_DUE_DATE, duedate);
    }
}
