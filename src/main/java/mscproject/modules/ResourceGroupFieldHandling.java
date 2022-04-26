package mscproject.modules;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.opensymphony.module.propertyset.PropertySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

@Scanned
public class ResourceGroupFieldHandling extends AbstractJiraCondition
{
    private static final Logger log = LoggerFactory.getLogger(ResourceGroupFieldHandling.class);

    public static final String FIELD_WORD = "ResourceGroup";

    public boolean passesCondition(Map transientVars, Map args, PropertySet ps)
    {
        String word = (String)transientVars.get(FIELD_WORD);
        Issue issue = getIssue(transientVars);
        String description = issue.getDescription();

        // Makes sure that the field exists
        return description != null && description.contains(word);
    }
}
