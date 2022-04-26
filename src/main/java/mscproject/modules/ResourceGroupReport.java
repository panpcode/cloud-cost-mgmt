package mscproject.modules;

import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.web.action.ProjectActionSupport;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.util.JSON;
import helpers.GraphHelper;
import helpers.MongoApi;
import helpers.MongoRun;
import org.jgrapht.Graph;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResourceGroupReport extends AbstractReport {

    public String generateReportHtml(ProjectActionSupport projectActionSupport, Map params) throws Exception {
        String azureUrlTemplate = "https://portal.azure.com/?nonceErrorSeen=true#@COMPANY_NAME.onmicrosoft.com/resource/subscriptions/SUBSCRIPTION_ID/resourceGroups/RG_GROUP/overview";

        String graph_data = GraphHelper.getJsonDependencies();

        String[] data_array = null;
        try {
            MongoApi dmapi = new MongoRun("localhost", 27017);
            System.out.println("Initialized the Mongo DB object, will now request graph data");
            data_array = dmapi.GetAllJira();
            System.out.println("Successfully got dependency graph");
        }catch(Exception e) {
            System.out.println("Successfully read RG names");
        }

        ArrayList<JSONObject> table_data = new ArrayList();
        for (String row : data_array) {
            try {
                JSONObject rowObj = new JSONObject(row);

                String resource_group = rowObj.getString("rg_name");
                rowObj.put("url", azureUrlTemplate.replace("RG_GROUP", resource_group));

                java.util.Date start = new java.util.Date(Long.parseLong(rowObj.getString("start_time")));
                java.util.Date end = new java.util.Date(Long.parseLong(rowObj.getString("start_time")));
                rowObj.put("start", start.toString());
                rowObj.put("end", end.toString());

                if (rowObj.getString("cost") == null) {
                    rowObj.put("cost", "N/A");
                }

                table_data.add(rowObj);
            }catch(Exception ex) {
            }
        }

        params.put("data", table_data.toArray());
        params.put("graph_data", graph_data);

        return descriptor.getHtml("view", params);
    }
}
