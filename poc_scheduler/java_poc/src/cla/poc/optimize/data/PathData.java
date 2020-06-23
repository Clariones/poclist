package cla.poc.optimize.data;

public class PathData {
    public String node1Id;   // 第一个节点的ID. 简化, 来去的路径相同.
    public String node2Id;
    public double cashCost;     // 路途费用. 简化,直接贴一个固定费用
    public double timeCost;     // 时间消耗. 简化,直接写一个时间数字
}
