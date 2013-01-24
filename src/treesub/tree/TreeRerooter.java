package treesub.tree;

import pal.tree.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * @author tamuri@ebi.ac.uk
 *
 * The class is used to root the best tree found by RAxML using the outgroup sequence.
 * The outgroup sequence is always "seq_1" which is the first sequence in the original FASTA file.
 */
public class TreeRerooter {
    private static String OUTGROUP_SEQUENCE_NAME = "seq_1";

    public void reroot(String filename, String outfile) throws Exception {
        // Reroot the tree on "seq_1" - the first sequence in the original FASTA file must be the outgroup
        // We need to set this so that ancestral reconstruction is done correctly in PAML
        Tree tree = TreeTool.readTree(new FileReader(filename));
        Tree rerootedTree = TreeManipulator.getRootedBy(tree, new String[]{OUTGROUP_SEQUENCE_NAME});
        PrintWriter pw = new PrintWriter(new FileWriter(outfile));
        TreeUtils.printNH(rerootedTree, pw);
        pw.close();
    }
}
