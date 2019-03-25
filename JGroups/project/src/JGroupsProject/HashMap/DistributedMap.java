package JGroupsProject.HashMap;

import java.util.HashMap;
import java.util.Map;

public class DistributedMap implements SimpleStringMap {
    private HashMap<String, Integer> distributedMap;

    public DistributedMap(){
        this.distributedMap = new HashMap<>();
    }

    @Override
    public boolean containsKey(String key) {
        return distributedMap.containsKey(key);
    }

    @Override
    public Integer get(String key) {
        return distributedMap.get(key);
    }

    @Override
    public void put(String key, Integer value) {
        distributedMap.put(key, value);
    }

    @Override
    public Integer remove(String key) {
        return distributedMap.remove(key);
    }

    public void getAllElements() {
        if (!distributedMap.isEmpty())
            for (Map.Entry<String, Integer> entry : distributedMap.entrySet())
                System.out.println(entry.getKey() + " : " + entry.getValue());
        else System.out.println("DistributedMap is empty.");
    }

    public HashMap<String, Integer> getDistributedMap() {
        return distributedMap;
    }

    public void setDistributedMap(HashMap<String, Integer> distributedMap) {
        this.distributedMap = distributedMap;
    }
}
