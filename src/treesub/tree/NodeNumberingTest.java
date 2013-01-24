package treesub.tree;

import pal.tree.Node;
import treesub.gui.MainPanel;

import javax.swing.*;
import java.awt.*;

public class NodeNumberingTest {
    static int number = 0;
    
    public static void main(String[] args) throws Exception {
        
        JFrame f = new JFrame();
        JPanel p = new MainPanel();
        f.getContentPane().setLayout(new BorderLayout());
        f.getContentPane().add(p, BorderLayout.CENTER);
        
        
        
        f.pack();

        Dimension d = f.getContentPane().getSize();
        f.setMinimumSize(new Dimension((int)d.getWidth(), (int) d.getHeight() + 20));
        
        f.setVisible(true);
        
        //System.exit(0);
        /*
        Tree t = TreeTool.readTree(new FileReader("/Users/atamuri/Documents/work/111108 Tree Annotator for Div. of Virology/etc/examples/H3HASO-251111/RAxML_bestTree.RECON.rooted"));

        for (int i = 0; i < t.getExternalNodeCount(); i++) {
            t.getExternalNode(i).getIdentifier().setName(t.getExternalNode(i).getIdentifier().getName() + "=" + Integer.toString(i + 1));
        }

        *//*for (int i = 0; i < t.getInternalNodeCount(); i++) {
            t.getInternalNode(i).getIdentifier().setName(t.getInternalNode(i).getNumber() + "=" + Integer.toString(i));
        }*//*

        number = t.getExternalNodeCount() + 1;
        
        traverse(t.getRoot());

        
        PrintWriter w = new PrintWriter("/Users/atamuri/Documents/work/111108 Tree Annotator for Div. of Virology/etc/examples/H3HASO-251111/test.tree");
        TreeUtils.printNH(t, w);
        w.close();*/
    }

    private static void traverse(Node n) {

        if (n.isRoot()) {
            n.getIdentifier().setName(Integer.toString(number));
            number++;
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            if (!n.getChild(i).isLeaf()) {
                n.getChild(i).getIdentifier().setName(n.getChild(i).getIdentifier().getName() + "=" + Integer.toString(number));
                number++;
                traverse(n.getChild(i));
            }
        }
    }
}
