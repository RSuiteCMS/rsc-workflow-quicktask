package com.rsicms.community.quickTask.datatype;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.DataTypeOptionValue;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.forms.DataTypeProviderOptionValuesContext;
import com.reallysi.rsuite.api.forms.DefaultDataTypeOptionValuesProviderHandler;
import com.reallysi.rsuite.service.AuthorizationService;

public class RoleListProvider extends DefaultDataTypeOptionValuesProviderHandler {
	
	private Log log = LogFactory.getLog(RoleListProvider.class);
	
 	public void provideOptionValues(
			 DataTypeProviderOptionValuesContext context,
			 List<DataTypeOptionValue> optionValues) throws RSuiteException {

		log.debug("provideOptionValues(): Looking for datatype for \"" + dataType.getName() + "\"");
		AuthorizationService authSvc = context.getAuthorizationService();
		String[] roles = authSvc.getAllRoles();
		for ( String role: roles) {
			optionValues.add( new DataTypeOptionValue( role, role));
		}
 	}
}
