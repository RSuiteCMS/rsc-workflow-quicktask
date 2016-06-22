package com.rsicms.community.quickTask.formhandler;

import static com.rsicms.pluginUtilities.FormsUtils.addFormHiddenParameter;
import static com.rsicms.pluginUtilities.FormsUtils.addFormSelectTypeParameter;
import static com.rsicms.pluginUtilities.FormsUtils.addFormSubmitButtonsParameter;
import static com.rsicms.pluginUtilities.FormsUtils.addFormTextAreaParameter;
import static com.rsicms.pluginUtilities.FormsUtils.addFormTextParameter;
import static com.rsicms.pluginUtilities.FormsUtils.addFormDateParameter;
import static com.rsicms.pluginUtilities.FormsUtils.allowMultiple;
import static com.rsicms.pluginUtilities.FormsUtils.dontAllowMultiple;
import static com.rsicms.pluginUtilities.FormsUtils.notReadOnly;
import static com.rsicms.pluginUtilities.FormsUtils.notRequired;
import static com.rsicms.pluginUtilities.FormsUtils.nullDataType;
import static com.rsicms.pluginUtilities.FormsUtils.nullDataTypeOptions;
import static com.rsicms.pluginUtilities.FormsUtils.nullValidationMessage;
import static com.rsicms.pluginUtilities.FormsUtils.nullValidationRegex;
import static com.rsicms.pluginUtilities.FormsUtils.nullValue;
import static com.rsicms.pluginUtilities.FormsUtils.nullValues;
import static com.rsicms.pluginUtilities.FormsUtils.required;
import static com.rsicms.pluginUtilities.FormsUtils.sortAscending;
import static com.rsicms.pluginUtilities.FormsUtils.sortNoSort;
import static com.rsicms.pluginUtilities.datatype.UserListProvider.populateOptions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.DataTypeOptionValue;
import com.reallysi.rsuite.api.FormControlType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.forms.DefaultFormHandler;
import com.reallysi.rsuite.api.forms.FormColumnInstance;
import com.reallysi.rsuite.api.forms.FormDefinition;
import com.reallysi.rsuite.api.forms.FormInstance;
import com.reallysi.rsuite.api.forms.FormInstanceCreationContext;
import com.reallysi.rsuite.api.forms.FormParameterInstance;
import com.reallysi.rsuite.api.remoteapi.CallArgument;

/**
 * Populate form for quick task set up 
 * 
 */
public class AdviseQtFormHandler extends DefaultFormHandler {
	private static Log log = LogFactory.getLog(AdviseQtFormHandler.class);

	public void initialize(FormDefinition formDefinition) {
	}

	@Override
	public void adjustFormInstance(FormInstanceCreationContext context, FormInstance formInstance) throws RSuiteException {
		log.info("AdviseQtFormHandler(): Returned arguments are: ");
		for (CallArgument arg : context.getArgs().getAll()) {
			log.info("  " + arg.getName() +  " = " + arg.getValue());
		}

		String roleNames = context.getArgs().getFirstString("roleNames");
		log.info("AdviseQtFormHandler(): roleNames provided by context menu item = " + roleNames);
		String descriptionOptions = context.getArgs().getFirstString("descriptionOptions");
		log.info("AdviseQtFormHandler(): descriptionOptions provided by context menu item = " + descriptionOptions);
		if (roleNames==null) {
			roleNames = "";
		}
		if (descriptionOptions==null) {
			descriptionOptions = "Review,Edit,Approve";
		}

        String selectedSendToUser = context.getArgs().getFirstString("selectedSendToUser");
        String selectedDescriptionList = context.getArgs().getFirstString("selectedDescriptionList");
        String selectedInstructions = context.getArgs().getFirstString("selectedInstructions");
        String selectedDueDate = context.getArgs().getFirstString("selectedDueDate");
        String selectedSendEmail = context.getArgs().getFirstString("selectedSendEmail");

        if (selectedSendToUser != null) {
        	formInstance.setInstructions("You must enter an assignment if \"Other\" is selected.");
        }
        
        List<FormColumnInstance> cols = new ArrayList<FormColumnInstance>();

		List<FormParameterInstance> params = new ArrayList<FormParameterInstance>();

		List<DataTypeOptionValue> userValues = new ArrayList<DataTypeOptionValue>();
		userValues.add(new DataTypeOptionValue ( "", "(Any user)"));
		populateOptions( context, userValues, "", false);
		
		addFormSelectTypeParameter( FormControlType.SELECT, params, "sendTo", "Assign to", nullDataType, userValues, new String[]{selectedSendToUser}, sortNoSort, dontAllowMultiple, notRequired, notReadOnly);

		List<DataTypeOptionValue> roleBeforeValues = new ArrayList<DataTypeOptionValue>();
		roleBeforeValues.add( new DataTypeOptionValue ( "", "None"));

		addFormSelectTypeParameter( FormControlType.SELECT, params, 
                                    "sendToRole", "Assign to Team", 
                                    "rsuite:roles", roleBeforeValues, nullValues, 
                                    sortNoSort, allowMultiple, notRequired, notReadOnly);
		
		List<DataTypeOptionValue> descOptions = new ArrayList<DataTypeOptionValue>();
		for (String descriptionOption : descriptionOptions.split(",")) {
			descOptions.add(new DataTypeOptionValue (descriptionOption.trim(), descriptionOption.trim()));
		}
		descOptions.add(new DataTypeOptionValue ("Other", "Other (enter below)"));
		addFormSelectTypeParameter(FormControlType.SELECT, params, "taskDescriptionList", "Assignment", nullDataType, descOptions, new String[]{selectedDescriptionList}, sortNoSort, dontAllowMultiple, required, notReadOnly);

		addFormTextParameter(params, "taskDescription", "", nullDataType, nullDataTypeOptions, nullValue, notRequired, nullValidationRegex, nullValidationMessage, notReadOnly);

		addFormTextAreaParameter(params, "taskInstructions", "Details", selectedInstructions, required, nullValidationRegex, "This field is required.", notReadOnly);

		addFormDateParameter(params, "dueDate", "Due Date (optional)", selectedDueDate, notRequired, notReadOnly);
		
		List<DataTypeOptionValue> checkBoxOptions = new ArrayList<DataTypeOptionValue>();
		checkBoxOptions.add(new DataTypeOptionValue ("yes", "Send email"));
		String checked = "yes";
		if (selectedSendEmail == null || selectedSendEmail.equals("no")) {
			checked = "no";
		}
		addFormSelectTypeParameter(FormControlType.CHECKBOX, params, "sendEmail", "Notify by email? (Does not apply to tasks assigned to \"Any User\".)", nullDataType, checkBoxOptions, new String[]{checked}, sortAscending, dontAllowMultiple, notRequired, notReadOnly);

		addFormHiddenParameter(params, "roleNames", roleNames);

		FormColumnInstance fci = new FormColumnInstance();
		fci.addParams(params);

		
		List<FormParameterInstance> controlsParams = new ArrayList<FormParameterInstance>();
		List<DataTypeOptionValue> submitButtonValues = new ArrayList<DataTypeOptionValue>();
		submitButtonValues.add(new DataTypeOptionValue("submit", "OK"));
		submitButtonValues.add(new DataTypeOptionValue("cancel", "Cancel"));
		addFormSubmitButtonsParameter(controlsParams, "submitButton", submitButtonValues);
		
		FormColumnInstance controlsFci = new FormColumnInstance();
		controlsFci.addParams(controlsParams);
		controlsFci.setName("controls");
		
		cols.add(fci);
		cols.add(controlsFci);

		formInstance.setColumns(cols);
	}

}
