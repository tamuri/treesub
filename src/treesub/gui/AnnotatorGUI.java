package treesub.gui;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import org.apache.commons.io.FileUtils;
import treesub.ancestral.ParsePAML;
import treesub.alignment.FASTAConverter;
import treesub.tree.TreeRerooter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Asif Tamuri (atamuri@nimr.mrc.ac.uk)
 */
public class AnnotatorGUI {
    private JPanel mainPanel;
    private JTextField raxmlPath;
    private JTextField pamlPath;
    private JButton pamlPathButton;
    private JButton raxmlPathButton;
    private JTextField alignmentPath;
    private JButton alignmentPathButton;
    private JButton runButton;
    private JTextArea mainTextArea;
    private JScrollPane scrollPane1;
    private JPanel Centre;
    private JPanel North;
    private JPanel actionPanel;
    private JButton reannotateButton;
    private static JFrame frame;

    private static String PROPERTIES_FILENAME = "annotator.properties";
    private static String RAXML_PATH_PROPERTY = "RAXML_PATH";
    private static String PAML_PATH_PROPERTY = "PAML_PATH";
    private static String RAXML_OPTIONS_PROPERTY = "RAXML_OPTIONS";
    private static String DEFAULT_PATH_PROPERTY = "DEFAULT_PATH";
    private static String RAXML_DEFAULT_OPTIONS = "-m GTRGAMMA -T 2 -# 10";

    private Properties properties = new Properties();
    private static List<String> FILES_TO_CHECK = Lists.newArrayList("alignment", "alignment.names", "alignment.raxml.phylip",
            "alignment.paml.phylip", "RAxML_bestTree.RECON", "RAxML_bestTree.RECON.rooted", "pamlout", "rst");

    public AnnotatorGUI() {
        try {
            properties.load(new FileInputStream(PROPERTIES_FILENAME));
        } catch (IOException e) {
            // Assume file not found
            properties.setProperty(RAXML_PATH_PROPERTY, "");
            properties.setProperty(PAML_PATH_PROPERTY, "");
            properties.setProperty(DEFAULT_PATH_PROPERTY, ".");
            properties.setProperty(RAXML_OPTIONS_PROPERTY, RAXML_DEFAULT_OPTIONS);
        }

        // These three buttons are simple file select dialogs
        raxmlPathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getAndSetFile(raxmlPath, RAXML_PATH_PROPERTY, null);
            }
        });
        pamlPathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getAndSetFile(pamlPath, PAML_PATH_PROPERTY, null);
            }
        });
        alignmentPathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getAndSetFile(alignmentPath, DEFAULT_PATH_PROPERTY, properties.getProperty(DEFAULT_PATH_PROPERTY));
                if (alignmentPath.getText().length() > 0) {
                    runButton.setEnabled(true);
                    reannotateButton.setEnabled(true);
                }
            }
        });

        runButton.addActionListener(new RunAction());

        reannotateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AnnotateWorker aw = new AnnotateWorker();
                aw.execute();
            }

            class AnnotateWorker extends SwingWorker<String, String> {
                @Override
                protected String doInBackground() throws Exception {
                    File source = new File(alignmentPath.getText());
                    String workingDir = source.getParent();

                    publish("Re-annotate\n-----------\n");

                    // Check the the analysis has been reroot
                    boolean ranAnalysis = true;
                    for (String f : FILES_TO_CHECK) {
                        if (!new File(workingDir + "/" + f).exists()) {
                            publish("File '" + f + "' not found in '" + workingDir + "'.\n");
                            ranAnalysis = false;
                        }
                    }

                    if (ranAnalysis) {
                        publish("Parsing PAML results and building tree for substitutions.\n");
                        ParsePAML pp = new ParsePAML();
                        pp.run(workingDir);
                        publish("Succesfully parsed PAML results.\n\n");
                    } else {
                        publish("\nCannot reannotate tree. Run the analysis first!\n\n");
                    }


                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String s : chunks) {
                        mainTextArea.append(s);
                    }
                }
            }
        });

        mainTextArea.setWrapStyleWord(true);
        mainTextArea.setAutoscrolls(true);
        mainTextArea.setLineWrap(true);

        // Disable the run and re-annotate buttons when we first start up
        setActionsEnabled(false);

        // If people edit the alignment path by hand, enable the run and re-annotate buttons
        alignmentPath.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (alignmentPath.getText().length() > 0) setActionsEnabled(true);
            }

            public void removeUpdate(DocumentEvent e) {
                if (alignmentPath.getText().length() == 0) setActionsEnabled(false);
            }

            public void changedUpdate(DocumentEvent e) {
            } // Not fired by plain text components
        });

        DefaultCaret caret = (DefaultCaret) mainTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        raxmlPath.setText(properties.getProperty(RAXML_PATH_PROPERTY));
        pamlPath.setText(properties.getProperty(PAML_PATH_PROPERTY));
    }

    public static void main(String[] args) {
        frame = new JFrame("AnnotatorGUI");
        AnnotatorGUI ag = new AnnotatorGUI();
        frame.setContentPane(ag.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void setActionsEnabled(boolean enabled) {
        runButton.setEnabled(enabled);
        reannotateButton.setEnabled(enabled);
    }

    private void recurseSetEnabled(JComponent component, boolean enabled) {
        for (Component c : component.getComponents()) {
            c.setEnabled(enabled);
            if (((JComponent) c).getComponents().length > 0) {
                recurseSetEnabled((JComponent) c, enabled);
            }
        }
    }

    private void getAndSetFile(final JTextField field, final String propertyName, final String path) {
        final JFileChooser fc = new JFileChooser(path);
        int returnVal = fc.showOpenDialog(mainPanel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            field.setText(fc.getSelectedFile().toString());
            if (propertyName != null) {
                saveProperty(propertyName, fc.getSelectedFile().toString());
            }
        }
    }

    private void saveProperty(String property, String s) {
        properties.setProperty(property, s);
        try {
            properties.store(new FileOutputStream(PROPERTIES_FILENAME), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setAutoscrolls(false);
        mainPanel.setMinimumSize(new Dimension(500, 250));
        mainPanel.setPreferredSize(new Dimension(700, 700));
        Centre = new JPanel();
        Centre.setLayout(new BorderLayout(0, 0));
        mainPanel.add(Centre, BorderLayout.CENTER);
        scrollPane1 = new JScrollPane();
        Centre.add(scrollPane1, BorderLayout.CENTER);
        mainTextArea = new JTextArea();
        mainTextArea.setEditable(false);
        mainTextArea.setFont(new Font("Monospaced", mainTextArea.getFont().getStyle(), 14));
        mainTextArea.setLineWrap(true);
        mainTextArea.setRows(30);
        mainTextArea.setWrapStyleWord(true);
        scrollPane1.setViewportView(mainTextArea);
        North = new JPanel();
        North.setLayout(new GridBagLayout());
        mainPanel.add(North, BorderLayout.NORTH);
        raxmlPath = new JTextField();
        raxmlPath.setColumns(25);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        North.add(raxmlPath, gbc);
        pamlPath = new JTextField();
        pamlPath.setColumns(25);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        North.add(pamlPath, gbc);
        alignmentPath = new JTextField();
        alignmentPath.setColumns(25);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        North.add(alignmentPath, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("RAxML (raxmlHPC) path:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        North.add(label1, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("PAML (baseml) path:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        North.add(label2, gbc);
        final JLabel label3 = new JLabel();
        label3.setText("FASTA alignment:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        North.add(label3, gbc);
        raxmlPathButton = new JButton();
        raxmlPathButton.setActionCommand("");
        raxmlPathButton.setLabel("Browse...");
        raxmlPathButton.setText("Browse...");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        North.add(raxmlPathButton, gbc);
        pamlPathButton = new JButton();
        pamlPathButton.setActionCommand("getPamlPath");
        pamlPathButton.setText("Browse...");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        North.add(pamlPathButton, gbc);
        alignmentPathButton = new JButton();
        alignmentPathButton.setActionCommand("getAlignmentPath");
        alignmentPathButton.setText("Browse...");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        North.add(alignmentPathButton, gbc);
        actionPanel = new JPanel();
        actionPanel.setLayout(new BorderLayout(0, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        North.add(actionPanel, gbc);
        runButton = new JButton();
        runButton.setActionCommand("doRun");
        runButton.setSelected(false);
        runButton.setText("RUN");
        runButton.putClientProperty("html.disable", Boolean.FALSE);
        actionPanel.add(runButton, BorderLayout.CENTER);
        reannotateButton = new JButton();
        reannotateButton.setEnabled(true);
        reannotateButton.setForeground(new Color(-16777216));
        reannotateButton.setHideActionText(false);
        reannotateButton.setText("Re-annotate");
        actionPanel.add(reannotateButton, BorderLayout.EAST);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    class RunAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            recurseSetEnabled(North, false);

            File source = new File(alignmentPath.getText());
            String workingDir = source.getParent();

            // STEP 0: This directory should be empty - we don't want to overwrite files.
            boolean foundRunFiles = false;
            for (String f : FILES_TO_CHECK) {
                if (new File(workingDir + "/" + f).exists()) {
                    mainTextArea.append(String.format("'%s' already exists in directory '%s'.\n", f, workingDir));
                    foundRunFiles = true;
                }
            }

            if (foundRunFiles) {
                mainTextArea.append("\nCannot overwrite files. Use an empty directory!\n\n");
                recurseSetEnabled(North, true);
            } else {
                runButton.setText("Running...");
                final RunWorker msw = new RunWorker();
                msw.execute();
            }
        }

        class RunWorker extends SwingWorker<String, String> {
            String workingDir;

            @Override
            protected String doInBackground() throws Exception {
                // 6 STEPS

                long start = System.currentTimeMillis();
                File source = new File(alignmentPath.getText());
                this.workingDir = source.getParent();

                // STEP 1: Copy original FASTA alignment file to 'alignment' *******************************************
                File target = new File(this.workingDir + "/alignment");
                publish(String.format("[1/6] Copying file from '%s' to '%s'.\n", source.toString(), target.toString()));
                // FileUtils.copyFile(source, target); - Can't use this because copies line-endings. We want native EOL.
                final BufferedWriter tbw = Files.newWriter(target, Charsets.US_ASCII);
                Files.readLines(source, Charsets.US_ASCII, new LineProcessor<Object>() {
                    public boolean processLine(String s) throws IOException {
                        tbw.write(s);
                        tbw.newLine();
                        return true;
                    }

                    public Object getResult() {
                        return null;
                    }
                });
                tbw.close();
                publish("Successfully copied file.\n\n");


                // STEP 2: Copy FASTA alignment to PHYLIP alignment ****************************************************
                publish("[2/6] Converting FASTA file 'alignment' to PHYLIP file 'alignment.phylip'.\n");
                String fcRaxOut = new FASTAConverter().run(this.workingDir, FASTAConverter.OUTPUT.RAXML); // RAxML, without 'GC' in header
                new FASTAConverter().run(this.workingDir, FASTAConverter.OUTPUT.PAML); // PAML, with 'GC' in header
                publish(fcRaxOut + "\n"); // gives some information about the alignment
                publish("Successfully converted FASTA file.\n\n");


                // STEP 3: Run RAxML to estimate tree topology *********************************************************
                publish("[3/6] Running RAxML application. Output from RAxML:\n");

                Process raxmlProcess;
                List<String> raxmlOptions = Lists.newArrayList(Splitter.on(" ").split(properties.getProperty(RAXML_OPTIONS_PROPERTY)));
                raxmlOptions.addAll(Lists.newArrayList("-s", "alignment.raxml.phylip", "-n", "RECON"));
                raxmlOptions.add(0, raxmlPath.getText());

                if (System.getProperty("os.name").startsWith("Windows")) {

                    raxmlProcess = getProcessBuilder(
                            // "c:\\cygwin\\bin\\bash.exe", "-li", "/cygdrive/c/cygwin/bin/unbuffer",
                            // "cmd.exe", "/c", "start", "cmd.exe", "/k", "\"",
                            raxmlPath.getText(), // RAxML executable path
                            "-s", "alignment.raxml.phylip", "-n", "RECON", properties.getProperty(RAXML_OPTIONS_PROPERTY) // RAxML options
                            , "\""
                    ).start();
                    publish("(You're running on Windows. No real-time output available!)\n");

                } else if (System.getProperty("os.name").equals("Mac OS X")) {

                    raxmlOptions.addAll(0, Lists.newArrayList("/usr/bin/script", "-q", "/dev/null"));
                    raxmlProcess = getProcessBuilder(raxmlOptions.toArray(new String[raxmlOptions.size()])).start();

                } else {

                    // Assume linux!
                    raxmlOptions.add(0, "/usr/bin/unbuffer");
                    raxmlProcess = getProcessBuilder(raxmlOptions.toArray(new String[raxmlOptions.size()])).start();

                }

                publishInputStream(raxmlProcess.getInputStream());
                raxmlProcess.destroy();

                publish("\nSuccessfully ran RAxML.\n\n");




                // STEP 4: Reroot the tree from RAxML ******************************************************************
                publish("[4/6] Rooting tree by outgroup sequence.\n");
                TreeRerooter tr = new TreeRerooter();
                tr.reroot(this.workingDir + "/RAxML_bestTree.RECON", this.workingDir + "/RAxML_bestTree.RECON.rooted");
                publish("Successfully rooted the tree.\n\n");


                // STEP 5: Run PAML to estimate branch lengths and do the ancestral reconstruction *********************
                // Copy standard codeml.ctl to the working directory
                // FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("codeml.ctl"), new File(this.workingDir + "/codeml.ctl"));
                FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/baseml.annotatorgui.ctl"), new File(this.workingDir + "/baseml.ctl"));
                publish("[5/6] Running PAML application. Output from PAML:\n");
                /*Process pamlProcess = getProcessBuilder("/usr/bin/script", "-q", "/dev/null",
                            pamlPath.getText() // PAML executable path
                            ).start();*/

                Process pamlProcess;

                if (System.getProperty("os.name").startsWith("Windows")) {
                    pamlProcess = getProcessBuilder(pamlPath.getText()).start();
                    publish("(You're running on Windows. No real-time output available!)\n");
                } else if (System.getProperty("os.name").equals("Mac OS X")) {
                    pamlProcess = getProcessBuilder("/usr/bin/script", "-q", "/dev/null", pamlPath.getText()).start();
                } else {
                    pamlProcess = getProcessBuilder("/usr/bin/unbuffer", pamlPath.getText()).start();
                }

                publishInputStream(pamlProcess.getInputStream());
                pamlProcess.destroy();
                publish("\nSuccessfully ran PAML.\n\n");


                // STEP 6: Parse PAML results for tree and ancestral states and write annotated tree *******************
                publish("[6/6] Parsing PAML results and building tree for substitutions.\n");
                ParsePAML pp = new ParsePAML();
                pp.run(this.workingDir);
                publish("Succesfully parsed PAML results.\n\n");


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
                return builder;
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
                    mainTextArea.append(chunk);
                }
            }

            @Override
            protected void done() {
                try {
                    super.get();
                    // save the results
                    BufferedWriter bw = Files.newWriter(new File(this.workingDir + "/annotator.log"), Charsets.US_ASCII);
                    bw.write(mainTextArea.getText());
                    bw.close();

                    mainTextArea.append("FINISHED!\n\n");

                    mainTextArea.append("Wrote tree file 'substitutions.tree'\n");
                    mainTextArea.append("Wrote substitutions list file 'substitutions.tsv'\n");
                    mainTextArea.append("Wrote this output to 'annotator.log'\n");

                    runButton.setText("RUN");
                    recurseSetEnabled(North, true);

                    JOptionPane.showMessageDialog(frame, "Analysis complete. See 'substitutions.tree' and 'substitutions.tsv'");
                } catch (Exception e) {
                    // NOTE: do something with the exception
                    e.getCause().printStackTrace();
                    String msg = String.format("ERROR: %s\n",
                            e.getCause().toString());
                    process(Lists.newArrayList(msg, e.getCause().getMessage()));
                }

            }
        }
    }
}
