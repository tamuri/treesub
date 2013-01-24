package treesub.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class MainFrame {
    
    private MainPanel mainPanel;

    private Properties properties = new Properties();
    private static String PROPERTIES_FILENAME = "treesub.properties";
    private static String RAXML_PATH_PROPERTY = "RAXML_PATH";
    private static String PAML_PATH_PROPERTY = "PAML_PATH";
    private static String RAXML_DEFAULT_OPTIONS = "RAXML_OPTIONS";

    public MainFrame() {

        // Try and read saved properties
        try {
            properties.load(new FileInputStream(PROPERTIES_FILENAME));
        } catch (IOException e) {
            e.printStackTrace();
            // Assume file not found
            properties.setProperty(RAXML_PATH_PROPERTY, "");
            properties.setProperty(PAML_PATH_PROPERTY, "");
            properties.setProperty(RAXML_DEFAULT_OPTIONS, "-#10");
        }

        mainPanel = new MainPanel();

        // Set the default values
        mainPanel.raxmlPathTextField.setText(properties.getProperty(RAXML_PATH_PROPERTY));
        mainPanel.raxmlOptionsTextField.setText(properties.getProperty(RAXML_DEFAULT_OPTIONS));
        mainPanel.pamlPathTextField.setText(properties.getProperty(PAML_PATH_PROPERTY));

        // Add ActionEvents for buttons to open file chooser dialog
        mainPanel.alignmentPathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getAndSetFile(mainPanel.alignmentPathTextField, null, false);
            }
        });
        mainPanel.treePathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getAndSetFile(mainPanel.treePathTextField, null, false);
                mainPanel.treeFileRadioButton.doClick(); // rather than setSelected(), so we fire action event
            }
        });
        mainPanel.raxmlPathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getAndSetFile(mainPanel.raxmlPathTextField, RAXML_PATH_PROPERTY, false);
            }
        });
        mainPanel.ancestralPathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getAndSetFile(mainPanel.ancestralPathTextField, null, false);
            }
        });
        mainPanel.pamlPathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getAndSetFile(mainPanel.pamlPathTextField, PAML_PATH_PROPERTY, true);
            }
        });

        // If someone has changed the RAxML options, save them as defaults TODO: should be saved later, after successful run
        mainPanel.raxmlOptionsTextField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) { }
            public void focusLost(FocusEvent e) {
                saveProperty(RAXML_DEFAULT_OPTIONS, mainPanel.raxmlOptionsTextField.getText());
            }
        });

        // If tree is estimated by RAxML, we only only ancestral states from PAML (and disabled ancestral states from user file)
        mainPanel.treeFileRadioButton.addActionListener(new TreeRadioButtonActionListener());
        mainPanel.treeRaxmlRadioButton.addActionListener(new TreeRadioButtonActionListener());

        mainPanel.runButton.addActionListener(new RunAction(mainPanel));

        // Draw the main window, adding the main panel.
        JFrame f = new JFrame("TreeSub");
        f.getContentPane().setLayout(new BorderLayout());
        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.pack();
        Dimension d = f.getContentPane().getSize();
        f.setMinimumSize(new Dimension((int)d.getWidth(), (int) d.getHeight() + 120));
        f.setVisible(true);
    }

    private class TreeRadioButtonActionListener implements ActionListener {
        // For enabling/disabling ancestral recon. options based of where the tree is coming from
        public void actionPerformed(ActionEvent e) {
            if (mainPanel.treeFileRadioButton.isSelected()) {
                mainPanel.ancestralFileRadioButton.setEnabled(true);
                mainPanel.ancestralPamlRadioButton.setEnabled(true);
            } else {
                mainPanel.ancestralFileRadioButton.setSelected(false);
                mainPanel.ancestralFileRadioButton.setEnabled(false);
                mainPanel.ancestralPamlRadioButton.setSelected(true);
                mainPanel.ancestralPamlRadioButton.setEnabled(true);
            }
        }
    }

    public static void main(String[] args) {
        new MainFrame();
    }

    private void getAndSetFile(final JTextField field, final String propertyName, boolean directoriesOnly) {
        final JFileChooser fc = new JFileChooser();

        if (directoriesOnly) fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

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
}

