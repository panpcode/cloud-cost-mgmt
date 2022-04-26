package helpers;

import helpers.MongoApi;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.json.JSONObject;
import java.util.List;

public class DummyMongoApi implements MongoApi{

    public String[] getRG(){

        String[] toreturn=new String[4];
        toreturn[0]="RG1";
        toreturn[1]="RG2";
        toreturn[2]="RG3";
        toreturn[3]="RG4";
        return toreturn;
    }

    public void updateJira(String JiraId , String rgName, JiraStatus status){
        if (status == JiraStatus.Started){
            System.out.println("Updating "+JiraId+" , will use azure rg : "+rgName +" and the expected behaviour is : start");
        }
        else{
            System.out.println("Updating "+JiraId+" , will use azure rg : "+rgName +" and the expected behaviour is : stopped");
        }
    }
    public Graph<String, LabeledEdge> createGraph(){
        return null;
    }
    public JSONObject GetJira(String JiraId ){return null;};
    public String[] GetAllJira(){return null;};
    public String[] GetAvailableServices() { return null; };
    public String[] GetSelectedServices(String issue) { return null; };
    public void addInServicesRgQueue(String resourceGroupName, List<String> serviceNames) { };
    public boolean doesServiceRgQueueExists(String issue) { return true; };
    public void startServiceRgQueue(String issue) {};
}
