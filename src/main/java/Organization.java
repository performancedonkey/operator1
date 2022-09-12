import org.agrona.collections.Long2ObjectHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class Organization {
    private final static Set<String> posts = new HashSet<>();
    private final static HashMap<String, Long> shifts = new HashMap<>();
    private final static Long2ObjectHashMap<String> who = new Long2ObjectHashMap<>();


}
