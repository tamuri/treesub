package treesub.tree;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.List;

/**
 * @author Asif Tamuri (atamuri@nimr.mrc.ac.uk)
 *
 */
public class NodeAttributes {
    HashMap<NodeAttributeKey, String> attributes = Maps.newHashMap();

    public NodeAttributes(NodeAttributeKey nak, String s) {
        attributes.put(nak, s);
    }

    public void add(NodeAttributeKey nak, String s) {
        attributes.put(nak, s);
    }

    public String get(NodeAttributeKey nak) {
        if (!attributes.containsKey(nak)) return "";
        return attributes.get(nak);
    }

    public int size() {
        return attributes.size();
    }

    @Override
    public String toString() {
        if (attributes.size() == 0) {
            return "";
        }

        List<NodeAttributeKey> entries = Lists.newArrayList(attributes.keySet());

        String s = String.format("[&%s=\"%s\"", entries.get(0).toString(), attributes.get(entries.get(0)));

        for (int i = 1; i < entries.size(); i++) {
            s += String.format(",%s=\"%s\"", entries.get(i).toString(), attributes.get(entries.get(i)));
        }

        s += "]";

        return s;

    }

    public enum NodeAttributeKey {
        REALNAME, ALLSUBS, NUMBER, NONSYNSUBS, FULL, NAME_AND_SUBS
    }
}
