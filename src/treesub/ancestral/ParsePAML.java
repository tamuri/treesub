package treesub.ancestral;

import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import pal.tree.Node;
import pal.tree.Tree;
import treesub.Utils;
import treesub.tree.NodeAttributes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Asif Tamuri (atamuri@nimr.mrc.ac.uk)
 *
 * Takes the output of codeml or baseml that has used the RateAncestor=1 to perform ancestral state reconstruction.
 * Specifically, we need two trees and a list of substitutions along each branch from the 'rst' output file. These are
 * provided by ParseBaseML and ParseCodeML classes. Once we have these, an annotated tree and .tsv table of
 * substitutions is created.
 *
 * TODO: We need to set this up so a user can use *either* codeml or baseml. Currently fixed to baseml...but the codeml
 * code is ready.
 */
public class ParsePAML {

    private boolean showTipSubs = true;

    BiMap<String, Integer> pamlToPalBranch = HashBiMap.create();
    Map<Node, NodeAttributes> nodeAttributes = Maps.newHashMap();

    public void run(String dir) throws Exception {
        String namesFile = dir + "/alignment.names";
        String outFile = dir + "/substitutions.tree";
        String subsFile = dir + "/substitutions.tsv";
        // String alignmentFile = dir + "/alignment.phylip";

        // TODO: what about codeml??
        parseBaseML(dir);

        // this creates a mapping between paml branch names (of the form "x..y") to pal branches
        mapPamlToPalBranch(treeBranchNames, treeBranchNames.getRoot());

        // here are the real names for the sequences
        List<String> names = Files.readLines(new File(namesFile), Charset.defaultCharset()) ;

        // annotate the tips
        for (int i = 0; i < treeBranchNames.getExternalNodeCount(); i++) {
            Node node = treeBranchNames.getExternalNode(i);

            int seqnum = Integer.parseInt(node.getIdentifier().getName().split("_")[0]);

            NodeAttributes nat = new NodeAttributes(NodeAttributes.NodeAttributeKey.REALNAME, names.get(seqnum - 1));
            nat.add(NodeAttributes.NodeAttributeKey.NUMBER, Integer.toString(getBranchNumber(treeBranchNames, node)));

            String pamlBranch = pamlToPalBranch.inverse().get(getBranchNumber(treeBranchNames, node));

            if (allBranchSubs.containsKey(pamlBranch) && showTipSubs) {
                if (allBranchSubs.get(pamlBranch).size() > 0)
                    nat.add(NodeAttributes.NodeAttributeKey.ALLSUBS, allBranchSubs.get(pamlBranch).toString());

                Collection<Substitution> nonsynSubs = Collections2.filter(allBranchSubs.get(pamlBranch), Predicates.not(new Substitution.isSynSubPredicate()));
                if (nonsynSubs.size() > 0)
                    nat.add(NodeAttributes.NodeAttributeKey.NONSYNSUBS, nonsynSubs.toString());
            }


            nat.add(NodeAttributes.NodeAttributeKey.FULL,
                    String.format("%s - %s %s",
                            nat.get(NodeAttributes.NodeAttributeKey.NUMBER),
                            nat.get(NodeAttributes.NodeAttributeKey.REALNAME),
                            nat.get(NodeAttributes.NodeAttributeKey.ALLSUBS)));

            nat.add(NodeAttributes.NodeAttributeKey.NAME_AND_SUBS,
                    String.format("%s %s",
                            nat.get(NodeAttributes.NodeAttributeKey.REALNAME),
                            nat.get(NodeAttributes.NodeAttributeKey.NONSYNSUBS)));

            nodeAttributes.put(node, nat);

            node.setBranchLength(treeBranchLengths.getExternalNode(i).getBranchLength());
        }

        // annotate the internal, ancestor, nodes
        for (int i = 0; i < treeBranchNames.getInternalNodeCount(); i++) {
            Node node = treeBranchNames.getInternalNode(i);

            NodeAttributes nat = new NodeAttributes(NodeAttributes.NodeAttributeKey.NUMBER, Integer.toString(getBranchNumber(treeBranchNames, node)));

            String pamlBranch = pamlToPalBranch.inverse().get(getBranchNumber(treeBranchNames, node));

            if (allBranchSubs.containsKey(pamlBranch)) {
                nat.add(NodeAttributes.NodeAttributeKey.ALLSUBS, allBranchSubs.get(pamlBranch).toString());
                Collection<Substitution> nonsynSubs = Collections2.filter(allBranchSubs.get(pamlBranch), Predicates.not(new Substitution.isSynSubPredicate()));
                if (nonsynSubs.size() > 0)
                    nat.add(NodeAttributes.NodeAttributeKey.NONSYNSUBS, nonsynSubs.toString());
            }

            nat.add(NodeAttributes.NodeAttributeKey.FULL,
                    String.format("%s - %s",
                            nat.get(NodeAttributes.NodeAttributeKey.NUMBER),
                            nat.get(NodeAttributes.NodeAttributeKey.ALLSUBS)));

            nodeAttributes.put(node, nat);

            node.setBranchLength(treeBranchLengths.getInternalNode(i).getBranchLength());
        }

        // Write out the NEXUS format tree
        // BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out));
        BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

        out.write("#NEXUS\n");
        out.write("begin taxa;\n");
        out.write("\tdimensions ntax=" + treeBranchNames.getExternalNodeCount() + ";\n");
        out.write("\ttaxlabels\n");
        for (int i = 0; i < treeBranchNames.getExternalNodeCount(); i++) {
            out.write("\t\t'" + nodeAttributes.get(treeBranchNames.getExternalNode(i)).get(NodeAttributes.NodeAttributeKey.REALNAME) + "'");
            out.write(nodeAttributes.get(treeBranchNames.getExternalNode(i)).toString());
            out.write("\n");
        }
        out.write(";\nend;\n\n");
        out.write("begin trees;\n");

        /*out.write("\ttranslate\n");
        out.write(String.format("\t\t%s '%s'",
                nodeAttributes.get(treeBranch.getExternalNode(0)).get(NodeAttributeKey.NUMBER),
                nodeAttributes.get(treeBranch.getExternalNode(0)).get(NodeAttributeKey.REALNAME)));
        for (int i = 1; i < treeBranch.getExternalNodeCount(); i++) {
            out.write(String.format(",\n\t\t%s '%s'",
                    nodeAttributes.get(treeBranch.getExternalNode(i)).get(NodeAttributeKey.NUMBER),
                    nodeAttributes.get(treeBranch.getExternalNode(i)).get(NodeAttributeKey.REALNAME)
                    ));
        }
        out.write("\n\t\t;\n");*/

        out.write("tree tree_1 = [&R] ");
        Utils.printNH(new PrintWriter(out), treeBranchNames.getRoot(), nodeAttributes);
        out.write(";\nend;\n");
        out.close();

        // table of substitutions
        BufferedWriter subs_out = new BufferedWriter(new FileWriter(subsFile));
        subs_out.write("branch\tsite\tcodon_from\tcodon_to\taa_from\taa_to\tstring\tnon_synonymous\n");
        for (Map.Entry<String,List<Substitution>> e : allBranchSubs.entrySet()) {
            String branch = pamlToPalBranch.get(e.getKey()).toString();
            List<Substitution> subs = e.getValue();

            for (Substitution s : subs) {
                subs_out.write(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                        branch,
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

    private void mapPamlToPalBranch(Tree t, Node n) {
        // for each child of this node (usually 2!)
        for (int i = 0; i < n.getChildCount(); i++) {
            // construct the branch name for the parent node to the child
            // PAML labels tips as <nodenumber>_<sequencename>. We just need the PAML node number
            String id = n.getChild(i).getIdentifier().getName();
            String branchName = n.getIdentifier().getName() + "" + (n.getChild(i).isLeaf() ? id.split("_")[0] : id);

            pamlToPalBranch.put(branchName, getBranchNumber(t, n.getChild(i)));
            mapPamlToPalBranch(t, n.getChild(i));
        }
    }

    private int getBranchNumber(Tree t, Node n) {
        return (n.isLeaf() ? 0 : t.getExternalNodeCount()) + n.getNumber();
    }

    private void parseCodeML(String dir) throws Exception {
        File rstFile = new File(dir + "/rst");
        ParseCodeML parser = new ParseCodeML();
        parser.run(rstFile);
        this.treeBranchLengths = parser.treeBranchLengths;
        this.treeBranchNames = parser.treeBranchNames;
        this.allBranchSubs = parser.allBranchSubs;
    }

    private void parseBaseML(String dir) throws Exception {
        File rstFile =  new File(dir + "/rst");
        ParseBaseML parser = new ParseBaseML();
        parser.run(rstFile);
        this.treeBranchLengths = parser.treeBranchLengths;
        this.treeBranchNames = parser.treeBranchNames;
        this.allBranchSubs = parser.allBranchSubs;
    }

    private Tree treeBranchLengths;
    private Tree treeBranchNames;
    private Map<String, List<Substitution>> allBranchSubs;

}
