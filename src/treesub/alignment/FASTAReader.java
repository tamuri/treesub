package treesub.alignment;

import org.biojava3.core.sequence.DNASequence;
import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava3.core.sequence.compound.NucleotideCompound;
import org.biojava3.core.sequence.io.FastaReader;
import org.biojava3.core.sequence.io.FastaReaderHelper;
import org.biojava3.core.sequence.io.FileProxyDNASequenceCreator;
import org.biojava3.core.sequence.io.GenericFastaHeaderParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class FASTAReader {
    public FASTAReader() {
    }

    public Map<String, DNASequence> readDNA(File filename) {

        FastaReader<DNASequence, NucleotideCompound> fastaReader;
        Map<String, DNASequence> fasta = null;

        try {
            fastaReader =
                    new FastaReader<DNASequence, NucleotideCompound>(filename,
                            new GenericFastaHeaderParser<DNASequence, NucleotideCompound>(),
                            new FileProxyDNASequenceCreator(filename, AmbiguityDNACompoundSet.getDNACompoundSet()));

            fasta = fastaReader.process();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fasta;
    }

    public Map<String, ProteinSequence> readAminoAcids(File filename) {
        try {
            return FastaReaderHelper.readFastaProteinSequence(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
