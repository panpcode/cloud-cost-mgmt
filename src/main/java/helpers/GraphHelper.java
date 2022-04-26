package helpers;

import helpers.DummyMongoApi;
import helpers.MongoRun;
import helpers.MongoApi;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class GraphHelper{

    static public String getJsonDependencies(){
        try {
            MongoApi dmapi = new MongoRun("localhost", 27017);
            System.out.println("Initialized the Mongo DB object, will now request graph data");
            Graph<String, LabeledEdge> g = dmapi.createGraph();
            System.out.println("Successfully got dependency graph");
            return GraphHelper.convertForGraphLib(g);
        }catch(Exception e) {
            System.out.println("Successfully read RG names");
            return "{}";
        }
    }

    static public String convertForGraphLib(Graph<String, LabeledEdge> g){
        JSONObject toreturn=new JSONObject();

        JSONObject options=new JSONObject();
        options.put("directed",true);
        options.put("multigraph",true);
        options.put("compound",false);

        toreturn.put("options",options);
        {
            ArrayList<JSONObject> node_list = new ArrayList<>();
            for (String node_name : g.vertexSet()) {
                if(g.edgesOf(node_name).size()>0){

                    JSONObject node_object = new JSONObject();
                    node_object.put("v", node_name);

                    JSONObject value = new JSONObject();
                    value.put("label", node_name);
                    node_object.put("value", value);

                    node_list.add(node_object);
                }
                else{
                    System.out.println("Dropping node "+node_name+" it had no edges");
                }

            }

            toreturn.put("nodes", node_list.toArray());
        }

        {
            ArrayList<JSONObject> edge_list = new ArrayList<>();
            for (LabeledEdge edge : g.edgeSet()) {

                JSONObject edge_object = new JSONObject();
                edge_object.put("w", g.getEdgeSource(edge));
                edge_object.put("v", g.getEdgeTarget(edge));

                JSONObject value = new JSONObject();
                value.put("label", edge.toString());
                edge_object.put("value", value);

                edge_list.add(edge_object);
            }

            toreturn.put("edges", edge_list.toArray());
        }
        System.out.println(toreturn.toString());
        return toreturn.toString();
    }



}