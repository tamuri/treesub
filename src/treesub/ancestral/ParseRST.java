package treesub.ancestral;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import pal.tree.Node;
import pal.tree.Tree;
import pal.tree.TreeTool;
import treesub.Constants;
import treesub.Utils;
import treesub.tree.Attributes;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author tamuri@ebi.ac.uk
 *
 **/
public class ParseRST {

    private List<String> names;
    private Map<String, List<String>> seqs;
    private Tree[] trees;

    private Map<Node, Attributes> nodeAttributes = Maps.newHashMap();
    private Map<Node, List<Substitution>> nodeSubstitutions = Maps.newHashMap();


    public static void main(String[] args) throws Exception {
        ParseRST p = new ParseRST();
        p.run(args[0]);
    }

    public void run(String f) throws  Exception{
        loadRealNames(f + "/" + Constants.ALIGNMENT_NAMES);

        seqs = getSequences(f + "/" + Constants.PAML_RECONSTRUCTION_FILE);
        trees = getTrees(f + "/" + Constants.PAML_RECONSTRUCTION_FILE);
        traverse(trees[0].getRoot());

        writeResults(f);
    }

    private void loadRealNames (String f) throws  Exception{
        names = Files.readLines(new File(f), Charset.defaultCharset()) ;
    }

    private void traverse(Node n) {
        List<Substitution> substitutions;

        if (n.isLeaf()) {

            substitutions = getSubstitutions(getSequenceKey(n), getSequenceKey(n.getParent()));
            nodeSubstitutions.put(n, substitutions);


            Attributes a = getAttributes(n, substitutions);
            nodeAttributes.put(n, a);

        } else {

            for (int i = 0; i < n.getChildCount(); i++) {
                traverse(n.getChild(i));
            }

            if (!n.isRoot()) {
                substitutions = getSubstitutions(getSequenceKey(n), getSequenceKey(n.getParent()));
                nodeSubstitutions.put(n, substitutions);
                Attributes a = getAttributes(n, substitutions);
                nodeAttributes.put(n, a);
            }
        }
    }

    private void writeResults(String f) throws Exception  {
        // Write out the NEXUS format tree
        BufferedWriter out = new BufferedWriter(new FileWriter(f + "/substitutions.tree"));

        out.write("#NEXUS\n");
        out.write("begin taxa;\n");
        out.write("\tdimensions ntax=" + trees[0].getExternalNodeCount() + ";\n");
        out.write("\ttaxlabels\n");
        for (int i = 0; i < trees[0].getExternalNodeCount(); i++) {
            out.write("\t\t'" + nodeAttributes.get(trees[0].getExternalNode(i)).get(Attributes.Key.REALNAME) + "'");
            out.write(nodeAttributes.get(trees[0].getExternalNode(i)).toString());
            out.write("\n");
        }
        out.write(";\nend;\n\n");
        out.write("begin trees;\n");

        out.write("tree tree_1 = [&R] ");
        Utils.printNH(new PrintWriter(out), trees[0].getRoot(), nodeAttributes);
        out.write(";\nend;\n");
        out.close();

        // table of substitutions
        BufferedWriter subs_out = new BufferedWriter(new FileWriter(f + "/substitutions.tsv"));
        subs_out.write("branch\tsite\tcodon_from\tcodon_to\taa_from\taa_to\tstring\tnon_synonymous\n");


        for (Map.Entry<Node, List<Substitution>> e : nodeSubstitutions.entrySet()) {
            String name = nodeAttributes.get(e.getKey()).get(Attributes.Key.NUMBER);
            List<Substitution> substitutions = e.getValue();

            for (Substitution s : substitutions) {
                subs_out.write(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                        name,
                        s.site,
                        s.codonFrom,
                        s.codonTo,
                        s.aaFrom,
                        s.aaTo,
                        s.toString(),
                        s.aaFrom == s.aaTo ? "" : "*"));
            }

        }

        subs_out.close();
    }

    private Attributes getAttributes(Node node, List<Substitution> substitutions) {

        String name;
        if (node.isLeaf()) {
            name = names.get(Integer.parseInt(getSequenceKey(node).split("_")[1]) - 1);
        } else {
            name = getSequenceKey(node);
        }

        Attributes a = new Attributes(Attributes.Key.REALNAME, name);
        a.add(Attributes.Key.NUMBER, Integer.toString(getBranchNumber(node)));

        if (substitutions.size() > 0) a.add(Attributes.Key.ALLSUBS, substitutions.toString());

        Collection<Substitution> nonsynSubs = Collections2.filter(substitutions, Predicates.not(new Substitution.isSynSubPredicate()));
        if (nonsynSubs.size() > 0) a.add(Attributes.Key.NONSYNSUBS, nonsynSubs.toString());

        a.add(Attributes.Key.FULL,
                String.format("%s - %s %s",
                        a.get(Attributes.Key.NUMBER),
                        a.get(Attributes.Key.REALNAME),
                        a.get(Attributes.Key.ALLSUBS)));

        a.add(Attributes.Key.NAME_AND_SUBS,
                String.format("%s %s",
                        a.get(Attributes.Key.REALNAME),
                        a.get(Attributes.Key.NONSYNSUBS)));

        return a;
    }

    private int getBranchNumber(Node n) {
        return (n.isLeaf() ? 0 : trees[0].getExternalNodeCount()) + n.getNumber();
    }


    private String getSequenceKey(Node n) {
        if (n.isLeaf()) {
            return trees[0].getExternalNode(n.getNumber()).getIdentifier().getName();
        } else {
            return "node#" + trees[1].getInternalNode(n.getNumber()).getIdentifier().getName();
        }
    }

    private List<Substitution> getSubstitutions(String child, String parent) {

        List<String> childSeq = seqs.get(child);
        List<String> parentSeq = seqs.get(parent);

        List<Substitution> substitutions = Lists.newArrayList();

        for (int i = 0; i < childSeq.size(); i++) {
            if (!childSeq.get(i).equals(parentSeq.get(i))) {

                char aaFrom, aaTo;
                Set<Character> aaFromSet = Utils.getAminoAcidsForCodonTLA(parentSeq.get(i));
                if (aaFromSet.size() > 1) aaFrom = '*'; else aaFrom = aaFromSet.iterator().next();

                Set<Character> aaToSet = Utils.getAminoAcidsForCodonTLA(childSeq.get(i));
                if (aaToSet.size() > 1) aaTo = '*'; else aaTo = aaToSet.iterator().next();

                Substitution s = new Substitution(i + 1, parentSeq.get(i), childSeq.get(i), aaFrom, aaTo);
                substitutions.add(s);

            }
        }

        return substitutions;
    }

    private Tree[] getTrees(String file) throws Exception {
        Tree[] trees = new Tree[2];

        BufferedReader reader = Files.newReader(new File(file), Charsets.US_ASCII);

        while (!reader.readLine().startsWith("Ancestral reconstruction by")) { /* empty */ }

        String line;

        // skip all lines until we read the first tree
        while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        // this is the true tree - use this to get the true branch lengths
        trees[0] = TreeTool.readTree(new StringReader(line));

        // skip all lines until we have read two more trees
        for (int i = 0; i < 2; i++) while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        // this third tree is tree with the branches labeled - use this to get node/branch names
        trees[1] = TreeTool.readTree(new StringReader(line));

        reader.close();

        return trees;
    }

    private Map<String, List<String>> getSequences(String file) throws Exception{
        Map<String, List<String>> sequences = Maps.newHashMap();

        // Read the sequences (in particular, the reconstructed nodes)
        BufferedReader reader = Files.newReader(new File(file), Charsets.US_ASCII);

        while (!reader.readLine().startsWith("List of extant and reconstructed sequences")) { /* empty */ }

        reader.readLine(); // Skip blank line
        reader.readLine(); // Skip header
        reader.readLine(); // Skip black line

        String line;

        // Until we reach the end of the sequences
        while ((line = reader.readLine()).trim().length() > 0) {
            List<String> parts = Lists.newArrayList(line.split("\\s+"));

            String key, full;
            if (parts.get(0).equals("node")) {
                // This is a reconstructed sequence of ancestral node
                key = "node" + parts.get(1);
                parts.remove(0); // 'node'
                parts.remove(0); // '#n'

                full = Joiner.on("").join(parts);
            } else {
                // This is a extant sequence
                key = parts.get(0);
                parts.remove(0); // sequence name
                full = Joiner.on("").join(parts);
            }

            List<String> sequence = Lists.newArrayList();
            for (int i = 0; i < full.length(); i += Constants.CODON_LENGTH) {
                sequence.add(full.substring(i, i + Constants.CODON_LENGTH));
            }
            sequences.put(key, sequence);
        }

        /*
        for (Map.Entry<String, List<String>> e : sequences.entrySet()) {
            System.out.printf("%s\t%s\n", e.getKey(), Joiner.on(",").join(e.getValue()));
        }
        */

        reader.close();

        return sequences;
    }
}
