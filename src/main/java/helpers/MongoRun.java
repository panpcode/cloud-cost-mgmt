package helpers;

import helpers.MongoApi;
import helpers.MongoConnector;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.json.JSONObject;

import org.bson.Document;

import java.util.List;

public class MongoRun implements MongoApi {

    private String MongoDBIp;
    private int MongoDBPort;
    MongoConnector myconnector;

    public MongoRun(String Ip , int port) {
        MongoDBIp = Ip;
        MongoDBPort = port;
        myconnector = new MongoConnector( Ip, port );
    }

    public String[] getRG() {
        return myconnector.getAllRGNames();
    }

    public void updateJira(String JiraId, String rgName, JiraStatus status) {
        myconnector.updateJira(JiraId, rgName, status);
    }

    public Graph<String, LabeledEdge> createGraph() {
        return myconnector.createGraph();
    }

    public JSONObject GetJira(String JiraId){
        return myconnector.getJira(JiraId);
    }

    public  String[] GetAllJira() {
        return myconnector.GetAllJira();
    }

    public String[] GetAvailableServices() { return myconnector.GetAllAvailableServices(); }

    public String[] GetSelectedServices(String issue) { return myconnector.GetSelectedServices(issue); }

    public void addInServicesRgQueue(String resourceGroupName, List<String> serviceNames) {
        myconnector.addInServicesRgQueue(resourceGroupName, serviceNames);
    }

    public boolean doesServiceRgQueueExists(String issue) {
        return myconnector.doesServiceRgQueueExists(issue);
    }

    public void startServiceRgQueue(String issue) {
        myconnector.startServiceRgQueue(issue);
    }
}
