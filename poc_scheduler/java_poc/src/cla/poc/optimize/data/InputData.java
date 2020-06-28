package cla.poc.optimize.data;

import cla.poc.optimize.utils.U;

import java.util.List;

public class InputData {
    public List<NodeData> nodeList;
    public List<PathData> pathList;
    private InputData(){}

    public static InputData data1() {
        InputData data = new InputData();
        data.nodeList = U.node(1,"出发点")
            .node(2,"某1")
            .node(3,"B")
                .has("氧气", 100)
                .has("氮气", 200)
            .node(4, "A")
                .has("氧气", 50)
                .has("氮气", 20)
            .node(5,"收费站1")
            .node(6,"收费站2")
            .node(7,"甲")
                .need("氧气", 80)
                .need("氮气", 30)
            .node(8,"乙")
                .need("氧气", 50)
                .need("氮气", 80)
            .node(9,"某2")
            .node(10,"停车场")

            .node(12,"丙")
                // .need("氧气", 10)
                // .need("氮气", 10)


            .buildNodeList()
        ;

        data.pathList = U.path(1).to(2).time(10).cash(5)
                .path(2).to(3).time(10).cash(5)
                .path(2).to(4).time(5).cash(3)
                .path(3).to(4).time(20).cash(7)
                .path(4).to(5).time(1).cash(3)
                .path(5).to(6).time(30).cash(50)
                .path(6).to(7).time(1).cash(3)
                .path(8).to(7).time(10).cash(5)
                .path(8).to(3).time(40).cash(15)
                .path(8).to(9).time(40).cash(15)
                .path(8).to(10).time(15).cash(5)

                .path(12).to(7).time(15).cash(5)
                .path(12).to(8).time(15).cash(5)

                .buildPathList()
                ;
        return data;
    }
}
