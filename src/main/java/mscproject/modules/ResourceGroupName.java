package mscproject.modules;

import com.atlassian.jira.issue.customfields.impl.SelectCFType;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.search.SearchContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import java.util.ArrayList;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import helpers.DummyMongoApi;
import helpers.MongoRun;
import helpers.MongoApi;

@Scanned
public class ResourceGroupName extends SelectCFType{
    private static final Logger log = LoggerFactory.getLogger(ResourceGroupName.class);
    private final OptionsManager optionsManager;

    @Autowired
    public ResourceGroupName(@ComponentImport CustomFieldValuePersister customFieldValuePersister,@ComponentImport OptionsManager optionsManager,@ComponentImport GenericConfigManager genericConfigManager,@ComponentImport JiraBaseUrls jiraBaseUrls) {
        super(customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls);
        this.optionsManager = optionsManager;
    }

    static boolean rgIsAllowed(String rg_name){
        ArrayList<String> allowed_rgs=new ArrayList<>();

        allowed_rgs.add("MSCJIR-*");

        boolean has_an_allowed_part=false;
        for(String allowed_part : allowed_rgs){
            if(rg_name.contains(allowed_part)){
                return true;
            }
        }
        return false;
    }


    static String[] rgsAllowed(String[] rgs){
        ArrayList<String> toreturn=new ArrayList<>();
        for(String rg_name:rgs){
            if(rgIsAllowed(rg_name)){
                toreturn.add(rg_name);
            }
        }
        return toreturn.toArray(new String[toreturn.size()]);
    }

    @Override
    public Map<String, Object> getVelocityParameters(final Issue issue,
                                                     final CustomField field,
                                                     final FieldLayoutItem fieldLayoutItem) {
        final Map<String, Object> parameters = super.getVelocityParameters(issue, field, fieldLayoutItem);

        if (issue == null) {
            return parameters;
        }

        // Retrieving all the resource group names from the Mongo Database
        String[] rg_names=null;
        HashMap<String,Boolean> allowed_options=new HashMap<>();
        try {
            MongoApi dmapi = new MongoRun("localhost", 27017);
            log.info("Initialized the Mongo DB object. Requesting data ...");
            rg_names = dmapi.getRG();
            // System.out.println(">>>>>>>>> RGSSSSS " + rg_names);
            rg_names=ResourceGroupName.rgsAllowed(rg_names); // input validation
            Arrays.sort(rg_names);
            for (String rg_name : rg_names){
                if (rg_name != null){
                    allowed_options.put(rg_name,true);
                    System.out.println(">>>> Allowed rg " + rg_name);
                }
            }
            log.info("Successfully read all the Resource Group names from Mongo DB...");
        }catch(Exception e) {
            log.error("Failed to retrieve Resource Group named from MongoDB because of error: "+e.toString());
            e.printStackTrace();
        }

        FieldConfig fieldConfiguration = null;
        if(issue == null)
        {
            fieldConfiguration = field.getReleventConfig(new SearchContextImpl());
        } else
        {
            fieldConfiguration = field.getRelevantConfig(issue);
        }

        // Delete all options added by the UI, deleting immediately ruins the iterator
        Options options = this.optionsManager.getOptions(fieldConfiguration);
        HashMap<String,Option> existing_options = new HashMap<>();

        for (Option option : (Iterable<Option>) options) {
            if(option != null){
                String value = option.getValue();
                if(allowed_options.get(value)== null || !allowed_options.get(value)) {
                    this.optionsManager.deleteOptionAndChildren(option);
                }else{
                    existing_options.put(option.getValue(),option);
                }
            }
        }

        HashMap<String, Long> optionsMap = new HashMap<>();
        long sequence = 0;
        for(String rg_name : rg_names) {
            if(existing_options.get(rg_name)==null && allowed_options.get(rg_name)!=null) {// Newly appeared option
                Option option = this.optionsManager.createOption(fieldConfiguration, null, sequence, rg_name);
                optionsMap.put(rg_name, option.getOptionId());
            }else{
                Option option = existing_options.get(rg_name);
                if(option !=null){// Existing option
                    optionsMap.put(rg_name, option.getOptionId());
                }else{
                    // Not allowed option
                }
            }
        }


        // Get the current value of the field
        Boolean selected = false;
        Object value = field.getValue(issue);
        if (value!=null) {
            selected=true;
        }

        // Check which option is selected
        String selectedRG = "";

        for (Option option : (Iterable<Option>) options) {
            if (selected && value.toString().equals(option.getValue())) {
                selectedRG = option.getValue();
            }
        }

        
        // Report these to paramters so that the .vm files can read em
        parameters.put("availableRGs", rg_names);
        parameters.put("rgsMap", optionsMap);
        parameters.put("selectedRG", selectedRG);
        
        return parameters;
    }
}
