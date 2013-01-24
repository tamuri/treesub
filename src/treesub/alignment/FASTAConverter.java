package treesub.alignment;

import org.biojava3.core.sequence.DNASequence;
import org.biojava3.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava3.core.sequence.compound.NucleotideCompound;
import org.biojava3.core.sequence.io.DNASequenceCreator;
import org.biojava3.core.sequence.io.FastaReader;
import org.biojava3.core.sequence.io.GenericFastaHeaderParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

/**
 * @author Asif Tamuri (atamuri@nimr.mrc.ac.uk)
 *
 * This class takes a name of a file as argument and creates two files:
 *     file.names - this is the ordered list of sequence names (i.e. the '>' label of the FASTA file
 *     file.phylip - this is a PHYLIP format alignment from the FASTA file, with the sequence names
 *                   renamed to seq_1, seq_2, seq_3...seq_n. This is to reduce the possibility of
 *                   label errors being thrown by RaXML and PAML.
 */
public class FASTAConverter {
    public String run(String dir, OUTPUT output) throws Exception {
        String filename = dir + "/alignment";

        FastaReader<DNASequence, NucleotideCompound> fastaReader =
                new FastaReader<DNASequence, NucleotideCompound>(new File(filename),
                        new GenericFastaHeaderParser<DNASequence, NucleotideCompound>(),
                        new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet()));

        // fastaReader.process returns LinkedHashMap, so order of sequences is preserved
        Map<String, DNASequence> fasta = fastaReader.process();

        // Write out the names of the sequences - we rename them to reroot sequence through RaXML and PAML
        BufferedWriter names_out = new BufferedWriter(new FileWriter(filename + ".names"));
        for (Map.Entry<String, DNASequence> e : fasta.entrySet()) names_out.write(String.format("%s\n", e.getKey()));
        names_out.close();

        // Output a PHYLIP file, which is the accepted format for both RAxML and PAML
        // However, the PAML file, for baseml requires a 'GC' in the header which makes RAxML error



        String outfilename;
        String header = fasta.size() + " " + fasta.values().iterator().next().getLength();
        if (output == OUTPUT.PAML) {
            outfilename = filename + ".paml.phylip";
            header = header + " GC";
        } else {
            outfilename = filename + ".raxml.phylip";
        }

        try {
        BufferedWriter phylip_out = new BufferedWriter(new FileWriter(outfilename));
        phylip_out.write(header + "\n");
        int sequenceCount = 1;

            for (Map.Entry<String, DNASequence> e : fasta.entrySet()) {
                phylip_out.write(String.format("seq_%s    %s\n", sequenceCount++, e.getValue().getSequenceAsString().toUpperCase()));
            }

            /*
            for (String name : fasta.keySet()) {
                phylip_out.write(String.format("seq_%s    %s\n", sequenceCount++, fasta.get(name).toString().toUpperCase()));
            }*/


        phylip_out.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return String.format("Alignment has %s sequences, each with %s sites.", fasta.size(), fasta.values().iterator().next().getLength());
    }

    public void convert(Map<String, String> sequence, String filename, String additionalHeader) throws Exception {

    }

    private void writeNames() {

    }


    public static enum OUTPUT {
        RAXML, PAML
    }

    public static void main(String[] args) throws Exception {
        FASTAConverter f = new FASTAConverter();
        f.run(".", OUTPUT.RAXML);
    }
}
