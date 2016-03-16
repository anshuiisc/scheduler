    package main.java;

    import backtype.storm.scheduler.Cluster;
    import backtype.storm.scheduler.SupervisorDetails;

    import java.util.*;

    /**
    * Created by anshushukla on 15/03/16.
    */
    public class StateFromConf {



    //        createStateFromConf
    public static void createSetFromConf(String jsonfilepath, String topoName, String boltName, Map<String, String> vm_Name_supIDMap, Set<String> boltName_Set_FromConf, Set<String> workerslot_Set_FromConf, List fullMappingRes_conf)
    {
    //sample-            boltMappingConfig-orion1#6701,2/orion3#6718,1/orion3#6717,1/orion1#6702

        String _boltMappingConfig = JsonFIleReader.getJsonConfig(jsonfilepath, topoName, boltName);
        System.out.println("Test:boltMappingConfig-"+_boltMappingConfig);
        String[] boltMappingConfig_list=_boltMappingConfig.split("/");
        for (String boltMappingConfig_list_val:boltMappingConfig_list){
            int entry= Integer.parseInt(boltMappingConfig_list_val.split(",")[1]);
            String vm_NameFromConf=boltMappingConfig_list_val.split(",")[0].split("#")[0];
            String vm_PortFromConf=boltMappingConfig_list_val.split(",")[0].split("#")[1];
            String _wrkrSlot=vm_Name_supIDMap.get(vm_NameFromConf)+":"+vm_PortFromConf;
            String res=_wrkrSlot+","+boltName+","+entry;
            System.out.println("createStateFromConf-"+res);

            boltName_Set_FromConf.add(boltName);
            workerslot_Set_FromConf.add(_wrkrSlot);
            fullMappingRes_conf.add(res);
            //use setVmNameSupervisorMapping fro setting values

        }

//    return FullMappingRes_conf;
    }





    public static void createStateFromConf(Set<String> boltName_Set_FromConf, Set<String> workerslot_Set_FromConf, List<String> fullMappingRes_conf)
    {
        Map<String, Integer> boltName_IntegerMap = UtilityFunction.StringsetToSortedIndexedList(boltName_Set_FromConf);
        Map<String, Integer> slotName_IntegerMap =UtilityFunction.WorkerSlotStringsetToSortedIndexedList(workerslot_Set_FromConf);
        System.out.println("StateFromConf_boltName_IntegerMap-"+boltName_IntegerMap);
        System.out.println("StateFromConf_slotName_IntegerMap-"+slotName_IntegerMap);
        System.out.println("\nfullMappingRes_conf-"+fullMappingRes_conf);


        int[][] execToboltNameMatrix_from_Conf=new int[slotName_IntegerMap.size()][boltName_IntegerMap.size()];
        for(String s1:fullMappingRes_conf){
            String _workrSlotFromConf=(s1.split(",")[0]);
            String _boltFromConf=(s1.split(",")[1]);
            int _entryFromConf=Integer.parseInt(s1.split(",")[2]);
            execToboltNameMatrix_from_Conf[slotName_IntegerMap.get(_workrSlotFromConf)][boltName_IntegerMap.get(_boltFromConf)]=_entryFromConf;
        }
        System.out.println("printing a 2-D array for execToboltNameMatrix_from_Conf - "+Arrays.deepToString(execToboltNameMatrix_from_Conf));
    }



    public static Map<String, String> setVmNameSupervisorMapping(Cluster cluster, String SITE)
    {

        Map<String,String>  _vm_Name_supIDMap=new HashMap<>();
        Map<String,SupervisorDetails>  supID_Details_Mapping=cluster.getSupervisors();

        for(String _supID : supID_Details_Mapping.keySet()){
            SupervisorDetails   _Details=supID_Details_Mapping.get(_supID);
            Map<String, String> metadata = (Map<String, String>) _Details.getSchedulerMeta();
            if (metadata.get(SITE) != null) {
                String vm_name=(String) metadata.get(SITE);
    //                System.out.println("TEST:-vm_name-" + vm_name+"-_supID-"+_supID);
                _vm_Name_supIDMap.put(vm_name,_supID);
            }
        }

        return _vm_Name_supIDMap;
    }
    }
