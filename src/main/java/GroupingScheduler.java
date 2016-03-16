package main.java;

/**
 * Created by anshushukla on 12/01/16.
 */
//public class SiteMod {
//}

import backtype.storm.generated.Bolt;
import backtype.storm.generated.SpoutSpec;
import backtype.storm.generated.StormTopology;
import backtype.storm.scheduler.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GroupingScheduler implements IScheduler{

    @Override
    public void prepare(Map conf) {

    }

    @Override
    public void schedule(Topologies topologies, Cluster cluster) {
        Collection<TopologyDetails> topologyDetails = topologies.getTopologies();
        Collection<SupervisorDetails> supervisorDetails = cluster.getSupervisors().values();
        Map<Integer, SupervisorDetails> supervisors = new HashMap<Integer, SupervisorDetails>();
        for(SupervisorDetails s : supervisorDetails){
            Map<String, Object> metadata = (Map<String, Object>)s.getSchedulerMeta();
            if(metadata.get("group-id") != null){
                supervisors.put((Integer)metadata.get("group-id"), s);
            }
        }

        for(TopologyDetails t : topologyDetails){
            if(cluster.needsScheduling(t)) continue;
            StormTopology topology = t.getTopology();
            Map<String, Bolt> bolts = topology.get_bolts();
            Map<String, SpoutSpec> spouts = topology.get_spouts();
            JSONParser parser = new JSONParser();
            try{
                for(String name : bolts.keySet()){
                    Bolt bolt = bolts.get(name);
                    JSONObject conf = (JSONObject)parser.parse(bolt.get_common().get_json_conf());
                    if(conf.get("group-id") != null && supervisors.get(conf.get("group-id")) != null){
                        Integer gid = (Integer)conf.get("group-id");
                        SupervisorDetails supervisor = supervisors.get(gid);
                        List<WorkerSlot> availableSlots = cluster.getAvailableSlots(supervisor);
                        List<ExecutorDetails> executors = cluster.getNeedsSchedulingComponentToExecutors(t).get(name);
                        if(!availableSlots.isEmpty() && executors != null){
                            cluster.assign(availableSlots.get(0), t.getId(), executors);
                        }
                    }
                }
                for(String name : spouts.keySet()){
                    SpoutSpec spout = spouts.get(name);
                    JSONObject conf = (JSONObject)parser.parse(spout.get_common().get_json_conf());
                    if(conf.get("group-id") != null && supervisors.get(conf.get("group-id")) != null){
                        Integer gid = (Integer)conf.get("group-id");
                        SupervisorDetails supervisor = supervisors.get(gid);
                        List<WorkerSlot> availableSlots = cluster.getAvailableSlots(supervisor);
                        List<ExecutorDetails> executors = cluster.getNeedsSchedulingComponentToExecutors(t).get(name);
                        if(!availableSlots.isEmpty() && executors != null){
                            cluster.assign(availableSlots.get(0), t.getId(), executors);
                        }
                    }
                }
            }catch(ParseException pe){
                pe.printStackTrace();
            }
        }
    }

}