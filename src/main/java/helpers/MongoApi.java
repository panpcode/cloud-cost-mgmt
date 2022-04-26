package helpers;

import helpers.JiraStatus;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.json.JSONObject;
import java.util.List;

public interface MongoApi {
    public String[] getRG();
    public JSONObject GetJira(String JiraId );
    public String[] GetAllJira();
    public void updateJira(String JiraId , String rgName, JiraStatus status);
    public Graph<String, LabeledEdge> createGraph();
    public String[] GetAvailableServices();
    public String[] GetSelectedServices(String issue);
    public void addInServicesRgQueue(String resourceGroupName, List<String> serviceNames);
    public boolean doesServiceRgQueueExists(String issue);
    public void startServiceRgQueue(String issue);
}
