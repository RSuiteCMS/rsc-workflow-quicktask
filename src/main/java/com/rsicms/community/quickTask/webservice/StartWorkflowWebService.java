package com.rsicms.community.quickTask.webservice;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.security.LocalUserManager;
import com.reallysi.rsuite.api.security.Role;
import com.reallysi.rsuite.api.workflow.ProcessInstanceSummaryInfo;
import com.reallysi.rsuite.service.AuthorizationService;
import com.rsicms.teamEdition.TEWorkflowConstants;
import com.rsicms.community.quickTask.QuickTaskConstants;
import com.rsicms.pluginUtilities.uiAction.RefreshInboxAction;

/**
 * Web service to start a workflow.
 * <p>
 * Although RSuite supports the invocation of workflows directly from context
 * menus/forms, this class provides the ability to have a workflow associated
 * with a different user from the one that starts it. For example, for cases
 * where one user assigns the process to another user.
 * </p>
 */
public class StartWorkflowWebService extends BaseRemoteApiHandler {
    private static Log log = LogFactory.getLog(StartWorkflowWebService.class);

    /**
     * Start the workflow.
     * <p>
     * If any exception is thrown when trying to start the workflow, this method
     * will return an error dialog result.
     * </p>
     * <p>
     * Sub-classes do not normally have to override this method.
     * </p>
     */
    public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args) throws RSuiteException {
        logArgs(args);

        // We'll catch any exceptions and return an error
        // response if we do. Therefore, it is okay for
        // code to throw an exception to terminate processing.
        try {
            Map<String, Object> parameters = copyArguments(args);
            preExecuteWorkflow(context, parameters);

            return executeWorkflow(context, parameters, getWorkflowName(), getUserArgName(), getUserRole());

        } catch (Throwable e) {
            log.error(e.getLocalizedMessage(), e);
            return new MessageDialogResult(

            MessageType.ERROR, "Error", "Error starting \"" + getWorkflowLabel() + "\": " + e.getLocalizedMessage() + ".");

        }
    }

    /**
     * Execute any operations before attempting to start workflow.
     * <p>
     * Default implementation does nothing and returns a <tt>null</tt>. If a
     * non-<tt>null</tt> value is returned, it becomes of the return value of
     * this service and any further processing is aborted.
     * </p>
     * 
     * @param context
     *            Execution context.
     * @param args
     *            Request arguments.
     * @return If not <tt>null</tt>, the workflow is <b>NOT</b> started and the
     *         return value becomes the return value of the this service.
     * @throws RSuiteException
     */
    protected void preExecuteWorkflow(RemoteApiExecutionContext context, Map<String, Object> parameters) throws RSuiteException {
        parameters.put("returnTo", this.getUser(context).getUserId());
    }

    /**
     * Retrieve name of workflow to start.
     * 
     * @return Name of workflow to start.
     */
    protected String getWorkflowName() {
        return QuickTaskConstants.WORKFLOW_NAME;
    }

    /**
     * Retrieve human-readable label identifying workflow.
     * <p>
     * Default implementation returns string retrieve from
     * {@link #getWorkflowName()}.
     * </p>
     * 
     * @return Workflow label.
     */
    protected String getWorkflowLabel() {
        return getWorkflowName();
    }

    /**
     * Retrieve global workflow variables to set.
     * <p>
     * Sub-classes can override this method to set any additional workflow
     * variables that should be set.
     * </p>
     * <p>
     * <b>NOTE:</b> This method is called ONLY once. The set of variables
     * returned will be applied to each workflow instance started. If different
     * variables settings are required on a pre-workflow instance, then the
     * {@link #getWorkflowVars} should be used.
     * </p>
     * <p>
     * <b>NOTE:</b> Service request parameters will automatically be added to
     * the list of workflow variables. Therefore, sub-classes only need to
     * override this method if additional variables beyond request parameters
     * are to be set.
     * </p>
     * 
     * @param context
     *            Execution context.
     * @param serviceArgs
     *            Arguments to service.
     * @param idList
     *            List of context IDs.
     * @return Default implementation return null.
     */
    protected Map<String, Object> getGlobalWorkflowVars(RemoteApiExecutionContext context, Map<String, Object> serviceArgs, String[] idList)
            throws RSuiteException {
        return null;
    }

    /**
     * Retrieve additional workflow variables to set.
     * <p>
     * Sub-classes can override this method to set any additional workflow
     * variables that should be set.
     * </p>
     * <p>
     * <b>NOTE:</b> This method is called for each workflow instance started.
     * Therefore, if {@link #doProcessPerObject} is <tt>true</tt>, this method
     * will be called for before each workflow instance started and the
     * <var>idList</var> parameter will, at most, contain a single ID value.
     * </p>
     * <p>
     * <b>NOTE:</b> Service request parameters will automatically be added to
     * the list of workflow variables. Therefore, sub-classes only need to
     * override this method if additional variables beyond request parameters
     * are to be set.
     * </p>
     * 
     * @param context
     *            Execution context.
     * @param serviceArgs
     *            Arguments to service.
     * @param idList
     *            List of context IDs.
     * @return Default implementation return null.
     */
    protected Map<String, Object> getWorkflowVars(RemoteApiExecutionContext context, Map<String, Object> serviceArgs, String[] idList)
            throws RSuiteException {
        return null;
    }

    /**
     * Retrieve request argument name specifying user ID.
     */
    public String getUserArgName() {
        return "sendTo";
    }

    /**
     * Retrieve role label of user.
     * 
     * @return Default implementation returns <tt>null</tt>.
     */
    public String getUserRole() {
        return null;
    }

    /**
     * Retrieve role label of user - for log messages.
     * 
     * @return Default implementation returns <tt>User</tt>.
     */
    public static String getUserRoleName() {
        return "User";
    }

    /**
     * Check if workflow should be started for each context object.
     * <p>
     * This method returns <tt>false</tt> by default. Sub-classes can override
     * this method to return <tt>true</tt> if a separate workflow instance
     * should be started for each context object.
     * </p>
     * 
     * @param context
     *            Execution context.
     * @param idList
     *            List of context object IDs.
     * @param userArg
     *            Name of argument specifying user ID to associate with
     *            workflow.
     * @param userRole
     *            Role name of user.
     * @return Default implementation return <tt>false</tt>.
     * @throws RSuiteException
     */
    protected boolean doProcessPerObject(RemoteApiExecutionContext context, String[] idList, String userArg, String userRole)
            throws RSuiteException {
        return false;
    }

    /**
     * Start workflow.
     * <p>
     * If <var>userArg</var> is null, then the user associated with the current
     * session will be associated with the workflow started.
     * </p>
     * 
     * @param context
     *            Execution context.
     * @param remoteArgs
     *            Web service argument list.
     * @param workflowName
     *            Name of workflow to start.
     * @param workflowVars
     *            Additional workflow variables to set.
     * @param userArg
     *            Name of argument specifying user ID to associate with
     *            workflow.
     * @param userRole
     *            Role name of user.
     * @return Service result instance to send back to client.
     * @throws Exception
     *             if an error occurs.
     */
    protected RemoteApiResult executeWorkflow(RemoteApiExecutionContext context, Map<String, Object> parameters, String workflowName,
            String userArg, String userRole) throws Exception {
        if (StringUtils.isBlank(workflowName)) {
            throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR, "Workflow name is null");
        }
        User assigner = context.getSession().getUser();

        Map<String, Object> args = new HashMap<String, Object>();
        for (String name : parameters.keySet()) {
            args.put(name, parameters.get(name));
        }
        addUserInfoToArgs(context, assigner, userArg, userRole, args);
        String idList[] = getIdList(assigner, context, args);
        idList = assertCanStartWorkflow(context, assigner, idList, args);
        boolean doMulti = doProcessPerObject(context, idList, userArg, userRole);

        Map<String, Object> globalArgs = getGlobalWorkflowVars(context, args, idList);

        StringBuilder pids = new StringBuilder();
        if (doMulti && (idList != null) && (idList.length > 0)) {
            for (String id : idList) {
                // XXX: Not knowing if arg map may contain mutable objects
                // and how JBPM operates, we create a separate map instance
                // for each workflow so changes to variable data in one
                // workflow does not affect another workflow.
                Map<String, Object> instArgs = new HashMap<String, Object>();
                for (String name : parameters.keySet()) {
                    instArgs.put(name, parameters.get(name));
                }
                addUserInfoToArgs(context, assigner, userArg, userRole, instArgs);
                if (globalArgs != null)
                    instArgs.putAll(globalArgs);
                Map<String, Object> workflowVars = getWorkflowVars(context, args, new String[] { id });
                if (workflowVars != null) {
                    instArgs.putAll(workflowVars);
                }
                instArgs.put("rsuite contents", id);

                ProcessInstanceSummaryInfo info = context.getProcessInstanceService().createAndStart(
                        context.getAuthorizationService().getSystemUser(), workflowName, instArgs);
                log.info("Started workflow \"" + workflowName + "\": " + info.getProcessInstanceId());
                if (pids.length() > 0)
                    pids.append(',');
                pids.append(info.getProcessInstanceId());
            }
        } else {
            if (globalArgs != null)
                args.putAll(globalArgs);
            Map<String, Object> workflowVars = getWorkflowVars(context, args, idList);
            if (workflowVars != null) {
                args.putAll(workflowVars);
            }
            if (idList != null && idList.length > 0) {
                args.put("rsuite contents", StringUtils.join(idList, ","));
            }
            args.put(TEWorkflowConstants.START_WORKFLOW_USERID_PARAM, assigner.getUserId());
            ProcessInstanceSummaryInfo info = context.getProcessInstanceService().createAndStart(
                    context.getAuthorizationService().getSystemUser(), workflowName, args);
            log.info("Started workflow \"" + workflowName + "\": " + info.getProcessInstanceId());
            pids.append(info.getProcessInstanceId());
        }

        String idsLabel = (pids.indexOf(",") < 0) ? "ID" : "IDs";
        RestResult result = new MessageDialogResult(MessageType.SUCCESS, getWorkflowLabel() + " Started", "\"" + getWorkflowLabel()
                + "\" started.  Process instance " + idsLabel + ": " + pids);
        result.addAction(new RefreshInboxAction());
        return result;
    }

    /**
     * Assert if workflow can be started.
     * <p>
     * This method can be overrided by sub-classes to set assert any conditions
     * that must exist before workflow can be started. If assertion fails, this
     * method should throw an exception, preventing the workflow from starting.
     * </p>
     * <p>
     * This method can also be used to alter the MO ID context list to be
     * associated with any workflow started.
     * </p>
     * <p>
     * The base class will catch the exception, and provide an error dialog back
     * to the user displaying the message associated with the exception.
     * </p>
     * <p>
     * Default implementation of this method does nothing and returns the
     * <tt>idList</tt> parameter.
     * </p>
     * 
     * @param context
     *            Execution context.
     * @param user
     *            User attempting to start workflow.
     * @param idList
     *            Workflow context MO ID list.
     * @param variables
     *            Variables to be passed to workflow.
     * @return Workflow context MO ID list.
     * @throws RSuiteException
     *             if assertion fails.
     */
    protected String[] assertCanStartWorkflow(RemoteApiExecutionContext context, User user, String[] idList, Map<String, Object> variables)
            throws RSuiteException {
        return idList;
    }

    /**
     * Add user-related variables to arguments map.
     * <p>
     * This method is called before starting the workflow so user-related
     * workflow variables are set.
     * </p>
     * 
     * @param context
     *            Execution context.
     * @param assigner
     *            User making the reviewer assignment.
     * @param userArg
     *            Name of argument containing user (optional).
     * @param userRole
     *            Role name of user (optional).
     * @param args
     *            Arguments map to set user-related variables.
     */
    protected static void addUserInfoToArgs(RemoteApiExecutionContext context, User assigner, String userArg, String userRole,
            Map<String, Object> args) throws RSuiteException {
        User user = null;
        if (!StringUtils.isBlank(userArg)) {
            Object o = args.get(userArg);
            if (o == null || StringUtils.isBlank(o.toString())) {
                throw new RSuiteException(0, getUserRoleName() + " not specified");
            }
            String userId = o.toString();
            AuthorizationService authSvc = context.getAuthorizationService();
            LocalUserManager luMgr = authSvc.getLocalUserManager();

            user = luMgr.getUser(userId);
            if (user == null) {
                throw new RSuiteException(0, getUserRoleName() + " \"" + userId + "\" does not exist in local user store");
            }
            if (StringUtils.isBlank(user.getEmail())) {
                log.warn(getUserRoleName() + " \"" + userId + "\" has no email address");
            }

        } else {
            user = assigner;
        }

        args.put("rsuiteUserId", user.getUserId());
        args.put("rsuiteUserFullName", user.getFullName());
        args.put("rsuiteUserName", user.getFullName());
        args.put("rsuiteUserEmailAddress", user.getEmail());
        args.put("rsuiteUserEmail", user.getEmail());

        Role[] roles = user.getRoles();
        if ((userRole != null) && roles != null && roles.length > 0) {
            String roleName = roles[0].getName();
            for (Role role : roles) {
                if (role.getName().equals(userRole)) {
                    roleName = role.getName();
                    break;
                }
            }
            args.put("rsuiteUserRole", roleName);
        } else {
            args.put("rsuiteUserRole", userRole);
        }

        // Add assigner information also
        args.put("assignerUserId", assigner.getUserId());
        args.put("assignerUserName", assigner.getFullName());
        args.put("assignerEmail", assigner.getEmail());
    }

    // ///////////////////////////////////////////////////////////////////////
    // Private Section
    // ///////////////////////////////////////////////////////////////////////
}
