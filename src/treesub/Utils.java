package treesub;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.primitives.Chars;
import org.biojava3.core.sequence.DNASequence;
import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.template.AbstractSequence;
import pal.alignment.Alignment;
import pal.datatype.CodonTableFactory;
import pal.datatype.Codons;
import pal.io.FormattedOutput;
import pal.tree.Node;
import treesub.tree.NodeAttributes;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Asif Tamuri (atamuri@nimr.mrc.ac.uk)
 */
public class Utils {
    /**
     * This function takes a nucleotide triplet string (i.e. a codon) and returns the resulting
     * amino acid. It returns a Set because the triplet can specify IUPAC ambiguous nucleotides,
     * for example, N (any nucleotide) or W (A or T) etc.
     *
     * @param tla The tree letter nucleotide triplet. Can include IUPAC ambiguous nucleotides
     * @return A Set of all amino acid characters that are coded by this nucleotide triplet
     */
    public static Set<Character> getAminoAcidsForCodonTLA(String tla) {
        // If we don't have any ambiguous nucleotides in our triplet
        if (!containsAmbiguousNucleotide(tla)) {
            // Just a straightforward lookup
            int codonIndex = Codons.getCodonIndexFromNucleotides(tla.toCharArray());
            char aaChar = CodonTableFactory.createUniversalTranslator().getAminoAcidCharFromCodonIndex(codonIndex);
            return Sets.newHashSet(aaChar);
        }

        // We're still here if we found ambiguous nucleotides in the triplet
        // First, get all the possible nucleotides at each of the three positions (i.e. resolve the ambiguous nucleotides)
        List<List<Character>> positions = Lists.newArrayList();
        positions.add(null);
        positions.add(null);
        positions.add(null);

        for (int i = 0; i < 3; i++) {
            char c = tla.charAt(i);
            if (IUPAC_ALL_AMBIGUOUS.contains(c)) {
                positions.set(i, IUPAC_AMBIGUOUS_LOOKUP.get(c));
            } else {
                positions.set(i, Chars.asList(c));
            }
        }

        // Now build a list of all the possible codons from all possible nucleotides at the three positions
        List<String> allPossibleCodons = Lists.newArrayList();
        for (int i = 0; i < positions.get(0).size(); i++) {
            for (int j = 0; j < positions.get(1).size(); j++) {
                for (int k = 0; k < positions.get(2).size(); k++) {
                    String s = String.valueOf(new char[]{positions.get(0).get(i), positions.get(1).get(j), positions.get(2).get(k)});
                    allPossibleCodons.add(s);
                }
            }
        }

        // Iterate through all possible codons and collect the amino acid for each
        Set<Character> allAminoAcids = Sets.newHashSet();
        for (String c : allPossibleCodons) {
            // We should only ever have one amino acid in this case (i.e. the ambiguities have been resolved)
            allAminoAcids.add(getAminoAcidsForCodonTLA(c).iterator().next());
        }

        return allAminoAcids;
    }

    public static boolean containsAmbiguousNucleotide(String tla) {
        // how about something like:
        // return !("TCAG".indexOf(tla.charAt(0)) > -1 && "TCAG".indexOf(tla.charAt(1)) > -1 && "TCAG".indexOf(tla.charAt(2)) > -1);

        boolean isAmbiguous = false;
        for (char c : IUPAC_ALL_AMBIGUOUS) {
            if (tla.charAt(0) == c || tla.charAt(1) == c || tla.charAt(2) == c) {
                isAmbiguous = true;
                break;
            }
        }
        return isAmbiguous;
    }

    // Lifted from PAL source code, so we can output NEXUS style trees with annotations (for Figtree)
    public static void printNH(PrintWriter out, Node node, Map<Node, NodeAttributes> nodeAttributes) {
        if (!node.isLeaf()) {
            out.print("(");

            for (int i = 0; i < node.getChildCount(); i++) {
                if (i != 0) {
                    out.print(",");
                }

                printNH(out, node.getChild(i), nodeAttributes);
            }

            out.print(")");
        }

        if (!node.isRoot()) {
            if (node.isLeaf()) {
                // String id = node.getIdentifier().toString();
                String id = nodeAttributes.get(node).get(NodeAttributes.NodeAttributeKey.REALNAME);
                out.print("'" + id + "'");
            } else {
                if (nodeAttributes.get(node).size() > 0) {
                    out.print(nodeAttributes.get(node).toString());
                }
            }

            out.print(":");
            FormattedOutput.getInstance().displayDecimal(out, node.getBranchLength(), 7);
        }
    }

    private static final Map<Character, List<Character>> IUPAC_AMBIGUOUS_LOOKUP = new ImmutableMap.Builder<Character, List<Character>>()
            .put('N', Chars.asList("TCAG".toCharArray()))
            .put('-', Chars.asList("TCAG".toCharArray()))
            .put('R', Chars.asList("GA".toCharArray()))
            .put('W', Chars.asList("AT".toCharArray()))
            .put('Y', Chars.asList("CT".toCharArray()))
            .put('M', Chars.asList("AC".toCharArray()))
            .put('K', Chars.asList("GT".toCharArray()))
            .put('S', Chars.asList("GC".toCharArray()))
            .put('H', Chars.asList("ACT".toCharArray()))
            .put('B', Chars.asList("CGT".toCharArray()))
            .put('V', Chars.asList("ACG".toCharArray()))
            .put('D', Chars.asList("AGT".toCharArray()))
            .build();

    private static final Set<Character> IUPAC_ALL_AMBIGUOUS = IUPAC_AMBIGUOUS_LOOKUP.keySet();


    public static Map<String,String> castSequence(Map<String, ?> sequence) {
        Map<String, String> plainSequence = Maps.newLinkedHashMap();

        for (Map.Entry<String, ?> e : sequence.entrySet()) {
            plainSequence.put(e.getKey(), ((AbstractSequence<?>) e.getValue()).getSequenceAsString());
        }

        return plainSequence;
    }

    public static void writeList(Collection<String> entries, File file) {
        try {
            BufferedWriter writer = Files.newWriter(file, Charsets.US_ASCII);
            for (String s : entries) {
                writer.write(s);
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }

    public static void writePhylipAlignment(Map<String, String> alignment, File file, String header) {
        try {
            BufferedWriter writer = Files.newWriter(file, Charsets.US_ASCII);
            writer.write(alignment.size() + " " + alignment.values().iterator().next().length());

            if (header != null) {
                writer.write(" " + header);
            }

            writer.newLine();

            int sequenceCount = 1;
            for (Map.Entry<String, String> e : alignment.entrySet()) {
                writer.write(String.format("seq_%s      %s", sequenceCount++, e.getValue()));
                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
