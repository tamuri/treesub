package treesub.ancestral;

import com.google.common.base.Charsets;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import pal.alignment.Alignment;
import pal.alignment.AlignmentReaders;
import pal.datatype.DataTypeTool;
import pal.datatype.MolecularDataType;
import pal.tree.Node;
import pal.tree.Tree;
import pal.tree.TreeTool;
import treesub.Utils;
import treesub.tree.NodeAttributes;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeSubBuilder {
    Alignment sequences, ancestral;
    Tree tree;
    int nodeNumber = 0;
    List<String> sequenceLookup = Lists.newArrayList();
    List<String> ancestralLookup = Lists.newArrayList();
    MolecularDataType dataType;
    String dataTypeString;
    Map<String, List<Substitution>> substitutions = Maps.newHashMap();
    List<String> names;
    Map<Node, NodeAttributes> nodeAttributes = Maps.newHashMap();

    public static void main(String[] args) throws Exception {
        TreeSubBuilder b = new TreeSubBuilder();
        b.setDataType("CODONS");
        b.loadSequences("/Users/atamuri/Documents/work/111108 Tree Annotator for Div. of Virology/etc/examples/H3HASO-251111/treesub.alignment.raxml.phylip");
        b.loadAncestralStates("/Users/atamuri/Documents/work/111108 Tree Annotator for Div. of Virology/etc/examples/H3HASO-251111/treesub.alignment.ancestral");
        b.loadTree("/Users/atamuri/Documents/work/111108 Tree Annotator for Div. of Virology/etc/examples/H3HASO-251111/treesub.ancestral.tree");
        b.loadSequenceNames("/Users/atamuri/Documents/work/111108 Tree Annotator for Div. of Virology/etc/examples/H3HASO-251111/treesub.alignment.names");
        b.build();
    }

    public void loadSequenceNames(String filename) throws Exception {
        this.names = Files.readLines(new File(filename), Charset.defaultCharset()) ;
    }

    public void setDataType(String type) {
        this.dataTypeString = type;
        if (type.equals("DNA")) {
            this.dataType = DataTypeTool.getNucleotides();
        } else if (type.equals("CODONS")) {
            this.dataType = DataTypeTool.getNucleotides();
        } else if (type.equals("AMINOACIDS")) {
            this.dataType = DataTypeTool.getUniverisalAminoAcids();
        }
    }

    public void loadSequences(String filename) throws Exception {
        this.sequences = AlignmentReaders.readPhylipClustalAlignment(new FileReader(filename), dataType);
    }

    public void loadAncestralStates(String filename) throws Exception {
        this.ancestral = AlignmentReaders.readPhylipClustalAlignment(new FileReader(filename), dataType);
    }

    public void loadTree(String filename) throws Exception {
        this.tree = TreeTool.readTree(new FileReader(filename));
    }

    public void build() throws  Exception{
        // Label internal nodes with their node number ala PAML
        nodeNumber = tree.getExternalNodeCount() + 1;
        numberInternalNodes(tree.getRoot());

        // Make a lookup table for each sequence and ancestral sequence
        for (int i = 0; i < sequences.getSequenceCount(); i++) { sequenceLookup.add(sequences.getIdentifier(i).getName()); }
        for (int i = 0; i < ancestral.getSequenceCount(); i++) { ancestralLookup.add(ancestral.getIdentifier(i).getName()); }

        System.out.println(sequenceLookup.toString());
        System.out.println(ancestralLookup.toString());


        // Traverse from root, marking all changes
        getAllSubstitutions(tree.getRoot());
        
        // Now write the final tree!
        buildTree();
    }

    private void buildTree() throws  Exception{
        // annotate the tips
        for (int i = 0; i < tree.getExternalNodeCount(); i++) {
            Node node = tree.getExternalNode(i);

            int seqnum = Integer.parseInt(node.getIdentifier().getName().split("_")[1]);

            NodeAttributes nat = new NodeAttributes(NodeAttributes.NodeAttributeKey.REALNAME, names.get(seqnum - 1));
            nat.add(NodeAttributes.NodeAttributeKey.NUMBER, Integer.toString(seqnum));

            String branchName = node.getParent().getIdentifier().getName() + ".." + node.getIdentifier().getName();
            
            if (substitutions.containsKey(branchName)) {
                if (substitutions.get(branchName).size() > 0)
                    nat.add(NodeAttributes.NodeAttributeKey.ALLSUBS, substitutions.get(branchName).toString());

                Collection<Substitution> nonsynSubs = Collections2.filter(substitutions.get(branchName), Predicates.not(new Substitution.isSynSubPredicate()));
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

        }

        // annotate the internal, ancestor, nodes
        for (int i = 0; i < tree.getInternalNodeCount(); i++) {
            
            Node node = tree.getInternalNode(i);
            
            if (node.isRoot()) continue;

            int nodeNumber = Integer.parseInt(tree.getInternalNode(i).getIdentifier().getName().split("_")[1]);

            NodeAttributes nat = new NodeAttributes(NodeAttributes.NodeAttributeKey.NUMBER, Integer.toString(nodeNumber));

            String branchName = node.getParent().getIdentifier().getName() + ".." + node.getIdentifier().getName();
            
            if (substitutions.containsKey(branchName)) {
                nat.add(NodeAttributes.NodeAttributeKey.ALLSUBS, substitutions.get(branchName).toString());
                Collection<Substitution> nonsynSubs = Collections2.filter(substitutions.get(branchName), Predicates.not(new Substitution.isSynSubPredicate()));
                if (nonsynSubs.size() > 0)
                    nat.add(NodeAttributes.NodeAttributeKey.NONSYNSUBS, nonsynSubs.toString());
            }

            nat.add(NodeAttributes.NodeAttributeKey.FULL,
                    String.format("%s - %s",
                            nat.get(NodeAttributes.NodeAttributeKey.NUMBER),
                            nat.get(NodeAttributes.NodeAttributeKey.ALLSUBS)));

            nodeAttributes.put(node, nat);

        }

        // Write out the NEXUS format tree
        // BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out));
        BufferedWriter out = new BufferedWriter(new FileWriter("/Users/atamuri/Documents/work/111108 Tree Annotator for Div. of Virology/etc/examples/H3HASO-251111/treesub.substitutions.tree"));

        out.write("#NEXUS\n");
        out.write("begin taxa;\n");
        out.write("\tdimensions ntax=" + tree.getExternalNodeCount() + ";\n");
        out.write("\ttaxlabels\n");
        for (int i = 0; i < tree.getExternalNodeCount(); i++) {
            out.write("\t\t'" + nodeAttributes.get(tree.getExternalNode(i)).get(NodeAttributes.NodeAttributeKey.REALNAME) + "'");
            out.write(nodeAttributes.get(tree.getExternalNode(i)).toString());
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
        Utils.printNH(new PrintWriter(out), tree.getRoot(), nodeAttributes);
        out.write(";\nend;\n");
        out.close();

        // table of substitutions
        String subFile = "/Users/atamuri/Documents/work/111108 Tree Annotator for Div. of Virology/etc/examples/H3HASO-251111/treesub.substitutions.csv";
        // BufferedWriter subs_out = new BufferedWriter(new FileWriter());
        BufferedWriter subs_out = Files.newWriter(new File(subFile), Charsets.US_ASCII);
        subs_out.write("branch,site,codon_from,codon_to,aa_from,aa_to,string,non_synonymous\n");
        for (Map.Entry<String,List<Substitution>> e : substitutions.entrySet()) {
            String branch = e.getKey();

            List<Substitution> subs = e.getValue();

            for (Substitution s : subs) {
                subs_out.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                        "\"" + branch + "\"",
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


    private void getAllSubstitutions(Node node) {

        if (node.isRoot()) System.out.printf("%s is root!\n", node.getIdentifier().getName());
        
        for (int i = 0; i < node.getChildCount(); i++) {
            Node child = node.getChild(i);

            System.out.printf("From %s -> %s\n", node.getIdentifier().getName(), child.getIdentifier().getName());

            List<Substitution> branchSubs = Lists.newArrayList();
            
            Alignment childAlign;
            int childSequence;
            if (child.isLeaf()) {
                childAlign = sequences;
                childSequence = sequenceLookup.indexOf(child.getIdentifier().getName());
                System.out.printf("is leaf\n");
            } else {
                System.out.printf("is not leaf\n");
                childAlign = ancestral;
                childSequence = ancestralLookup.indexOf(child.getIdentifier().getName());
            }


            int ancestralSequence = ancestralLookup.indexOf(node.getIdentifier().getName());




            int sites;
            if (dataTypeString.equals("CODONS")) {
                sites = sequences.getSiteCount() / 3;
            } else {
                sites = sequences.getSiteCount();
            }
            
            for (int j = 0; j < sites; j++) {

                if (dataTypeString.equals("CODONS")) {
                    int nucleotideSite = j * 3;
                    
                    String ancestralState = new String(new char[]{
                            ancestral.getData(ancestralSequence, nucleotideSite),
                            ancestral.getData(ancestralSequence, nucleotideSite + 1),
                            ancestral.getData(ancestralSequence, nucleotideSite + 2)
                    });
                    
                    String childState = new String(new char[]{
                            childAlign.getData(childSequence, nucleotideSite),
                            childAlign.getData(childSequence, nucleotideSite + 1),
                            childAlign.getData(childSequence, nucleotideSite + 2)
                    });

                    Set<Character> ancestralAminoAcids = Utils.getAminoAcidsForCodonTLA(ancestralState);
                    Set<Character> childAminoAcids = Utils.getAminoAcidsForCodonTLA(childState);

                    if (!ancestralState.equals(childState)) {


                        System.out.printf("%s%s%s\t", ancestralAminoAcids.toString(), j + 1, childAminoAcids.toString());
                        System.out.printf("%s%s%s\n", ancestralState, j + 1, childState);

                        char aaFrom = ancestralAminoAcids.size() > 1 ? '*' : ancestralAminoAcids.iterator().next();
                        char aaTo = childAminoAcids.size() > 1 ? '*' : childAminoAcids.iterator().next();

                        branchSubs.add(new Substitution(j + 1, ancestralState, childState, aaFrom, aaTo));
                   }

                } else {
                    if (ancestral.getData(ancestralSequence, j) != childAlign.getData(childSequence, j)) {
                        System.out.printf("%s%s%s\n", ancestral.getData(ancestralSequence, j), j + 1, childAlign.getData(childSequence, j));

                        branchSubs.add(new Substitution(j + 1, Character.toString(ancestral.getData(ancestralSequence, j)), Character.toString(childAlign.getData(childSequence, j)), ancestral.getData(ancestralSequence, j), childAlign.getData(childSequence, j)));
                    }
                }

            }

            String branchName = node.getIdentifier().getName() + ".." + child.getIdentifier().getName();
            substitutions.put(branchName, branchSubs);
            
            getAllSubstitutions(child);
            
        }
    }
    
    private void numberInternalNodes(Node n) {
        if (n.isRoot() && !n.isLeaf()) {
            n.getIdentifier().setName("node_" + nodeNumber);
            nodeNumber++;
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            if (!n.getChild(i).isLeaf()) {
                n.getChild(i).getIdentifier().setName("node_" + nodeNumber);
                nodeNumber++;
                numberInternalNodes(n.getChild(i));
            }
        }
    }
}
