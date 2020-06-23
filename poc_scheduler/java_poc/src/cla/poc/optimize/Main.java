package cla.poc.optimize;

import cla.poc.optimize.data.InputData;
import cla.poc.optimize.data.NodeData;
import cla.poc.optimize.data.TransportRequirement;
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

        // 统计出货品的品类
        List<String> cargoClassList = data.nodeList.stream().filter(it -> it.cargoList != null && it.cargoList.size() > 0)
                .flatMap(it -> it.cargoList.stream())
                .map(it -> it.cargoClass).distinct().collect(Collectors.toList());
        Log.log("货品种类:"+ cargoClassList);
        List<List<String>> allCargoPriorityList = new ArrayList<>();
        // 先全排列出所有的"优先安排"列表
        U.perm(cargoClassList, 0, new ArrayList<>(), (used, all)->{
            List<String> x = new ArrayList<>(all);
            x.removeAll(used);
            return x;
        }, allCargoPriorityList::add);

        System.out.println(allCargoPriorityList);
    }
}
