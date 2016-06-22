package com.rsicms.community.quickTask.webservice;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import com.reallysi.rsuite.api.Basket;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiDefinition;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;

/**
 * Base class for WKH web services.
 */
public abstract class BaseRemoteApiHandler implements RemoteApiHandler {
    private static Log log = LogFactory.getLog(BaseRemoteApiHandler.class);

    /**
     * Old misspelled initialization method for older versions of RSuite.
     */
    public void initalize(RemoteApiDefinition def) {
        initialize(def);
    }

    /**
     * Initialize handler.
     */
    public void initialize(RemoteApiDefinition def) {
        // noop
    }

    /**
     * Process request.
     * @param   context         Execution context.
     * @param   args            Service parameters provided by client.
     */
    public abstract RemoteApiResult execute(
            RemoteApiExecutionContext context,
            CallArgumentList args
    ) throws RSuiteException;

    /**
     * Log service arguments.
     * @param   args        Argument list.
     */
    protected void logArgs(CallArgumentList args) {
        log.info("Remote API Arguments:");
        for (String name : args.getNames()) {
            log.info("["+name+"] "+args.getValues(name));
        }
    }

    protected static Map<String, Object> copyArguments(
    	    CallArgumentList args) {
    	Map<String, Object> parameters = new HashMap<String, Object>();
    	Set<String> names = args.getNames();
    	for (String name : names)
    	{
    		parameters.put(name, args.getValues(name));
    	}

    	return parameters;
    }
    
    /**
     * Retrieve the list IDs from context.
     * @param   user        Effective user.
     * @param   context     Execution context.
     * @param   args        Map of arguments to get IDs from.
     * @return  String array of IDs.
     * @throws RSuiteException if a system error occurs.
     */
    protected static String[] getIdList(
            User user,
            RemoteApiExecutionContext context,
            Map<String,Object> args
    ) throws RSuiteException
    {
        String s = getStringVariable(
                args, "rsuiteViewContext", "");
        if (StringUtils.isBlank(s)) {
            // The following is what used to exist in previous
            // RCs of 3.5 to determine if clipboard context.
            s = getStringVariable(args, "view", "");
        }
        if ("clipboard".equals(s)) {
            Basket b = context.getBasketService().getOrCreateClipboard(user);
            return b.getObjectIdList();
        }
        s = getStringVariable(args, "rsuiteId", null);
        if (!StringUtils.isBlank(s)) {
            return new String[] { s };
        }
        return null;
    }
    
    /**
     * Retrieve user that invoked this service.
     * @param   context   Execution context.
     * @return  User instance.
     */
    public User getUser(RemoteApiExecutionContext context) {
        User u = context.getSession().getUser();
        return u;
    }
            
    /**
     * Retrieve object ID associated with this service request.
     * @param   context     Execution context.
     * @param   args        Map of arguments to get ID from.
     * @return  ID string or <tt>null</tt> if no ID provided.
     */
    public String getObjectId(
            RemoteApiExecutionContext context,
            CallArgumentList args) {
        return args.getFirstValue("rsuiteId");
    }
    
    /**
     * Retrieve object alias associated with this service request.
     * @param   context     Execution context.
     * @param   args        Map of arguments to get ID from.
     * @return  ID string or <tt>null</tt> if no ID provided.
     */
    public String getObjectAlias(
            RemoteApiExecutionContext context,
            CallArgumentList args) {
        return args.getFirstValue("alias");
    }

    /**
     * Retrieve string value from string map.
     * @param   map     String map.
     * @param   name    Name of key to get value of.
     * @param   defVal  Default value if key is not defined.
     */
    public static String getStringVariable(
            Map<String,? extends Object> map,
            String name,
            String defVal
    )
    {
        Object o = map.get(name);
        if (o == null) return defVal;
        return o.toString();
    }
    
    /**
     * Retrieve session key associated with this require.
     * @param context   Execution context.
     * @return  Session key string.
     */
	protected String getSessionKey(
	        RemoteApiExecutionContext context
	) {
		return context.getSession().getKey();
	}
	
}
