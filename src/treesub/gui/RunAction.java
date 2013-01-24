package treesub.gui;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import org.apache.commons.io.FileUtils;
import org.biojava3.core.sequence.DNASequence;
import org.biojava3.core.sequence.ProteinSequence;
import pal.tree.Tree;
import pal.tree.TreeTool;
import pal.tree.TreeUtils;
import treesub.Constants;
import treesub.Utils;
import treesub.alignment.FASTAReader;
import treesub.ancestral.ParsePAML;
import treesub.ancestral.ParsePamlRst;
import treesub.ancestral.TreeSubBuilder;
import treesub.tree.TreeRerooter;

import javax.print.attribute.standard.PrinterName;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RunAction implements ActionListener {

    MainPanel mainPanel;

    public RunAction(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    public void actionPerformed(ActionEvent e) {
        final RunWorker msw = new RunWorker();

        mainPanel.setEnabled(false);

        try {
            mainPanel.outputTextPane.getDocument().remove(0, mainPanel.outputTextPane.getDocument().getLength());
            msw.execute();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        mainPanel.setEnabled(true);
    }

    class RunWorker extends SwingWorker<String, String> {

        String workingDir;
        
        @Override
        protected String doInBackground() throws Exception {

            long start = System.currentTimeMillis();
            File source = new File(mainPanel.alignmentPathTextField.getText());
            this.workingDir = source.getParent();

            if (!source.exists()) {
                publish(String.format("\nERROR: please provide an alignment file. '%s' not found.\n\n", source.toString()));
                return null;
            }
            
            // 1/6 Copy original FASTA alignment to output_dir/alignment
            File target = new File(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment");
            publish("*** Copying original file.\n");
            publish(source.toString() + " -> " + target.toString() + "\n");
            final BufferedWriter tbw = Files.newWriter(target, Charsets.US_ASCII);
            // We copy line by line to get rid of any line ending problems.
            Files.readLines(source, Charsets.US_ASCII, new LineProcessor<Object>() {
                public boolean processLine(String s) throws IOException {
                    tbw.write(s);
                    tbw.newLine();
                    return true;
                }
                public Object getResult() { return null; }
            });
            tbw.close();
            publish("*** Done.\n\n");

            
            // 2/6 Reading FASTA DNA, codon or amino acid file. Read and check.
            publish("*** Loading copy of alignment file '" + target.toString() + "'\n");
            Map<String, String> plainSequence = null;

            if (mainPanel.aminoAcidRadioButton.isSelected()) {
                Map<String, ProteinSequence> sequence = new FASTAReader().readAminoAcids(target);
                plainSequence = Utils.castSequence(sequence);
            } else {
                Map<String, DNASequence> sequence = new FASTAReader().readDNA(target);
                // If codon alignment, check that sequence length is correct
                if (mainPanel.codonRadioButton.isSelected()) {
                    int codonSequenceLength = sequence.entrySet().iterator().next().getValue().getLength();
                    if (codonSequenceLength % 3 != 0) {
                        publish(String.format("ERROR: Codon sequence length (%s) is not multiple of 3.\n", codonSequenceLength));
                        return null;
                    }
                }
                plainSequence = Utils.castSequence(sequence);
            }
            publish("*** Done\n\n");

            assert plainSequence != null;
            
            // 3/6 Convert FASTA alignment to PHYLIP format for RAxML and PAML
            publish("*** Converting FASTA format file 'alignment' to PHYLIP file 'alignment.*.phylip'.\n");
            Utils.writeList(plainSequence.keySet(), new File(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment.names"));
            publish("Written list of sequence names.\n");
            Utils.writePhylipAlignment(plainSequence, new File(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment.raxml.phylip"), null);
            publish("Written PHYLIP alignment file for RAxML.\n");

            if (mainPanel.codonRadioButton.isSelected()) {
                Utils.writePhylipAlignment(plainSequence, new File(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment.paml.phylip"), "GC");
                publish("Written PHYLIP alignment file for PAML (with GC header for codon analysis using baseml).\n");
            } else {
                Utils.writePhylipAlignment(plainSequence, new File(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment.paml.phylip"), null);
                publish("Written PHYLIP alignment file for PAML (without GC header).\n");
            }
            publish("*** Done.\n\n");

            File outTree = new File(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".tree");
            // If user supplied tree
            if (mainPanel.treeFileRadioButton.isSelected()) {
                publish("*** Reading user-supplied tree.\n");
                // Try and load tree
                File treeFile = new File(mainPanel.treePathTextField.getText());
                if (!treeFile.exists()) {
                    publish("ERROR: Please provide tree file or select option to build tree with RAxML.\n");
                    return null;
                }

                Tree userTree = TreeTool.readTree(new FileReader(treeFile));

                // Check that all taxa in tree are in alignment and vice versa
                Set<String> treeTaxa = Sets.newHashSet();
                for (int i = 0; i < userTree.getExternalNodeCount(); i++) {
                    treeTaxa.add(userTree.getExternalNode(i).getIdentifier().getName());
                }

                // set difference of tree \ alignment
                Sets.SetView<String> difference = Sets.symmetricDifference(treeTaxa, plainSequence.keySet());

                if (difference.size() > 0) {
                    publish("ERROR: Taxa are in tree but not in alignment (or vice versa):\n");
                    publish(difference.toString());
                    return null;
                }

                // rename the taxa to match our alignment names - seq_1, seq_2 etc.
                List<String> order = Arrays.asList(plainSequence.keySet().toArray(new String[plainSequence.keySet().size()]));
                
                for (int i = 0; i < userTree.getExternalNodeCount(); i++) {
                    userTree.getExternalNode(i).getIdentifier().setName("seq_" + (order.indexOf(userTree.getExternalNode(i).getIdentifier().getName()) + 1));
                }

                PrintWriter pw = new PrintWriter(outTree);
                TreeUtils.printNH(userTree, pw);
                pw.close();
                publish("*** Saved user-supplied tree.\n");
            } else {
                // 3/6 Run RAxML to estimate tree topology
                publish("*** Running RAxML application to estimate tree topology:\n");
                // We want to capture the output as its generated (i.e. don't buffer stdout)
                // See http://stackoverflow.com/questions/1401002/trick-an-application-into-thinking-its-stdin-is-interactive-not-a-pipe
                // MacOS: "/usr/bin/script", "-q", "/dev/null",
                // Linux: "/usr/bin/script", "-c", "\"[executable string]\"", "/dev/null"
                List<String> raxmlCommand = Lists.newArrayList("/usr/bin/script", "-q", "/dev/null", // to capture the output in realtime
                        mainPanel.raxmlPathTextField.getText(), // path to raxml
                        "-s", "treesub.alignment.raxml.phylip", "-n", "RECON" // basic options that cannot be overridden
                );

                // add user-options
                for (String s: mainPanel.raxmlOptionsTextField.getText().split(" ")) raxmlCommand.add(s);

                Process raxmlProcess = getProcessBuilder(raxmlCommand.toArray(new String[raxmlCommand.size()])).start(); //
                publishInputStream(raxmlProcess.getInputStream());
                raxmlProcess.destroy();
                publish("\n");
                publish("*** Finished running RAxML.\n\n");
                
                // Save the RAxML tree to our filename
                FileUtils.copyFile(new File(this.workingDir + File.separator + "RAxML_bestTree.RECON"), outTree);
            }

            // 4/6 Reroot the tree using the first sequence in the alignment (this is our own convention)
            publish("*** Rooting tree by outgroup sequence.\n");
            publish("Rooting " + Constants.PROGRAM_FILE_PREFIX + ".tree" + " -> " + Constants.PROGRAM_FILE_PREFIX + ".rooted.tree \n");
            TreeRerooter tr = new TreeRerooter();
            tr.reroot(outTree.getAbsolutePath(), this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".rooted.tree");
            publish("*** Successfully rooted the tree.\n\n");

            // 5/6 Run PAML to estimate branch lengths and do the ancestral reconstruction
            // Copy standard codeml.ctl to the working directory
            // FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("codeml.ctl"), new File(this.workingDir + "/codeml.ctl"));
            publish("*** Running PAML application. Output from PAML:\n");
            FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/baseml.ctl"), new File(this.workingDir + File.separator + "baseml.ctl"));
            Process pamlProcess = getProcessBuilder("/usr/bin/script", "-q", "/dev/null", // to capture output in realtime
                    mainPanel.pamlPathTextField.getText() + File.separator + "baseml" // PAML executable path
            ).start();
            publishInputStream(pamlProcess.getInputStream());
            pamlProcess.destroy();
            publish("\n");
            publish("*** Successfully ran PAML.\n\n");

            publish("*** Parsing PAML results.\n");
            ParsePamlRst parser = new ParsePamlRst();
            parser.parse(this.workingDir + File.separator + "rst", this.workingDir);
            publish("*** Successfully parsed PAML results.\n\n");

            
            publish("*** Building final tree using treesub.alignment.ancestral, treesub.ancestral.tree and treesub.alignment.\n");
            TreeSubBuilder builder = new TreeSubBuilder();
            builder.setDataType("CODONS");
            builder.loadSequences(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment.raxml.phylip");
            builder.loadSequenceNames(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment.names");
            builder.loadAncestralStates(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".alignment.ancestral");
            builder.loadTree(this.workingDir + File.separator + Constants.PROGRAM_FILE_PREFIX + ".ancestral.tree");
            builder.build();
            publish("*** Done!\n\n");



            // FINISHED!

            long elapsed = System.currentTimeMillis() - start;
            publish(String.format("Total running time: %dm %ds.\n\n",
                    TimeUnit.MILLISECONDS.toMinutes(elapsed),
                    TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed))
            ));


            return null;
        }

        private ProcessBuilder getProcessBuilder(String... args) {
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.directory(new File(this.workingDir));
            builder.redirectErrorStream(true);
            System.out.printf("%s\n", builder.command().toString());
            return builder;
        }

        protected void done() {
            try {
                get();
            } catch (ExecutionException e) {
                e.getCause().printStackTrace();
                String msg = String.format("ERROR: %s\n",
                        e.getCause().toString());
                process(Lists.newArrayList(msg));
                /*JOptionPane.showMessageDialog(mainPanel,
                        msg, "Error", JOptionPane.ERROR_MESSAGE, errorIcon);*/
            } catch (InterruptedException e) {
                // Process e here
            }
        }


        private void publishInputStream(InputStream is) throws Exception {
            byte[] buffer = new byte[100];
            int len;
            while ((len = is.read(buffer, 0, buffer.length)) != -1) {
                publish(new String(buffer, 0, len));
            }
            is.close();
        }

        @Override
        protected void process(List<String> chunks) {
            // Done on the event thread
            for (String chunk : chunks) {
                StyledDocument d = mainPanel.outputTextPane.getStyledDocument();

                Style style;
                if (chunk.contains("ERROR")) {
                    style = d.addStyle("ERROR", null);
                    StyleConstants.setForeground(style, Color.RED);
                    StyleConstants.setBold(style, true);
                } else if (chunk.startsWith("***")) {
                    style = d.addStyle("PROGRESS", null);
                    StyleConstants.setForeground(style, Color.BLUE);
                    StyleConstants.setBold(style, true);
                } else {
                    style = null;
                }

                try {
                    d.insertString(d.getLength(), chunk, style);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
