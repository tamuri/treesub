package treesub.ancestral;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import pal.tree.Tree;
import pal.tree.TreeTool;
import pal.tree.TreeUtils;
import treesub.Constants;

import java.io.*;

/*
List of extant and reconstructed sequences

11    759

seq_1             MERIKELRNL MSQSRTREIL TKTTVDHM...
seq_2             MERIKELRNL MSQSRTREIL TKTTVDHM...
seq_3             MERIKELRDL MSQSRTREIL TKTTVDHM...
seq_4             MERIKELRDL MSQSRTREIL TKTTVDHM...
seq_5             MERIKELRNL MSQSRTREIL TKTTVDHM...
seq_6             MERIKELRDL MSQSRTREIL TKTTVDHM...
node #7           MERIKELRNL MSQSRTREIL TKTTVDHM...
node #8           MERIKELRNL MSQSRTREIL TKTTVDHM...
node #9           MERIKELRNL MSQSRTREIL TKTTVDHM...
node #10          MERIKELRDL MSQSRTREIL TKTTVDHM...
node #11          MERIKELRDL MSQSRTREIL TKTTVDHM...
*/

public class ParsePamlRst {

    public void parse(String inFile, String outDir) throws Exception {
        String line;
        BufferedReader reader = Files.newReader(new File(inFile), Charsets.US_ASCII);

        while (!reader.readLine().startsWith("Ancestral reconstruction by")) { /* empty */ } // could be baseml or codeml
        while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        Tree tree = TreeTool.readTree(new StringReader(line));

        PrintWriter pw = new PrintWriter(outDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".ancestral.tree");
        TreeUtils.printNH(tree, pw);
        pw.close();
        
        while (!reader.readLine().startsWith("List of extant and reconstructed sequences")) { /* empty */ }

        BufferedWriter writer = Files.newWriter(new File(outDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment.ancestral"), Charsets.US_ASCII);

        // skip three lines
        reader.readLine();
        reader.readLine();
        reader.readLine();


        while ((line = reader.readLine()).trim().length() > 0) {
            if (line.startsWith("node #")) {
                line = line.replaceAll("node #", "node_");
                writer.write(line);
                writer.newLine();
            }
        }

        reader.close();
        writer.close();
    }
}
