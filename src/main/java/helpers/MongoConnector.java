package helpers;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class MongoConnector {

    private MongoDatabase Mscproject_database;
    private MongoCollection<Document> JiraCollection;
    private MongoCollection<Document> RGCollection;
    private MongoCollection<Document> AvailableServicesCollection;
    private MongoCollection<Document> ServicesRgQueues;

    private String MongoDBIp;
    private int MongoDBPort;

    private boolean IsConnected = false;

    public MongoConnector(String Ip , int port) {
        IsConnected = false;
        MongoDBIp = Ip;
        MongoDBPort = port;
        connect();
    }

    public void connect(){
        if (IsConnected) {
            return;
        }

        String database = "test";        // the name of the database in which the user is defined
        //String user="panos";     // the user name
        //char[] password ={ 'p', 'a', 'p', 'a', 't', 'h', 'a', 'n', 'a', 's' };
        String user="panos";     // the user name
        char[] password =  "papathanas".toCharArray();
        MongoCredential credential = MongoCredential.createCredential(user,
                database,
                password);

        MongoClient mongoClient = new MongoClient(new ServerAddress(MongoDBIp, MongoDBPort),
                Arrays.asList(credential));

        Mscproject_database = mongoClient.getDatabase("test");
        JiraCollection = Mscproject_database.getCollection("items");
        RGCollection = Mscproject_database.getCollection("rgs");
        AvailableServicesCollection = Mscproject_database.getCollection("available_services");
        ServicesRgQueues = Mscproject_database.getCollection("services_rg_queue");

        IsConnected = true;
    }


    /*------------------------------------------------------
    JIRA SUB CLASS
    --------------------------------------------------------*/

    /*------------------------------------------------------
    Return te total number of Jira Items
    --------------------------------------------------------*/

    public int getNumJiraItems(){
        connect();
        return (int)JiraCollection.count();
    }

    /*------------------------------------------------------
    Returns the Jira Item by JiraId or null if does not exists
    --------------------------------------------------------*/

    public Document getJiraItemById(String JiraId){
        connect();
        Document JiraItem = JiraCollection.find(eq("_id", JiraId)).first();
        return JiraItem;
    }

    /*------------------------------------------------------
    Returns the Jira Item by Resource Group or null if does not exists
    // Todo Make this a list since more than 1 can have the same RG.
    --------------------------------------------------------*/
    public Document getJiraItemByRG(String RGId){
        connect();
        Document JiraItem = JiraCollection.find(eq("rg_name", RGId)).first();
        return JiraItem;
    }


    /*------------------------------------------------------
    Returns the Jira Item by Resource Group or null if does not exists
    --------------------------------------------------------*/
    public Map<String, String> getAllJiraItemByRG(String RGId){
        connect();

        FindIterable<Document> JiracollectionResult = JiraCollection.find(eq("rg_name", RGId));

        Map<String, String> ReturnMap = new HashMap<>();

        int count = 0 ;
        MongoCursor<Document> cursor = JiracollectionResult.iterator();
        try {
            while (cursor.hasNext()) {
                JSONObject json = new JSONObject(cursor.next().toJson());
                String Id = json.getString("_id");
                String status = json.getString("status");
                ReturnMap.put(Id,status);
            }
        } finally {
            cursor.close();
        }
        return ReturnMap;

    }

    public JSONObject getJira(String JiraId){
        Document JiraItem=  getJiraItemById(JiraId);
        if (null == getJiraItemById(JiraId) )
        {
            return null;
        }
        JSONObject jsonResult = new JSONObject(JiraItem.toJson());
        return jsonResult;
    }


    /*------------------------------------------------------
    Return all Resource Groups in an array of String.
    --------------------------------------------------------*/




    public String[] GetAllJira() {
        int CollectionCount = getNumJiraItems();
        String[] ReturnString = new String[CollectionCount];
        int count=0;
        MongoCursor<Document> cursor = JiraCollection.find().iterator();
        try {
            while (cursor.hasNext()) {
                JSONObject json = new JSONObject(cursor.next().toJson());
                System.out.println(json);
                ReturnString[count++]=json.toString();

            }
        } finally {
            cursor.close();
        }
        System.out.println(ReturnString);
        return ReturnString;
    }



    private String getJiraStatus(Document JiraItem ) {
        JSONObject json = new JSONObject(JiraItem.toJson());
        return json.getString("status");
    }

    private String getJiraRG(Document JiraItem ) {
        JSONObject json = new JSONObject(JiraItem.toJson());
        return json.getString("rg_name");
    }

    private long getJiraTime(Document JiraItem ) {
        JSONObject json = new JSONObject(JiraItem.toJson());
        String mytime = json.getString("start_time");
        return Long.valueOf(mytime);
    }

    private long getJiraTotalDuration(Document JiraItem ) {
        JSONObject json = new JSONObject(JiraItem.toJson());
        String totalDuration = json.getString("total_duration");
        return Long.valueOf(totalDuration);
    }

    private void setJiraTotalDuration(String JiraId , long totalDuration ) {
        JiraCollection.updateOne(eq("_id", JiraId), set("total_duration", Long.toString(totalDuration)));
    }

    private void setJiraCurrentDuration(String JiraId , long currentDuration ) {
        JiraCollection.updateOne(eq("_id", JiraId), set("last_duration", Long.toString(currentDuration)));
    }

    private void setJiraStartTime(String JiraId , long now ) {
        JiraCollection.updateOne(eq("_id", JiraId), set("start_time", Long.toString(now)));
    }

    private void setJiraStopTime(String JiraId , long now ) {
        JiraCollection.updateOne(eq("_id", JiraId), set("stop_time", Long.toString(now)));
    }

    /*------------------------------------------------------
    Creates and returns a Jira Item
    args
    double JiraId
    --------------------------------------------------------*/

    public Document addJiraItem(String JiraId, String rgName, JiraStatus status){
        connect();
        if (null != getJiraItemById(JiraId) )
        {
            System.out.println("JiraId already exists No Action");
            return null;
        }
        String newStatus= "Start";
        if (status == JiraStatus.Stopped)
            newStatus= "Stop";

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Document doc = new Document("_id", JiraId)
                .append("rg_name", rgName)
                .append("status", "Stop")
                .append("start_time", Long.toString(timestamp.getTime()))
                .append("total_duration", "0")
                .append("last_duration", "0");
        JiraCollection.insertOne(doc);

        JiraCollection.updateOne(eq("_id", JiraId), set("status", newStatus));

        Document JiraItem = JiraCollection.find(eq("_id", JiraId)).first();
        return JiraItem;
    }


    public void updateJira(String JiraId, String rgName, JiraStatus status){
        // Search if Jira Item with JiraId already exists.
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Document currentJiraItem = getJiraItemById(JiraId);
        if (null != currentJiraItem){
            // Get the sting of the status.
            String newStatus= "Start";
            if (status == JiraStatus.Stopped)
                newStatus= "Stop";
            // Get current Jira status.
            String currentStatus = getJiraStatus(currentJiraItem ) ;
            if (currentStatus.equalsIgnoreCase("Stop") &&  status == JiraStatus.Started ) {
                // It was stopped and now it is started. Update the start field.
                setJiraStartTime(JiraId , timestamp.getTime());
            }
            if (currentStatus.equalsIgnoreCase("Start") &&  status == JiraStatus.Stopped ) {
                // It was started and now it is stopped. Add the number of used seconds.
                long start_time = getJiraTime(currentJiraItem);
                long totalDuration = getJiraTotalDuration(currentJiraItem);
                long currentDuration = timestamp.getTime() - start_time;
                setJiraCurrentDuration( JiraId , currentDuration);
                setJiraTotalDuration( JiraId ,totalDuration + currentDuration);
                setJiraStopTime(JiraId , timestamp.getTime());
            }

            // If the Resource group changed during this transaction. We will sent a stop to the old group before changing the rgname.
            String currentRG = getJiraRG(currentJiraItem ) ;
            if (!currentRG.equalsIgnoreCase(rgName) ) {
                JiraCollection.updateOne(eq("_id", JiraId), set("status", "Stop"));
            }

            JiraCollection.updateOne(eq("_id", JiraId), set("rg_name", rgName));
            JiraCollection.updateOne(eq("_id", JiraId), set("status", newStatus));
        }
        else {
            addJiraItem(JiraId, rgName, status);
        }

    }


    /*------------------------------------------------------
    Resource Group SUB CLASS
    --------------------------------------------------------*/

    /*------------------------------------------------------
    Return te total number of Resource Groups
    --------------------------------------------------------*/
    public int getNumRGItems(){
        connect();
        //COUNT NUMBER F ITEMS IN COLLECTION
        return (int)RGCollection.count();
    }

    /*------------------------------------------------------
    Return all Resource Groups in an array of String.
    --------------------------------------------------------*/
    public String[] getAllRGNames() {
        int CollectionCount = getNumRGItems();
        int count=0;
        String[] ReturnString = new String[CollectionCount];
        MongoCursor<Document> cursor = RGCollection.find().iterator();
        try {
            while (cursor.hasNext()) {
                JSONObject json = new JSONObject(cursor.next().toJson());
                String RgName = json.getString("rg_name");
                ReturnString[count++]=RgName;
            }
        } finally {
            cursor.close();
        }


        return ReturnString;
    }


    /*     CREATE JIRA/RG graph*/



    public Graph<String, LabeledEdge> createGraph() {


        Graph<String, LabeledEdge> g = new SimpleGraph<>(LabeledEdge.class);


        //Get Jira Items
        String[] allRG = getAllRGNames();
        for (int i = 0; i < allRG.length; i++) {
            String rgName = allRG[i];
            g.addVertex(rgName);

            Map<String,String> allJiraOfRG =getAllJiraItemByRG(rgName);
            Set<String> setJiras = allJiraOfRG.keySet();
            Iterator<String> iterator = setJiras.iterator();

            while (iterator.hasNext()) {
                String JiraName = iterator.next();
                String status = allJiraOfRG.get(JiraName);

                //System.out.println("NAME   :"+ JiraName+ "  label:" + status);
                g.addVertex(JiraName);
                g.addEdge(rgName, JiraName, new LabeledEdge(status));
            }
        }

        return g;
    }

    public String[] GetAllAvailableServices() {
        List<String> services = new ArrayList<String>();

        MongoCursor<Document> cursor = AvailableServicesCollection.find().iterator();
        try {
            while (cursor.hasNext()) {
                String name = new JSONObject(cursor.next().toJson()).getString("serviceName");
                services.add(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        return services.toArray(new String[0]);
    }

    public String[] GetSelectedServices(String issue) {
        Document doc = ServicesRgQueues.find(eq("resourceGroupName", issue)).first();

        if (doc == null) {
            return new String[0];
        }

        JSONArray services = new JSONObject(doc.toJson()).getJSONArray("serviceName");
        List<String> serviceNames = new ArrayList<>();
        for (Object o : services) {
            serviceNames.add(o.toString());
        }

        return serviceNames.toArray(new String[0]);
    }

    public void addInServicesRgQueue(String resourceGroupName, List<String> serviceNames) {
        Document doc = new Document("_id", new ObjectId());

        doc.put("resourceGroupName", resourceGroupName);
        doc.put("serviceName", serviceNames);
        doc.put("creationStatus", "skip");

        ServicesRgQueues.insertOne(doc);
    }

    public boolean doesServiceRgQueueExists(String issue) {
        Document doc = ServicesRgQueues.find(eq("resourceGroupName", issue)).first();
        return doc != null;
    }

    public void startServiceRgQueue(String issue) {
        String doc = ServicesRgQueues.find(eq("resourceGroupName", issue)).first().toJson();
        String creationStatus = new JSONObject(doc).getString("creationStatus");
        if (creationStatus.equals("skip")) {
            ServicesRgQueues.updateOne(eq("resourceGroupName", issue), set("creationStatus", "create"));
        }
    }
}
