package cla.poc.optimize.utils;

import cla.poc.optimize.data.NodeData;
import cla.poc.optimize.data.PathData;
import cla.poc.optimize.data.TransportRequirement;
import com.sun.org.apache.xalan.internal.lib.ExsltDatetime;

import java.util.ArrayList;
import java.util.List;

public class SimudataBuilder {
    protected int nodeCnt = 0;
    protected List<String> cargoNames = new ArrayList<>();

    protected List<NodeData> nodeList = new ArrayList<>();
    protected NodeData curNode = null;
    protected List<PathData> pathList = new ArrayList<>();
    protected PathData curPath = null;

    public SimudataBuilder node(int seq, String nodeTitle) {
        NodeData node = createNode(seq, nodeTitle);
        nodeList.add(node);
        curNode = node;
        return this;
    }

    private NodeData createNode(int seq, String nodeTitle) {
        NodeData result = new NodeData();
        result.title = nodeTitle;
        result.id = String.valueOf(seq);
        return result;
    }

    public SimudataBuilder has(String cargoName, double qty) {
        addCargoRequirement(curNode, cargoName, qty);
        return this;
    }
    public SimudataBuilder need(String cargoName, double qty) {
        addCargoRequirement(curNode, cargoName, -qty);
        return this;
    }

    private void addCargoRequirement(NodeData curNode, String cargoName, double qty) {
        curNode.ensureCargoList();
        curNode.cargoList.add(createCargo(cargoName, qty));
    }

    private TransportRequirement createCargo(String cargoName, double qty) {
        int idx = cargoNames.indexOf(cargoName);
        if (idx < 0){
            cargoNames.add(cargoName);
            idx = cargoNames.indexOf(cargoName);
        }
        TransportRequirement tr = new TransportRequirement();
        tr.cargoClass = String.valueOf(idx);
        tr.cargoTitle = cargoName;
        tr.quantity = qty;
        return tr;
    }

    public List<NodeData> buildNodeList() {
        return nodeList;
    }

    public SimudataBuilder path(int fromNodeId) {
        curPath = new PathData();
        curPath.node1Id = String.valueOf(fromNodeId);
        pathList.add(curPath);
        return this;
    }

    public SimudataBuilder to(int i) {
        curPath.node2Id = String.valueOf(i);
        return this;
    }

    public SimudataBuilder time(double t) {
        curPath.timeCost = t;
        return this;
    }

    public SimudataBuilder cash(double t) {
        curPath.cashCost = t;
        return this;
    }

    public List<PathData> buildPathList() {
        return pathList;
    }
}
