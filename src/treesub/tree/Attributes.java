package treesub.tree;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.List;

/**
 * @author tamuri@ebi.ac.uk
 *
 */
public class Attributes {
    HashMap<Key, String> attributes = Maps.newHashMap();

    public Attributes(Key nak, String s) {
        attributes.put(nak, s);
    }

    public void add(Key nak, String s) {
        attributes.put(nak, s);
    }

    public String get(Key nak) {
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

        List<Key> entries = Lists.newArrayList(attributes.keySet());

        String s = String.format("[&%s=\"%s\"", entries.get(0).toString(), attributes.get(entries.get(0)));

        for (int i = 1; i < entries.size(); i++) {
            s += String.format(",%s=\"%s\"", entries.get(i).toString(), attributes.get(entries.get(i)));
        }

        s += "]";

        return s;

    }

    public enum Key {
        REALNAME, ALLSUBS, NUMBER, NONSYNSUBS, FULL, NAME_AND_SUBS
    }
}
