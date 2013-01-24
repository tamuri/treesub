package treesub.ancestral;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import pal.tree.Node;
import pal.tree.Tree;
import pal.tree.TreeTool;
import treesub.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Asif Tamuri (atamuri@nimr.mrc.ac.uk)
 */
public class ParseBaseML {
    void run(File rstFile) throws Exception {
        String line;
        BufferedReader reader = Files.newReader(rstFile, Charsets.US_ASCII);

        while (!reader.readLine().startsWith("Ancestral reconstruction by BASEML.")) { /* empty */ }
        // skip all lines until we read the first tree
        while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        // this is the true tree - use this to get the true branch lengths
        treeBranchLengths = TreeTool.readTree(new StringReader(line));

        // skip all lines until we have read two more trees
        for (int i = 0; i < 2; i++) while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        // this third tree is tree with the branches labeled - use this to get node/branch names
        treeBranchNames = TreeTool.readTree(new StringReader(line));

        while (!reader.readLine().startsWith("Prob of best state at each node, listed by site")) { /* empty */ }
        // Skip three comment lines
        reader.readLine();
        reader.readLine();
        reader.readLine();

        // For each site
        while ((line = reader.readLine()).trim().length() > 0) {
            // First line is the nucleotide at the first position. we need the second and third positions too
            String line2 = reader.readLine();
            String line3 = reader.readLine();

            // Create a list of codons for this site
            String[] parts1 = line.trim().split("\\s+");
            String[] parts2 = line2.trim().split("\\s+");
            String[] parts3 = line3.trim().split("\\s+");

            List<String> siteCodons = Lists.newArrayList();

            // Codons at the tips come first
            for (int i = 0; i < parts1[3].length() - 1; i++) {
                String c = new String(new char[]{parts1[3].charAt(i), parts2[3].charAt(i), parts3[3].charAt(i)});
                siteCodons.add(c);
            }

            // Codons at internal nodes come next, but are formatted differently
            for (int i = 4; i < parts1.length; i++) {
                String c = new String(new char[]{parts1[i].charAt(0), parts2[i].charAt(0), parts3[i].charAt(0)});
                siteCodons.add(c);
            }


            allCodons.add(siteCodons);
        }

        reader.close();

        // Traverse the tree and fill the PAML branch -> substitution map
        collectSubsAlongTree(treeBranchNames, treeBranchNames.getRoot());

        // Let's see what we have
        /*System.out.printf("%s sites, %s nodes\n", allCodons.size(), allCodons.get(0).size());
        for (Map.Entry<String, List<Substitution>> e : allBranchSubs.entrySet()) {
            for (Substitution s : e.getValue()) {
                System.OUT.printf("%s %s %s (%s) -> %s (%s)\n", e.getKey(), s.site, s.codonFrom, s.aaFrom, s.codonTo, s.aaTo);
            }
        }*/
    }

    private void collectSubsAlongTree(Tree t, Node n) {
        //
        int fromNode;
        if (n.isLeaf()) {
            fromNode = Integer.parseInt(n.getIdentifier().getName().split("_")[0]);
        } else {
            fromNode = Integer.parseInt(n.getIdentifier().getName());
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            Node c = n.getChild(i);

            int toNode;
            if (c.isLeaf()) {
                toNode = Integer.parseInt(c.getIdentifier().getName().split("_")[0]);
            } else {
                toNode = Integer.parseInt(c.getIdentifier().getName());
                collectSubsAlongTree(t, c);
            }

            List<Substitution> branchSubs = Lists.newArrayList();

            // For each site, check for any changes
            for (int j = 0; j < allCodons.size(); j++) {
                if (!allCodons.get(j).get(fromNode - 1).equals(allCodons.get(j).get(toNode - 1))) {
                    String codonFrom = allCodons.get(j).get(fromNode - 1);
                    String codonTo = allCodons.get(j).get(toNode - 1);

                    char aaFrom, aaTo;
                    Set<Character> aaFromSet = Utils.getAminoAcidsForCodonTLA(codonFrom);
                    if (aaFromSet.size() > 1) aaFrom = '*'; else aaFrom = aaFromSet.iterator().next();

                    Set<Character> aaToSet = Utils.getAminoAcidsForCodonTLA(codonTo);
                    if (aaToSet.size() > 1) aaTo = '*'; else aaTo = aaToSet.iterator().next();

                    branchSubs.add(new Substitution(j + 1, codonFrom, codonTo, aaFrom, aaTo));
                }
            }

            // Store all the changes that happened along this branch
            allBranchSubs.put(fromNode + "" + toNode, branchSubs);
        }
    }



    /**
     * Each position in this class member list is a site. Each value is a list of codon states at each of the nodes,
     * first the codons at the tips, and then the reconstructed codons at internal nodes. The position of the codon in
     * the list is the same as the PAML node number (although offset by 0, rather than 1)
     * i.e.
     * [ site 1, [ tip1, tip2, tip3, ..., tipN, node150, node151, node152, ..., nodeN ], site 2, [ .... ] ... siteN ]
     */
    private List<List<String>> allCodons = Lists.newArrayList();

    /**
     * This class member map is a lookup on PAML branch label (of the form x..y) to a list of substitutions along that
     * branch. We store it this way as this is how the information is presented when doing ancestral reconstruction in
     * codeml. i.e. to have parity between baseml and codeml, and we can share the code after parsing the respective
     * 'rst' file from each program
     */
    public Map<String, List<Substitution>> allBranchSubs = Maps.newHashMap();

    public Tree treeBranchLengths;

    public Tree treeBranchNames;
}
