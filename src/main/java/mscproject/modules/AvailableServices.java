package mscproject.modules;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.customfields.impl.MultiSelectCFType;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.search.SearchContextImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import helpers.MongoApi;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import helpers.MongoRun;
import helpers.MongoApi;
import org.springframework.beans.factory.annotation.Autowired;

@Scanned
public class AvailableServices extends MultiSelectCFType {
    @Autowired
    public AvailableServices(@ComponentImport CustomFieldValuePersister customFieldValuePersister, @ComponentImport OptionsManager optionsManager, @ComponentImport GenericConfigManager genericConfigManager, @ComponentImport JiraBaseUrls jiraBaseUrls, @ComponentImport SearchService searchService, @ComponentImport FeatureManager featureManager, @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
        super(optionsManager, customFieldValuePersister, genericConfigManager, jiraBaseUrls, searchService, featureManager, jiraAuthenticationContext);

        this.optionsManager = optionsManager;

        this.init();
    }

    static boolean serviceIsAllowed(String service) {
        ArrayList<String> disallowedServices = new ArrayList<>();
        disallowedServices.add("new rg");

        for (String serviceName : disallowedServices) {
            if(service.contains(serviceName)) {
                return false;
            }
        }

        return true;
    }

    static String[] servicesAllowed(String[] services) {
        ArrayList<String> toReturn = new ArrayList<>();

        for(String service : services) {
            if (serviceIsAllowed(service)) {
                toReturn.add(service);
            }
        }

        return toReturn.toArray(new String[toReturn.size()]);
    }

    @Override
    public Map<String, Object> getVelocityParameters(final Issue issue,
                                                     final CustomField field,
                                                     final FieldLayoutItem fieldLayoutItem) {
        final Map<String, Object> parameters = super.getVelocityParameters(issue, field, fieldLayoutItem);

        // This method is also called to get the default value, in
        // which case issue is null so we can't use it to add currencyLocale
        if (issue == null) {
            return parameters;
        }

        FieldConfig fieldConfiguration = field.getRelevantConfig(issue);

        //add what you need to the map here
        String[] availableServices = _dmapi.GetAvailableServices();
        String[] allowedServices = AvailableServices.servicesAllowed(availableServices);

        String[] selectedServices = _dmapi.GetSelectedServices(issue.toString());
        boolean disabledSelection = false;
        if (selectedServices.length > 0) {
            disabledSelection = true;
        }

        // Delete all options added by the UI, deleting immediately ruins the iterator
        Options options = this.optionsManager.getOptions(fieldConfiguration);
        HashMap<String, Option> existingOptions = new HashMap<>();

        for (Option option : (Iterable<Option>) options) {
            existingOptions.put(option.getValue(), option);
        }

        HashMap<String, Long> optionsMap = new HashMap<>();
        long sequence = 0;
        for (String service : allowedServices) {
            Option option = this.optionsManager.createOption(fieldConfiguration, null, sequence, service);
            sequence += 1;
            optionsMap.put(service, option.getOptionId());
        }

        parameters.put("optionsMap", optionsMap);
        parameters.put("availableServices", allowedServices);
        parameters.put("selectedServices", selectedServices);
        parameters.put("disabledSelection", disabledSelection);

        return parameters;
    }

    private void init() {
        try {
            _dmapi = new MongoRun("localhost", 27017);
            System.out.println("Connected into MongoDB");
        } catch (Exception e) {
            System.out.println("Failed to connect into MongoDB");
        }
    }

    private final OptionsManager optionsManager;
    private MongoApi _dmapi;
}
