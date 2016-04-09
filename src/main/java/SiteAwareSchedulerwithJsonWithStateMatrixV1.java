
package main.java;

import backtype.storm.generated.SpoutSpec;
import backtype.storm.generated.StormTopology;
import backtype.storm.scheduler.*;

import java.util.*;


public class SiteAwareSchedulerwithJsonWithStateMatrixV1 implements IScheduler {

    private static final String SITE = "site"; //used only while caching supervisor details
    //    private static final String THREADS = "threads";
//    String jsonfilepath = "/data/tetc/apache-storm-0.9.4-ForScheduling/conf/inputTopoConfig.json";
    String jsonfilepath = "/home/anshu/apache-storm-0.10.0/conf/inputTopoConfig.json";
    Map<ExecutorDetails, String> executorDetailsStringMap;

    @Override
    public void prepare(Map conf) {
    }

    @Override
    public void schedule(Topologies topologies, Cluster cluster) {
        System.out.println("=======================================TEST:running====================================");
        Collection<TopologyDetails> topologyDetails = topologies.getTopologies();
        Collection<SupervisorDetails> supervisorDetails = cluster.getSupervisors().values();  //get supervisor details of cluster
        int needsSchedulingFlagBolt = 0;
        int needsSchedulingFlagSpout = 0;
        int needsSchedulingFlag = 0;

        for (TopologyDetails topo : topologyDetails) {

            //creating VM name and supID mapping
            Map<String, String> vm_Name_supIDMap = new HashMap<>();
            vm_Name_supIDMap = StateFromConf.setVmNameSupervisorMapping(cluster, SITE);
            System.out.println("vm_Name_supIDMap-\n" + vm_Name_supIDMap + "\n");


            System.out.println("\n\t\t\t\t--Conf state creation started--");
            //create input sets from conf
            Set<String> boltName_Set_FromConf = new HashSet<>();
            Set<String> workerslot_Set_FromConf = new HashSet<>();
            List<String> FullMappingRes_conf = new ArrayList();

            StateFromConf.createSetFromConf(jsonfilepath, topo, vm_Name_supIDMap, boltName_Set_FromConf, workerslot_Set_FromConf, FullMappingRes_conf);

            int row_size_fromConf = workerslot_Set_FromConf.size();
            int column_size_fromConf = boltName_Set_FromConf.size();
            HashMap<String, Integer> boltName_IntegerMap = new HashMap<>();
            HashMap<String, Integer> slotName_IntegerMap = new HashMap<>();
            HashMap<String, HashMap<String, Integer>> execToboltNameMap_Conf_State = new HashMap<>();

            StateFromConf.createStateFromConf(boltName_Set_FromConf, workerslot_Set_FromConf, FullMappingRes_conf, boltName_IntegerMap, slotName_IntegerMap, execToboltNameMap_Conf_State);

            System.out.println("execToboltNameMap_from_Conf-" + execToboltNameMap_Conf_State);
            System.out.println("slotName_IntegerMap" + "-" + slotName_IntegerMap + "\n-boltName_IntegerMap-" + boltName_IntegerMap + "\n-FullMappingRes_conf-" + FullMappingRes_conf);
            System.out.println("\n\t\t\t\t--Conf state done--");


            System.out.println("\n\nTest:Current state Start-----------------------------------------");

            HashMap<WorkerSlot, HashMap<String, Integer>> currentState_execToboltNameMap = new HashMap<>();
            Map<String, Integer> current_boltname_NumberPair = new HashMap<>();
            Map<WorkerSlot, Integer> current_workeSlot_NumberPair = new HashMap<>();
            int[][] currentexecToboltNameMatrix = new int[row_size_fromConf][column_size_fromConf];

            Map<ExecutorDetails, String> _executorDetailsStringMapDummy = UtilityFunction.getcurrentExecListToboltname(topo, cluster);
            //_executorDetailsStringMapDummy will be empty after scheduling is done


//            if (_executorDetailsStringMapDummy.size() != 0)
//                this.executorDetailsStringMap = _executorDetailsStringMapDummy;


            //test
            if (_executorDetailsStringMapDummy.size() != 0) {
                System.out.println("log7-New code");
                if (this.executorDetailsStringMap != null) {
                    //failure case have returned something
                    System.out.println("log5-Inside else part");
                    for (ExecutorDetails e : _executorDetailsStringMapDummy.keySet()) {
                        String newBoltname = _executorDetailsStringMapDummy.get(e);
                        if (this.executorDetailsStringMap.containsKey(e)) {
                            this.executorDetailsStringMap.put(e, newBoltname);
                        } else {
                            this.executorDetailsStringMap.put(e, newBoltname);
                        }
                    }
                } else {//first iteration or fresh submission
                    System.out.println("log6-Inside else part");
                    this.executorDetailsStringMap = _executorDetailsStringMapDummy;
                }
            }
            //

            System.out.println("executorDetailsStringMap inside main-" + this.executorDetailsStringMap + "\n");
            if (executorDetailsStringMap != null) {
                currentexecToboltNameMatrix = UtilityFunction.createCurrentStateMatrix(topo, cluster, current_boltname_NumberPair, current_workeSlot_NumberPair, row_size_fromConf, column_size_fromConf, this.executorDetailsStringMap, currentState_execToboltNameMap);

            }
            System.out.println("CurrentexecToboltNameMatrix-" + Arrays.deepToString(currentexecToboltNameMatrix));


            System.out.println("currentexecToboltNameMap inside main function -" + currentState_execToboltNameMap + "\n\n");
            System.out.println("UtilityFunction_workeSlot_NumberPair-" + current_workeSlot_NumberPair + "\n\n");
            System.out.println("UtilityFunction_boltname_NumberPair-" + current_boltname_NumberPair + "\n\n");

            System.out.println("CurrentexecToboltNameMatrix-" + Arrays.deepToString(currentexecToboltNameMatrix));
            System.out.println("\t\t\t\tTest:Current state End------------------------------------\n\n");


            Map<String, SupervisorDetails> supervisors = new HashMap<>();
            StormTopology topology = topo.getTopology();
            String topoName = topo.getName();
            Map<String, SpoutSpec> spouts = topology.get_spouts();
            //setting supervisor data
            //logging: getting supervisor names
            {
                for (String s : cluster.getSupervisors().keySet()) {
                    System.out.println("supervisor names are -" + s);
            }
            }
            //

            System.out.println("TEST:caching supervisors indexed by their sites in a hash map...");
            for (SupervisorDetails s : cluster.getSupervisors().values()) {
                //System.out.println("TEST:supervisor name-" + s);
                Map<String, String> metadata = (Map<String, String>) s.getSchedulerMeta();
                if (metadata.get(SITE) != null) {
                    System.out.println("TEST: checking if metadata is set on this supervisor....");
                    supervisors.put(metadata.get(SITE), s);
                    System.out.println("TEST:Value for this supervisor-" + metadata.get(SITE));
            }
                System.out.println(s.getAllPorts() + "\n");
            }

            //schedule bolt using hashmap
            ScheduleFromMapDiff.findMatrixDiffandSchedule(currentState_execToboltNameMap, execToboltNameMap_Conf_State, vm_Name_supIDMap, supervisors, cluster, topo);


//            //schedule Spout seperately
//            List<ExecutorDetails> spout_executors = new ArrayList<>();
//            for (String spoutName : spouts.keySet()) {
//                String site1 = null;
////                SpoutSpec spout = spouts.get(spoutName);
//                String spoutMappingConfig = JsonFIleReader.getJsonConfig(jsonfilepath, topoName, spoutName);
//                String boltMappingThreads = JsonFIleReader.getJsonThreadCount(jsonfilepath, topoName, spoutName);
//
//                //
//                List<String> mappings = Arrays.asList(spoutMappingConfig.split("/"));
//                //
//                //CHECK:No split to this config
//
//                for (String node_slot : mappings) {
//
//                    String nodeName = node_slot.split(",")[0].split("#")[0];
//                    int slotid = Integer.parseInt(node_slot.split(",")[0].split("#")[1]);
//                    int threads_forSlot = Integer.parseInt(node_slot.split(",")[1]);
//
//                    System.out.println("Scheduling spout for -" + nodeName + "-" + slotid + "-" + threads_forSlot);
//                    if (mappings != null && supervisors.get(nodeName) != null) {
//                        site1 = nodeName;
//                        System.out.println("TEST:inside topology loop for spout-" + site1);
//                    }
//
//                    SupervisorDetails supervisor = supervisors.get(site1);
//                    List<WorkerSlot> workerSlots = cluster.getAvailableSlots(supervisor);
//
//                    spout_executors = cluster.getNeedsSchedulingComponentToExecutors(topo).get(spoutName);
//                    System.out.println("Remaining list -" + spout_executors);
//                    if (!workerSlots.isEmpty() && spout_executors != null) {
//                        for (WorkerSlot ws : workerSlots) {
//                            if (ws.getPort() == slotid) {
//                                cluster.assign(ws, topo.getId(), new ArrayList(spout_executors.subList(0, threads_forSlot)));
//                            }
//                        }
////                     System.out.println("Going to assign spout" + workerSlots.get(0) + "-" + spout_executors);
////                     cluster.assign(workerSlots.get(0), topo.getId(), spout_executors);
//                    }
//
//
//                }
//            }


        }
    }

}


