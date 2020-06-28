package cla.poc.optimize.schedule;

import cla.poc.optimize.utils.Log;
import cla.poc.optimize.utils.U;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanHelper {
    public static List<AllocationPlan> getAllocationPlanByGivenScheduleList(SchedulingModel model, List<CargoTransInfo> cargoTransInfo,
                    String cargoType, List<Map<String, Object>> scheduleList) {
        // 首先复制一份原始的库存
        Map<String, Double> orgStores = cargoTransInfo.stream().collect(Collectors.toMap(node -> node.nodeId, node -> node.cargoQuantity));
        List<AllocationPlan> planList = new ArrayList<>();
        double totalTimeCost = 0.0;
        double totalCashCost = 0.0;
        double totalNeeded = 0.0;
        {
            AllocationPlan plan = new AllocationPlan();
            plan.cargoType = cargoType;
            planList.add(plan);
        }

        for (Map<String, Object> stringObjectMap : scheduleList) {
            String tgtNode = (String) stringObjectMap.get("toNode");
            List<String> fromNodeList = (List<String>) stringObjectMap.get("fromNodeList");

            System.out.println("现在按照"+fromNodeList+"的顺序往"+tgtNode+"调配"+model.cargoTitles.get(cargoType));
            double totalNeed = model.allNodes.get(tgtNode).cargos.get(cargoType);
            totalNeeded -= totalNeed;
            for (String fromNode : fromNodeList) {
                double left = orgStores.get(fromNode);
                if (left <=0) { // 没货了
                    continue;
                }
                if ((left + totalNeed) >= 0) { // 储量充足
                    AllocationPlan plan = new AllocationPlan();
                    plan.cargoType = cargoType;
                    plan.cashCost = model.allNodes.get(fromNode).costFromNodes.get(tgtNode).get(SchedulingModel.COST_TIME);
                    plan.timeCost = model.allNodes.get(fromNode).costFromNodes.get(tgtNode).get(SchedulingModel.COST_CASH);
                    totalTimeCost += plan.timeCost;
                    totalCashCost += plan.cashCost;
                    plan.fromNodeId = fromNode;
                    plan.toNodeId = tgtNode;
                    plan.quantity = -totalNeed;
                    planList.add(plan);
                    orgStores.put(fromNode, left + totalNeed);
                    totalNeed = 0.0;
                    break;
                }
                // 有货,但是不足
                AllocationPlan plan = new AllocationPlan();
                plan.cargoType = cargoType;
                plan.cashCost = model.allNodes.get(fromNode).costFromNodes.get(tgtNode).get(SchedulingModel.COST_TIME);
                plan.timeCost = model.allNodes.get(fromNode).costFromNodes.get(tgtNode).get(SchedulingModel.COST_CASH);
                totalTimeCost += plan.timeCost;
                totalCashCost += plan.cashCost;
                plan.fromNodeId = fromNode;
                plan.toNodeId = tgtNode;
                plan.quantity = left;
                totalNeed = totalNeed + left;
                planList.add(plan);
                orgStores.put(fromNode, 0.0);
            }
        }

        {
            AllocationPlan plan = planList.get(0);
            plan.timeCost = totalTimeCost;
            plan.cashCost = totalCashCost;
            plan.quantity = totalNeeded;
        }
        return planList;
    }
}
