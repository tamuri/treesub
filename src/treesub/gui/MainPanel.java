package treesub.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import com.jgoodies.forms.layout.*;
/*
 * Created by JFormDesigner on Tue Mar 27 11:17:32 BST 2012
 */



/**
 * @author qwerty
 */
public class MainPanel extends JPanel {
	public MainPanel() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		label1 = new JLabel();
		alignmentPathTextField = new JTextField();
		alignmentPathButton = new JButton();
		panel1 = new JPanel();
		dnaRadioButton = new JRadioButton();
		codonRadioButton = new JRadioButton();
		aminoAcidRadioButton = new JRadioButton();
		label2 = new JLabel();
		panel3 = new JPanel();
		treeFileRadioButton = new JRadioButton();
		treePathTextField = new JTextField();
		treePathButton = new JButton();
		treeRaxmlRadioButton = new JRadioButton();
		panel6 = new JPanel();
		label5 = new JLabel();
		raxmlPathTextField = new JTextField();
		label6 = new JLabel();
		raxmlOptionsTextField = new JTextField();
		raxmlPathButton = new JButton();
		label7 = new JLabel();
		panel5 = new JPanel();
		ancestralFileRadioButton = new JRadioButton();
		ancestralPathTextField = new JTextField();
		ancestralPathButton = new JButton();
		ancestralPamlRadioButton = new JRadioButton();
		panel8 = new JPanel();
		label8 = new JLabel();
		pamlPathTextField = new JTextField();
		button1 = new JButton();
		pamlPathButton = new JButton();
		separator4 = new JSeparator();
		runButton = new JButton();
		panel2 = new JPanel();
		scrollPane1 = new JScrollPane();
		outputTextPane = new JTextPane();
		alignmentTypeButtonGroup = new ButtonGroup();
		treeButtonGroup = new ButtonGroup();
		ancestralButtonGroup = new ButtonGroup();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setBorder(new EmptyBorder(15, 15, 15, 15));
		setLayout(new FormLayout(
			"258dlu:grow, $lcgap, default",
			"2*(default, $lgap), default, $pgap, 3*(default, $lgap), default, $pgap, 3*(default, $lgap), default, $pgap, default, $lgap, default, $pgap, fill:default:grow"));

		//---- label1 ----
		label1.setText("1. ALIGNMENT (FASTA format)");
		label1.setFont(new Font("Dialog", Font.BOLD, 16));
		add(label1, cc.xy(1, 1));
		add(alignmentPathTextField, cc.xy(1, 3));

		//---- alignmentPathButton ----
		alignmentPathButton.setText("Browse...");
		add(alignmentPathButton, cc.xy(3, 3));

		//======== panel1 ========
		{
			panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

			//---- dnaRadioButton ----
			dnaRadioButton.setText("DNA");
			dnaRadioButton.setSelected(true);
			panel1.add(dnaRadioButton);

			//---- codonRadioButton ----
			codonRadioButton.setText("CODONS");
			panel1.add(codonRadioButton);

			//---- aminoAcidRadioButton ----
			aminoAcidRadioButton.setText("AMINO ACIDS");
			aminoAcidRadioButton.setActionCommand("AMINOACIDS");
			panel1.add(aminoAcidRadioButton);
		}
		add(panel1, cc.xywh(1, 5, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

		//---- label2 ----
		label2.setText("2. TREE (NEWICK format)");
		label2.setFont(new Font("Dialog", Font.BOLD, 16));
		add(label2, cc.xy(1, 7));

		//======== panel3 ========
		{
			panel3.setLayout(new BorderLayout());

			//---- treeFileRadioButton ----
			treeFileRadioButton.setText("File: ");
			treeFileRadioButton.setActionCommand("treeFromFile");
			panel3.add(treeFileRadioButton, BorderLayout.WEST);
			panel3.add(treePathTextField, BorderLayout.CENTER);
		}
		add(panel3, cc.xy(1, 9));

		//---- treePathButton ----
		treePathButton.setText("Browse...");
		add(treePathButton, cc.xy(3, 9));

		//---- treeRaxmlRadioButton ----
		treeRaxmlRadioButton.setText("Estimate using RAxML:");
		treeRaxmlRadioButton.setActionCommand("treeFromRaxml");
		treeRaxmlRadioButton.setSelected(true);
		add(treeRaxmlRadioButton, cc.xy(1, 11));

		//======== panel6 ========
		{
			panel6.setLayout(new FormLayout(
				"default, $lcgap, [50dlu,pref]:grow",
				"default, $lgap, default"));

			//---- label5 ----
			label5.setText("RAxML path:");
			panel6.add(label5, cc.xy(1, 1));
			panel6.add(raxmlPathTextField, cc.xywh(3, 1, 1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

			//---- label6 ----
			label6.setText("RAxML options:");
			panel6.add(label6, cc.xy(1, 3));
			panel6.add(raxmlOptionsTextField, cc.xywh(3, 3, 1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
		}
		add(panel6, cc.xy(1, 13));

		//---- raxmlPathButton ----
		raxmlPathButton.setText("Browse...");
		add(raxmlPathButton, cc.xywh(3, 13, 1, 1, CellConstraints.DEFAULT, CellConstraints.TOP));

		//---- label7 ----
		label7.setText("3. ANCESTRAL RECONSTRUCTION");
		label7.setFont(new Font("Dialog", Font.BOLD, 16));
		add(label7, cc.xywh(1, 15, 3, 1));

		//======== panel5 ========
		{
			panel5.setLayout(new BorderLayout());

			//---- ancestralFileRadioButton ----
			ancestralFileRadioButton.setText("File: ");
			ancestralFileRadioButton.setActionCommand("ancestorsFromFile");
			ancestralFileRadioButton.setEnabled(false);
			panel5.add(ancestralFileRadioButton, BorderLayout.WEST);
			panel5.add(ancestralPathTextField, BorderLayout.CENTER);
		}
		add(panel5, cc.xy(1, 17));

		//---- ancestralPathButton ----
		ancestralPathButton.setText("Browse...");
		add(ancestralPathButton, cc.xy(3, 17));

		//---- ancestralPamlRadioButton ----
		ancestralPamlRadioButton.setText("Estimate using PAML");
		ancestralPamlRadioButton.setActionCommand("ancestorsFromPaml");
		ancestralPamlRadioButton.setSelected(true);
		add(ancestralPamlRadioButton, cc.xy(1, 19));

		//======== panel8 ========
		{
			panel8.setLayout(new FormLayout(
				"default, $lcgap, default:grow",
				"default, $lgap, default"));

			//---- label8 ----
			label8.setText("PAML 'bin' directory:");
			panel8.add(label8, cc.xy(1, 1));
			panel8.add(pamlPathTextField, cc.xy(3, 1));

			//---- button1 ----
			button1.setText("Edit PAML control file");
			panel8.add(button1, cc.xywh(3, 3, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
		}
		add(panel8, cc.xy(1, 21));

		//---- pamlPathButton ----
		pamlPathButton.setText("Browse...");
		add(pamlPathButton, cc.xywh(3, 21, 1, 1, CellConstraints.DEFAULT, CellConstraints.TOP));
		add(separator4, cc.xywh(1, 23, 3, 1));

		//---- runButton ----
		runButton.setText("RUN");
		add(runButton, cc.xywh(1, 25, 3, 1));

		//======== panel2 ========
		{
			panel2.setLayout(new BorderLayout());

			//======== scrollPane1 ========
			{
				scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

				//---- outputTextPane ----
				outputTextPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
				scrollPane1.setViewportView(outputTextPane);
			}
			panel2.add(scrollPane1, BorderLayout.CENTER);
		}
		add(panel2, cc.xywh(1, 27, 3, 1));

		//---- alignmentTypeButtonGroup ----
		alignmentTypeButtonGroup = new ButtonGroup();
		alignmentTypeButtonGroup.add(dnaRadioButton);
		alignmentTypeButtonGroup.add(codonRadioButton);
		alignmentTypeButtonGroup.add(aminoAcidRadioButton);

		//---- treeButtonGroup ----
		treeButtonGroup = new ButtonGroup();
		treeButtonGroup.add(treeFileRadioButton);
		treeButtonGroup.add(treeRaxmlRadioButton);

		//---- ancestralButtonGroup ----
		ancestralButtonGroup = new ButtonGroup();
		ancestralButtonGroup.add(ancestralFileRadioButton);
		ancestralButtonGroup.add(ancestralPamlRadioButton);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel label1;
	JTextField alignmentPathTextField;
	JButton alignmentPathButton;
	private JPanel panel1;
	JRadioButton dnaRadioButton;
	JRadioButton codonRadioButton;
	JRadioButton aminoAcidRadioButton;
	private JLabel label2;
	private JPanel panel3;
	JRadioButton treeFileRadioButton;
	JTextField treePathTextField;
	JButton treePathButton;
	JRadioButton treeRaxmlRadioButton;
	private JPanel panel6;
	private JLabel label5;
	JTextField raxmlPathTextField;
	private JLabel label6;
	JTextField raxmlOptionsTextField;
	JButton raxmlPathButton;
	private JLabel label7;
	private JPanel panel5;
	JRadioButton ancestralFileRadioButton;
	JTextField ancestralPathTextField;
	JButton ancestralPathButton;
	JRadioButton ancestralPamlRadioButton;
	private JPanel panel8;
	private JLabel label8;
	JTextField pamlPathTextField;
	private JButton button1;
	JButton pamlPathButton;
	private JSeparator separator4;
	JButton runButton;
	private JPanel panel2;
	private JScrollPane scrollPane1;
	JTextPane outputTextPane;
	ButtonGroup alignmentTypeButtonGroup;
	ButtonGroup treeButtonGroup;
	ButtonGroup ancestralButtonGroup;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
