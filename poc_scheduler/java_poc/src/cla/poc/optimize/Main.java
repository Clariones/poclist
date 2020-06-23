package cla.poc.optimize;

import cla.poc.optimize.data.InputData;
import cla.poc.optimize.data.NodeData;
import cla.poc.optimize.data.TransportRequirement;
import cla.poc.optimize.schedule.CargoTransInfo;
import cla.poc.optimize.schedule.SchedulingModel;
import cla.poc.optimize.utils.Log;
import cla.poc.optimize.utils.U;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class Main {

    public static void main(String[] args) throws Exception {
        Log.log("首先,准备模拟数据");
        InputData data = InputData.data1();
        Log.log(U.dump(data));



        Log.log("构建运算节点数据");
        SchedulingModel model = U.createModelFrom(data);
        // Log.log(U.dump(model));
        //从sink点开始,计算所有的点之间的成本
        U.calculateCostBetweenNodes(model);
        Log.log(U.dump(model));

        // 统计出货品的品类及其上下货点

        Map<String, List<CargoTransInfo>> cargoTransInfoLists = U.makeCargoTransInfo(model);
        List<String> cargoClassList = new ArrayList<>(cargoTransInfoLists.keySet());
        Log.log("货品种类:"+ cargoClassList);

//        // 先全排列出所有的"优先安排"列表
//        List<List<String>> allCargoPriorityList = new ArrayList<>();
//        U.perm(cargoClassList, 0, new ArrayList<>(), U::getUnused, allCargoPriorityList::add);
//
//        System.out.println(allCargoPriorityList.stream().map(list->list.stream()
//                .map(it -> model.cargoTitles.get(it)).collect(Collectors.toList())
//        ).collect(Collectors.toList()));

        // 每种品类单独规划'调拨计划'
        for(String cargoType: cargoClassList){
            List<String> sinkNodeIds = new ArrayList<>();
            model.allNodes.values().forEach(node->{
                if (node.cargos == null || node.cargos.isEmpty()){
                    return;
                }
                if (node.cargos.get(cargoType) != null && node.cargos.get(cargoType) < 0){
                    sinkNodeIds.add(node.id);
                }
            });

            List<List<String>> allPlanedNodeList = new ArrayList<>();
            U.perm(sinkNodeIds, 0, new ArrayList<>(), U::getUnused, allPlanedNodeList::add);
            System.out.println("规划顺序");
            System.out.println(allPlanedNodeList);
//            System.out.println(allPlanedNodeList.stream().map(list->list.stream()
//                    .map(it -> model.allNodes.get(it).name).collect(Collectors.toList())
//            ).collect(Collectors.toList()));
        }
    }
}
