package cla.poc.optimize;

import cla.poc.optimize.data.InputData;
import cla.poc.optimize.schedule.*;
import cla.poc.optimize.utils.Log;
import cla.poc.optimize.utils.U;

import java.util.*;
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
        Log.log("货品种类:"+ cargoClassList+"\t:\t" +
                cargoClassList.stream().map(it -> model.cargoTitles.get(it)).collect(Collectors.toList()));

//        // 先全排列出所有的"优先安排"列表
//        List<List<String>> allCargoPriorityList = new ArrayList<>();
//        U.perm(cargoClassList, 0, new ArrayList<>(), U::getUnused, allCargoPriorityList::add);
//
//        System.out.println();
        Map<String, List<List<AllocationPlan>>> plans = cargoTransportPlanSchedule(model, cargoTransInfoLists, cargoClassList);

        // List-品类<List-可能<list-调拨单>>>
        List<List<List<AllocationPlan>>> plansGroupByCargoType = new ArrayList<>();
        for(String cargoType: cargoClassList){
            plansGroupByCargoType.add(plans.get(cargoType));
        }
        Log.log(U.dump(plansGroupByCargoType));

        List<List<List<AllocationPlan>>> finalPlanForAllCargo = new ArrayList<>();
        U.perm(plansGroupByCargoType, l -> {
            Log.log("现在处理" + U.dump(l));
            return l.subList(1, l.size());
        }, l->finalPlanForAllCargo.add(l));
        Log.log("最后的调拨计划可能为");
        Log.log(U.dump(finalPlanForAllCargo));

        // 最终的结果,应该是以节点安排的
        finalPlanForAllCargo.forEach(plan->{
            Map<String, List<AllocationPlan>> nodes = plan.stream().flatMap(List::stream).collect(Collectors.groupingBy(ap -> ap.fromNodeId));
            Log.log(U.dump(nodes));
        });
    }



    private static Map<String, List<List<AllocationPlan>>> cargoTransportPlanSchedule(SchedulingModel model, Map<String, List<CargoTransInfo>> cargoTransInfoLists, List<String> cargoClassList) {
        // 每种品类单独规划'调拨计划'
        Map<String, List<List<AllocationPlan>>> plans = new HashMap<>();
        for(String cargoType: cargoClassList) {
            String cargoName = String.format("[%10s]",model.cargoTitles.get(cargoType));
            List<List<AllocationPlan>> allPlan = planForOneCargoType(model, cargoTransInfoLists, cargoType);

            // 从中挑出按时间成本和现金成本最少的
            double minTimeCost = Double.MAX_VALUE;
            double minCashCost = Double.MAX_VALUE;
            Set<Integer> idxMinByTime = new HashSet<>();
            Set<Integer> idxMinByCash = new HashSet<>();
            for (int i = 0; i < allPlan.size(); i++) {
                List<AllocationPlan> plan = allPlan.get(i);
                double cost = plan.get(0).timeCost;
                if (cost < minTimeCost){
                    idxMinByTime.clear();
                    idxMinByTime.add(i);
                }else if (cost == minTimeCost) {
                    idxMinByTime.add(i);
                }else{
                    // 大的跳过
                }
                // cash
                cost = plan.get(0).cashCost;
                if (cost < minCashCost){
                    idxMinByCash.clear();
                    idxMinByCash.add(i);
                }else if (cost == minTimeCost) {
                    idxMinByCash.add(i);
                }else{
                    // 大的跳过
                }
            }

            // 输出一下阶段结果
            Set<Integer> finalPlanIds = new HashSet<>();
            finalPlanIds.addAll(idxMinByTime);
            finalPlanIds.addAll(idxMinByCash);
            List<List<AllocationPlan>> plansForCargo = new ArrayList<>();
            for (Integer finalPlanId : finalPlanIds) {
                List<AllocationPlan> plan = allPlan.get(finalPlanId);
                plansForCargo.add(plan);
                Log.log(cargoName+" time cost: %8.3f, cash cost: %8.3f", plan.get(0).timeCost, plan.get(0).cashCost);
                StringBuilder sb = new StringBuilder();

                for(int i=1;i<plan.size();i++){
                    AllocationPlan step = plan.get(i);
                    sb.append("第").append(i).append("步:从").append(step.fromNodeId).append("运送")
                            .append(step.quantity).append("到").append(step.toNodeId).append(", ");
                }
                Log.log(cargoName+sb.toString());
            }

            plans.put(cargoType, plansForCargo);
        }

        return plans;
    }

    // 同一品类的货品的调度计划. 注意 "同一品类"是指可以没有上货下货约束的一批货.
    // 例如, 都是可乐, 100箱是甲卖给乙的,和丙卖给乙的10箱,就不能混装, 看到甲的货有多的,就从甲的仓库取120箱给乙.
    private static List<List<AllocationPlan>> planForOneCargoType(SchedulingModel model, Map<String, List<CargoTransInfo>> cargoTransInfoLists, String cargoType) {
        String cargoName = String.format("[%10s]",model.cargoTitles.get(cargoType));
        // 先挑出所有需要"送货"的节点
        List<String> sinkNodeIds = new ArrayList<>();
        model.allNodes.values().forEach(node->{
            if (node.cargos == null || node.cargos.isEmpty()){
                return;
            }
            if (node.cargos.get(cargoType) != null && node.cargos.get(cargoType) < 0){
                sinkNodeIds.add(node.id);
            }
        });

        // 列出依次调度"送货点"的所有排列
        List<List<String>> allPlanedNodeList = new ArrayList<>();
        U.perm(sinkNodeIds, 0, new ArrayList<>(), U::getUnused, allPlanedNodeList::add);
        Log.log("规划顺序");
        Log.log(cargoName + allPlanedNodeList);
//            System.out.println(allPlanedNodeList.stream().map(list->list.stream()
//                    .map(it -> model.allNodes.get(it).name).collect(Collectors.toList())
//            ).collect(Collectors.toList()));

        List<List<AllocationPlan>> allPlan = new ArrayList<>();
        // 遍历所有的 调拨优先站点 可能, 找出top-n最合理的调拨计划
        for (List<String> nodeIdList : allPlanedNodeList) {
            Map<String,Map<String, List<String>>> possibleNodeSeq = new HashMap<>(); // Map<sinkNode, Map<list-string, list>>
            // 按顺序一个一个站点来排, 找出按照到送货节点的 时间成本 和 现金成本 来排序的, "发货节点" 列表
            for(String nodeId: nodeIdList){
                List<String> sourceNodeIds = U.getSourceNodeIdsByCargoTimeCost(model, cargoTransInfoLists.get(cargoType), nodeId);
                Map<String, List<String>> possibleSeqInNode = possibleNodeSeq.computeIfAbsent(nodeId, k -> new HashMap<>());

                String key = String.join(",",sourceNodeIds);
                possibleSeqInNode.put(key, sourceNodeIds);
            }
            for(String nodeId: nodeIdList){
                List<String> sourceNodeIds = U.getSourceNodeIdsByCargoCashCost(model, cargoTransInfoLists.get(cargoType), nodeId);
                Map<String, List<String>> possibleSeqInNode = possibleNodeSeq.computeIfAbsent(nodeId, k -> new HashMap<>());

                String key = String.join(",",sourceNodeIds);
                possibleSeqInNode.put(key, sourceNodeIds);
            }

            // Log.log(cargoName+"when try " + nodeIdList+", we have " + possibleNodeSeq);

            // 把相同的"调度顺序"合并
            List<List<List<String>>> tmpList1 = new ArrayList<>();
            for(String nodeId: nodeIdList){
                List<List<String>> listForNode = new ArrayList<>();
                Map<String, List<String>> possibleSeqInNode = possibleNodeSeq.get(nodeId);
                possibleSeqInNode.forEach((k,list)->{
                    List<String> tl2 = new ArrayList<>();
                    tl2.add(nodeId);
                    tl2.addAll(list);
                    listForNode.add(tl2);
                });
                tmpList1.add(listForNode);
            }
            // Log.log(tmpList1.toString());

            // 现在得到了每个'送货点'的可能的发货点调度顺序, 再拼接上所有的送货点的, 得到完整(同一批次的货品)的送货点的调度顺序
//            List<List<String>> planList = U.perm(tmpList1, item -> {
//                List list = (List) item;
//                return "move from " + list.subList(1, list.size()) + " to " + list.get(0);
//            });
            List<List<Map<String, Object>>> planList = new ArrayList<>();
            U.perm(tmpList1, item -> {
                Map<String, Object> data = new HashMap<>();
                data.put("toNode", item.get(0));
                data.put("fromNodeList", item.subList(1, item.size()));
                return data;
            }, item->planList.add(item));


            for (List<Map<String, Object>> scheduleList : planList) {
                // Log.log(cargoName+scheduleList);
                List<AllocationPlan> plan = PlanHelper.getAllocationPlanByGivenScheduleList(model, cargoTransInfoLists.get(cargoType), cargoType, scheduleList);
                // Log.log(U.dump(plan.get(0)));

                allPlan.add(plan);
            }
        }
        return allPlan;
    }
}
