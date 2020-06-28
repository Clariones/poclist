package cla.poc.optimize.utils;

import cla.poc.optimize.data.InputData;
import cla.poc.optimize.schedule.CargoTransInfo;
import cla.poc.optimize.schedule.SchedulerNode;
import cla.poc.optimize.schedule.SchedulingModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
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
        model.cargoTitles = new HashMap<>();
        data.nodeList.forEach(node -> {
            SchedulerNode sNode = new SchedulerNode();
            sNode.id = node.id;
            sNode.name = node.title;
            if (node.cargoList != null && node.cargoList.size() > 0) {
                sNode.cargos = node.cargoList.stream().collect(Collectors.toMap(it -> it.cargoClass, it -> it.quantity, Double::sum));
                model.cargoTitles.putAll(node.cargoList.stream().collect(Collectors.toMap(it -> it.cargoClass, it -> it.cargoTitle)));
            }
            sNode.costFromNodes = new HashMap<>();
            model.allNodes.put(sNode.id, sNode);
        });

        model.costInfo = new HashMap<>();
        data.pathList.forEach(path -> {
            addCost(model.costInfo, path.node1Id, path.node2Id, SchedulingModel.COST_TIME, path.timeCost);
            addCost(model.costInfo, path.node1Id, path.node2Id, SchedulingModel.COST_CASH, path.cashCost);
            addCost(model.costInfo, path.node2Id, path.node1Id, SchedulingModel.COST_TIME, path.timeCost);
            addCost(model.costInfo, path.node2Id, path.node1Id, SchedulingModel.COST_CASH, path.cashCost);
        });
        return model;
    }

    private static void addCost(Map<String, Map<String, Map<String, Double>>> costInfo, String node1Id, String node2Id, String costType, double costValue) {
        Map<String, Map<String, Double>> map1 = costInfo.computeIfAbsent(node1Id, k -> new HashMap<>());
        Map<String, Double> map2 = map1.computeIfAbsent(node2Id, k -> new HashMap<>());
        map2.put(costType, costValue);
    }

    public static void calculateCostBetweenNodes(SchedulingModel model) {
        model.allNodes.values().forEach(node -> {
            if (node.cargos == null) {
                return;
            }

            calculateCostFromNode(model, node);
        });
    }

    private static void calculateCostFromNode(SchedulingModel model, SchedulerNode node) {
        String key = node.id;
        model.allNodes.forEach((k, n) -> {
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
        for (String neighborId : neighborCost.keySet()) {
            SchedulerNode neighborNode = model.allNodes.get(neighborId);
            Map<String, Double> neighborCostInfo = neighborCost.get(neighborId);
            for (String costType : neighborCostInfo.keySet()) {
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
                            BiFunction<List<String>, List<String>, List<String>> filterFunc,
                            Consumer<List<String>> executorFunc) {
        if (curList.size() == all.size()) {
            executorFunc.accept(curList);
            return;
        }
        List<String> canUsed = filterFunc.apply(all, curList);
        for (String item : canUsed) {
            List<String> newList = new ArrayList<>(curList);
            newList.add(item);
            perm(all, idx, newList, filterFunc, executorFunc);
        }
    }

    public static <T,R> void perm(List<List<T>> possibleValues, Function<T, R> itemMapper, Consumer<List<R>> executorFunc){
        List<Integer> flags = possibleValues.stream().map(List::size).collect(Collectors.toList());

        ArrayList<R> currentList = new ArrayList<>(Collections.nCopies(possibleValues.size(), null));
        permAtPosition(currentList, 0, possibleValues, itemMapper, executorFunc);
    }

    private static <T, R> void permAtPosition(ArrayList<R> currentList, int idx, List<List<T>> possibleValues,
                      Function<T, R> itemMapper, Consumer<List<R>> executorFunc) {
        List<T> candidates = possibleValues.get(idx);
        for(int i=0;i<candidates.size();i++){
            currentList.set(idx, itemMapper.apply(candidates.get(i)));
            if (idx == (possibleValues.size()-1)){
                List<R> newResult = new ArrayList<>(currentList);
                executorFunc.accept(newResult);
                continue;
            }

            permAtPosition(currentList, idx+1, possibleValues, itemMapper, executorFunc);
        }
    }

    /**
     * Map(cargoType, List(CargoTransInfo))
     */
    public static Map<String, List<CargoTransInfo>> makeCargoTransInfo(SchedulingModel model) {
        Map<String, List<CargoTransInfo>> result = new HashMap<>();
        model.allNodes.forEach((idx, node) -> {
            if (node.cargos == null || node.cargos.isEmpty()) {
                return;
            }
            node.cargos.forEach((cargoType, cargoQuantity) -> {
                if (cargoQuantity < 0) {
                    return;
                }
                List<CargoTransInfo> cargoInfoList = result.computeIfAbsent(cargoType, k -> new ArrayList<>());
                CargoTransInfo cargoInfo = new CargoTransInfo();
                cargoInfo.nodeId = node.id;
                cargoInfo.cargoQuantity = cargoQuantity;
                cargoInfo.cargoType = cargoType;
                cargoInfoList.add(cargoInfo);
            });
        });
        return result;
    }

    public static List<String> getUnused(List<String> all, List<String> used) {
        List<String> x = new ArrayList<>(all);
        x.removeAll(used);
        return x;
    }

    public static List<String> getSourceNodeIdsByCargoTimeCost(SchedulingModel model, List<CargoTransInfo> cargoTransInfos, String sinkNodeId) {
        return getSourceNodeIdsByCargoCost(model, cargoTransInfos, sinkNodeId, (cost) -> cost.get(SchedulingModel.COST_TIME));
    }
    public static List<String> getSourceNodeIdsByCargoCashCost(SchedulingModel model, List<CargoTransInfo> cargoTransInfos, String sinkNodeId) {
        return getSourceNodeIdsByCargoCost(model, cargoTransInfos, sinkNodeId, (cost) -> cost.get(SchedulingModel.COST_CASH));
    }

    private static List<String> getSourceNodeIdsByCargoCost(SchedulingModel model, List<CargoTransInfo> cargoTransInfos, String sinkNodeId, Function<Map<String,Double>, Double> costPicker) {
        if (cargoTransInfos == null || cargoTransInfos.isEmpty()){
            return new ArrayList<>();
        }
        if(cargoTransInfos.size() == 1){
            return new ArrayList<String>(){{this.add(cargoTransInfos.get(0).nodeId);}};
        }
        List<String> allNodesTitle = new ArrayList<>();
        for (CargoTransInfo cargoTransInfo : cargoTransInfos) {
            SchedulerNode srcNode = model.allNodes.get(cargoTransInfo.nodeId);
            Map<String, Double> costInfo = srcNode.costFromNodes.get(sinkNodeId);
            double cost = costPicker.apply(costInfo);
            String nTitle = String.format("%030.10f_%s", cost, srcNode.id);
            allNodesTitle.add(nTitle);
        }
        Collections.sort(allNodesTitle);
        return allNodesTitle.stream().map(title->title.substring(title.indexOf("_")+1)).collect(Collectors.toList());
    }


}