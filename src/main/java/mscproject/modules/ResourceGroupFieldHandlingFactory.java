package mscproject.modules;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginConditionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ConditionDescriptor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

/**
 * This is the factory class responsible for dealing with the UI for the post-function.
 * This is typically where you put default values into the velocity context and where you store user input.
 */

@Scanned
public class ResourceGroupFieldHandlingFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginConditionFactory
{
    public static final String FIELD_WORD = "ResourceGroup";

    protected void getVelocityParamsForInput(Map velocityParams)
    {
        //the default message
        velocityParams.put(FIELD_WORD, "test1");
        velocityParams.put(FIELD_WORD, "test2");
        velocityParams.put(FIELD_WORD, "test3");
        velocityParams.put(FIELD_WORD, "test4");
        velocityParams.put(FIELD_WORD, "test5");
    }

    protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor)
    {
        getVelocityParamsForInput(velocityParams);
        getVelocityParamsForView(velocityParams, descriptor);
    }

    protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor)
    {
        if (!(descriptor instanceof ConditionDescriptor))
        {
            throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
        }

        ConditionDescriptor conditionDescriptor = (ConditionDescriptor)descriptor;
        velocityParams.put("", conditionDescriptor.getArgs().get(FIELD_WORD));
    }

    public Map getDescriptorParams(Map conditionParams)
    {
        // Process The map
        Map params = new HashMap();
        params.put(FIELD_WORD, extractSingleParam(conditionParams, FIELD_WORD));
        return params;
    }
}
