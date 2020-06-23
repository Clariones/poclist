package cla.poc.optimize.data;

public class TransportRequirement {
    public String cargoClass; // 品类. (懒得写getter,直接public)
    public String cargoTitle; // 用于展示的名字. 实际计算只区分 cargoClass
    public Double quantity;   // 货品数量. 简化, 不考虑同时上下货, 正表示有货可运走, 负表示需要收多少货.

}
