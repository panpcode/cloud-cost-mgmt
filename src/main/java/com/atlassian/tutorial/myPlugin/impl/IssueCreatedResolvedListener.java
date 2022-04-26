package com.atlassian.tutorial.myPlugin.impl;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.component.ComponentAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import helpers.DummyMongoApi;
import helpers.MongoRun;
import helpers.MongoApi;
import helpers.JiraStatus;

@Component
public class IssueCreatedResolvedListener implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(IssueCreatedResolvedListener.class);

    @JiraImport
    private final EventPublisher eventPublisher;

    @Autowired
    public IssueCreatedResolvedListener(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Enabling plugin");
        eventPublisher.register(this);
    }

    /**
     * Called when the plugin is being disabled or removed.
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        log.info("Disabling plugin");
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();

        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        CustomField customField = customFieldManager.getCustomFieldObjectByName("Resource Group Name");
        CustomField availableServices = customFieldManager.getCustomFieldObjectByName("Available Services");
        // System.out.println(">>>>>>> Field" +customField);
        // System.out.println(">>>>>>> Services" +availableServices);

        List<CustomField> fields = customFieldManager.getCustomFieldObjects(issue);
        for(CustomField field : fields){
           System.out.println("Field found : "+field.toString());
        }

        if (customField == null) {
            System.out.println("Failed to get the field");
            return;
        }

        // System.out.println(">>>>>>> 3");
        Object rg_object = issue.getCustomFieldValue(customField);
        String rg_name = null;
        if (rg_object != null) {
            rg_name = rg_object.toString();
        }

        List<Option> serviceNamesOptions = (List<Option>) availableServices.getValue(issue);
        List<String> serviceNames = new ArrayList<>();
        for (Option service : serviceNamesOptions) {
            if (rg_object == null) {
                serviceNames.add(service.toString());
            }
        }

        System.out.println(">>>>>>> 14");

        MongoApi dmapi = new MongoRun("localhost", 27017);

        System.out.println(">>>>>>> 15");

        String status_string = issue.getStatus().getSimpleStatus().getName();

        System.out.println("Status : " + status_string);

        System.out.println(">>>>>>> 16");

        boolean serviceRgQueueExists = dmapi.doesServiceRgQueueExists(issue.toString());

        // JIRA_ID-testing-nor

        System.out.println(">>> RG_NAME: " + rg_name);
        switch (status_string) {
            case "Open":
            case "Approved":
            case "Under Review":
                if (rg_name != null) {
                    dmapi.updateJira(issue.toString(), rg_name, JiraStatus.Stopped);
                    System.out.println(">>>>>>> Stopping rg " + rg_name + " for issue " + issue.toString());
                }
                break;
            case "In Progress":
                System.out.println(">>> In progress: " + rg_name);
                if (rg_name != null) {
                    dmapi.updateJira(issue.toString(), rg_name, JiraStatus.Started);
                    System.out.println(">>>>>>> Starting rg " + rg_name + " for issue " + issue.toString());
                } else {
                    System.out.println(">>> startServiceRgQueue: " + issue.toString());
                    dmapi.startServiceRgQueue(issue.toString());
                }
                break;
            case "To Do":
                if (rg_name != null) {
                    dmapi.updateJira(issue.toString(), rg_name, JiraStatus.Stopped);
                    System.out.println(">>>>>>> Stopping rg " + rg_name + " for issue " + issue.toString());
                } else {
                    if (!serviceRgQueueExists && serviceNames.size() > 0) {
                        dmapi.updateJira(issue.toString(), "testing-eastus-" + issue.toString() + "-rg", JiraStatus.Stopped);
                        System.out.println(">>>>>>> Adding into services queue " + serviceNames + " for issue " + issue.toString());
                        dmapi.addInServicesRgQueue(issue.toString(), serviceNames);
                    }
                }
            default:
                if (rg_name != null) {
                    dmapi.updateJira(issue.toString(), rg_name, JiraStatus.Stopped);
                    System.out.println(">>>>>>> Stopping rg " + rg_name + " for issue " + issue.toString());
                }
                break;
        }

        System.out.println(">>>>>>> 17");
    }
}
