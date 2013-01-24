package treesub;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: atamuri
 * Date: 30/03/2012
 * Time: 14:17
 * To change this template use File | Settings | File Templates.
 */
public class JLabelLink extends JFrame {

    private static final String LABEL_TEXT = "For further information visit:";
    private static final String A_VALID_LINK = "http://stackoverflow.com";
    private static final String A_HREF = "<a href=\"";
    private static final String HREF_CLOSED = "\">";
    private static final String HREF_END = "</a>";
    private static final String HTML = "<html>";
    private static final String HTML_END = "</html>";

    public JLabelLink() {
        setTitle("HTML link via a JLabel");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label = new JLabel(LABEL_TEXT);
        contentPane.add(label);

        label = new JLabel(A_VALID_LINK);
        contentPane.add(label);
        if (isBrowsingSupported()) {
            makeLinkable(label, new LinkMouseListener());
        }

        pack();
    }

    private static void makeLinkable(JLabel c, MouseListener ml) {
        assert ml != null;
        c.setText(htmlIfy(linkIfy(c.getText())));
        c.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        c.addMouseListener(ml);
    }

    private static boolean isBrowsingSupported() {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        boolean result = false;
        Desktop desktop = java.awt.Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            result = true;
        }
        return result;

    }

    private static class LinkMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            JLabel l = (JLabel) evt.getSource();
            try {
                Desktop desktop = java.awt.Desktop.getDesktop();
                URI uri = new java.net.URI(getPlainLink(l.getText()));
                desktop.browse(uri);
            } catch (URISyntaxException use) {
                throw new AssertionError(use);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(null, "Sorry, a problem occurred while trying to open this link in your system's standard browser.", "A problem occured", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String getPlainLink(String s) {
        return s.substring(s.indexOf(A_HREF) + A_HREF.length(), s.indexOf(HREF_CLOSED));
    }

    //WARNING
//This method requires that s is a plain string that requires
//no further escaping
    private static String linkIfy(String s) {
        return A_HREF.concat(s).concat(HREF_CLOSED).concat(s).concat(HREF_END);
    }

    //WARNING
//This method requires that s is a plain string that requires
//no further escaping
    private static String htmlIfy(String s) {
        return HTML.concat(s).concat(HTML_END);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                new JLabelLink().setVisible(true);
            }
        });
    }
}

