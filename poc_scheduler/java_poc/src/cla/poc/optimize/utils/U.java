package cla.poc.optimize.utils;

import cla.poc.optimize.data.InputData;
import cla.poc.optimize.data.TransportRequirement;
import cla.poc.optimize.schedule.SchedulerNode;
import cla.poc.optimize.schedule.SchedulingModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class U {
    protected static SimudataBuilder dataBuilder = null;

    public static SimudataBuilder node(int seq, String nodeTitle) {
        ensureSimudataBuilder();
        return dataBuilder.node(seq, nodeTitle);
    }

    private static void ensureSimudataBuilder() {
        if (dataBuilder == null) {
            dataBuilder = new SimudataBuilder();
        }
    }

    public static SimudataBuilder path(int fromNodeId) {
        ensureSimudataBuilder();
        return dataBuilder.path(fromNodeId);
    }

    public static String dump(Object data) {
        return prettyGson().toJson(data);
    }

    private static Gson prettyGson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    public static SchedulingModel createModelFrom(InputData data) {
        SchedulingModel model = new SchedulingModel();
        model.allNodes = new HashMap<>();
        data.nodeList.forEach(node->{
            SchedulerNode sNode = new SchedulerNode();
            sNode.id = node.id;
            sNode.name = node.title;
            if (node.cargoList != null && node.cargoList.size() > 0){
                sNode.cargos = node.cargoList.stream().collect(Collectors.toMap(it->it.cargoClass, it->it.quantity, Double::sum));
            }
            sNode.costFromNodes = new HashMap<>();
            model.allNodes.put(sNode.id, sNode);
        });

        model.costInfo = new HashMap<>();
        data.pathList.forEach(path->{
            addCost(model.costInfo, path.node1Id, path.node2Id, SchedulingModel.COST_TIME, path.timeCost);
            addCost(model.costInfo, path.node1Id, path.node2Id, SchedulingModel.COST_CASH, path.cashCost);
            addCost(model.costInfo, path.node2Id, path.node1Id, SchedulingModel.COST_TIME, path.timeCost);
            addCost(model.costInfo, path.node2Id, path.node1Id, SchedulingModel.COST_CASH, path.cashCost);
        });
        return model;
    }

    private static void addCost(Map<String, Map<String, Map<String, Double>>> costInfo, String node1Id, String node2Id, String costType, double costValue) {
        Map<String, Map<String, Double>> map1 = costInfo.computeIfAbsent(node1Id, k -> new HashMap<>());
        Map<String, Double> map2 = map1.computeIfAbsent(node2Id, k->new HashMap<>());
        map2.put(costType, costValue);
    }

    public static void calculateCostBetweenNodes(SchedulingModel model) {
        model.allNodes.values().forEach(node->{
            if (node.cargos == null){
                return;
            }

            calculateCostFromNode(model, node);
        });
    }

    private static void calculateCostFromNode(SchedulingModel model, SchedulerNode node) {
        String key = node.id;
        model.allNodes.forEach((k,n)->{
            Map<String, Double> nodeCost = n.costFromNodes.computeIfAbsent(key, k1 -> new HashMap<>());
            nodeCost.put(SchedulingModel.COST_CASH, Double.MAX_VALUE);
            nodeCost.put(SchedulingModel.COST_TIME, Double.MAX_VALUE);
        });

        node.costFromNodes.get(key).put(SchedulingModel.COST_CASH, 0.0);
        node.costFromNodes.get(key).put(SchedulingModel.COST_TIME, 0.0);
        
        updateCostOfNode(key, model, node);
    }

    private static void updateCostOfNode(String key, SchedulingModel model, SchedulerNode curNode) {
        Map<String, Map<String, Double>> neighborCost = model.costInfo.get(curNode.id);
        for(String neighborId: neighborCost.keySet()){
            SchedulerNode neighborNode = model.allNodes.get(neighborId);
            Map<String, Double> neighborCostInfo = neighborCost.get(neighborId);
            for(String costType: neighborCostInfo.keySet()){
                double mineCost = curNode.costFromNodes.get(key).get(costType);
                double neighborCurCost = neighborNode.costFromNodes.get(key).get(costType);
                double costFromMe = mineCost + neighborCostInfo.get(costType);
                if (costFromMe < neighborCurCost) {
                    neighborNode.costFromNodes.get(key).put(costType, costFromMe);
                    updateCostOfNode(key, model, neighborNode);
                }
            }
        }
    }

    public static void perm(List<String> all, int idx, List<String> curList,
                            BiFunction<List<String>, List<String>,List<String>> filterFunc,
                            Consumer<List<String>> executorFunc) {
        if (curList.size() == all.size()){
            executorFunc.accept(curList);
            return;
        }
        List<String> canUsed = filterFunc.apply(curList, all);
        for(String item: canUsed){
            List<String> newList = new ArrayList<>(curList);
            newList.add(item);
            perm(all, idx, newList, filterFunc, executorFunc);
        }

    }
}
