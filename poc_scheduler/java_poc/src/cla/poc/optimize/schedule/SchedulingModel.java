package cla.poc.optimize.schedule;

import java.util.Map;

public class SchedulingModel {
    public static final String COST_TIME="time";
    public static final String COST_CASH="cash";

    public Map<String, SchedulerNode> allNodes;
    // Map<node1Id, Map<toNodeId, Map<costType, cost>>
    public Map<String, Map<String, Map<String, Double>>> costInfo;
    //
    public Map<String, String> cargoTitles;
}
