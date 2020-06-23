package cla.poc.optimize.schedule;

import java.util.Map;

public class SchedulerNode {
    public String id;
    public String name;

    // Map<cargoType, cargoQuantity>
    public Map<String, Double> cargos;
    // Map<fromNodeId, Map<costType, minCost>>
    public Map<String, Map<String, Double>> costFromNodes;
}
