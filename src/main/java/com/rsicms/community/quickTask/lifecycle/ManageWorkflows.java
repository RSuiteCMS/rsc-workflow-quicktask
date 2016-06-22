package com.rsicms.community.quickTask.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ElementMatchingCriteria;
import com.reallysi.rsuite.api.LayeredMetadataDefinition;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.Plugin;
import com.reallysi.rsuite.api.extensions.PluginLifecycleListener;
import com.rsicms.pluginUtilities.WorkflowUtility;
import com.rsicms.pluginUtilities.types.ElementTypeMatchingCriteria;
import com.rsicms.teamEdition.TEWorkflowConstants;

public class ManageWorkflows implements PluginLifecycleListener  {
	
    Log log = LogFactory.getLog(ManageWorkflows.class);

    @Override
	public void start(ExecutionContext context, Plugin plugin) {
		WorkflowUtility workflowUtility = new WorkflowUtility(context, plugin);
		try {
			workflowUtility.importAllWorkflows("workflows");
            
            createLmdDef(context, TEWorkflowConstants.STATUS_LMD);
            createLmdDef(context, TEWorkflowConstants.WORKFLOW_DETAILS_LMD);

		} catch (RSuiteException e) {
			e.printStackTrace();
		}
		
	}

    private void createLmdDef(ExecutionContext context, String lmdName) throws RSuiteException {
        log.info("Creating metadata definition for " + lmdName + "...");
        User systemUser = context.getAuthorizationService().getSystemUser();
        
        List<ElementMatchingCriteria> elements = new ArrayList<ElementMatchingCriteria>();
        elements.add(ElementTypeMatchingCriteria.createForElementType(null, "rs_ca"));
        elements.add(ElementTypeMatchingCriteria.createForElementType(null, "rs_canode"));
        elements.add(ElementTypeMatchingCriteria.createForElementType(null, "nonxml"));
//        This code causing start to hang up. Figure it out another time. 
//        Collection<SchemaInfo> schemaList = context.getSchemaService().getSchemaInfoValues();
//        for (SchemaInfo schemaInfo : schemaList) {
//            List<SchemaElementTypeInfo> elementList = context.getSchemaService().getElementTypes(schemaInfo.getSchemaId());
//            for (SchemaElementTypeInfo elementType : elementList) {
//                ElementTypeMatchingCriteria elementCriteria = ElementTypeMatchingCriteria.createForElementType(null, elementType.getLocalName());
//                if (!elements.contains(elementCriteria)) {
//                    elements.add(elementCriteria);
//                }
//            }
//        }
        
        LayeredMetadataDefinition lmdDef = context.getMetaDataService().getLayeredMetaDataDefinition(systemUser, lmdName);
        if (lmdDef == null) {
            List<String> values = new ArrayList<String>();
            context.getMetaDataService().createLayeredMetaDataDefinition(
                    systemUser,
                    lmdName,
                    "string", 
                    false,
                    true,
                    true,
                    elements,
                    values.toArray(new String[0])
                );
                context.getMetaDataService().setLayeredMetaDataDefinitionElementCriteria(systemUser, lmdName, elements);
        }
    }

	@Override
	public void stop(ExecutionContext context, Plugin plugin) {}

}
