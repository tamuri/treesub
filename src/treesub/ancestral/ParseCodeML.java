package treesub.ancestral;

import com.google.common.base.Charsets;
import com.google.common.collect.*;
import com.google.common.io.Files;
import pal.tree.Tree;
import pal.tree.TreeTool;

import java.io.*;
import java.util.*;

public class ParseCodeML {
    public Map<String, List<Substitution>> allBranchSubs = Maps.newHashMap();
    public Tree treeBranchLengths;
    public Tree treeBranchNames;

    void run(File rstFile) throws Exception {
        String line;

        // Read and parse the PAML file
        BufferedReader reader = Files.newReader(rstFile, Charsets.US_ASCII);

        // skip all lines until we get to this section
        while (!reader.readLine().startsWith("Ancestral reconstruction by CODONML.")) { /* empty */ }

        // skip all lines until we read the first tree
        while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        // this is the true tree - use this to get the true branch lengths
        treeBranchLengths = TreeTool.readTree(new StringReader(line));

        // skip all lines until we have read two more trees
        for (int i = 0; i < 2; i++) while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        // this third tree is tree with the branches labeled - use this to get node/branch names
        treeBranchNames = TreeTool.readTree(new StringReader(line));

        // skip all lines until we get to list of branch substitutions
        while (!reader.readLine().startsWith("Summary of changes along branches")) { /* empty */ }

        // between here and 'list of extant...' are the ancestral reconstruction substitutions
        while (!(line = reader.readLine()).startsWith("List of extant and reconstructed sequences")) {
            if (line.startsWith("Branch")) {
                // get the branch information
                String branchName  = line.split(" ")[3];
                //System.out.printf("Branch name: %s\n", branchName);

                // skip a blank line
                reader.readLine();

                // this will be either a substitution or a blank line
                line = reader.readLine();

                // if it's a substitution
                if (line.trim().length() > 0) {

                    // collect all the substitutions for this branch
                    List<Substitution> substitutions = Lists.newArrayList();

                    // keep reading substitutions until we reach an empty line
                    do {

                        String[] split = line.split("\\s+");
                        
                        Substitution sub = new Substitution(Integer.parseInt(split[1]), // site
                                split[2], // codon from
                                split[6], // codon to
                                split[3].charAt(1), // amino acid from
                                split[7].charAt(1)); // amino acid to

                        substitutions.add(sub);

                    } while ((line = reader.readLine()).trim().length() > 0);

                    allBranchSubs.put(branchName, substitutions);
                }
            }
        }

        // we're done reading and parsing the PAML output
        reader.close();
    }
}
