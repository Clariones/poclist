package cla.poc.optimize.data;

import java.util.ArrayList;
import java.util.List;

public class NodeData {
    public String id;
    public String title;
    public List<TransportRequirement> cargoList;

    public void ensureCargoList() {
        if (cargoList == null) {
            cargoList = new ArrayList<>();
        }
    }
}
