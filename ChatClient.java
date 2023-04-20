import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JEditorPane;

//Adicionar o Highlighter
import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * A simple Swing-based client for the chat server. Graphically it is a frame
 * with a text field for entering messages and a textarea to see the whole
 * dialog.
 *
 * The client follows the following Chat Protocol. When the server sends
 * "SUBMITNAME" the client replies with the desired screen name. The server will
 * keep sending "SUBMITNAME" requests as long as the client submits screen names
 * that are already in use. When the server sends a line beginning with
 * "NAMEACCEPTED" the client is now allowed to start sending the server
 * arbitrary strings to be broadcast to all chatters connected to the server.
 * When the server sends a line beginning with "MESSAGE" then all characters
 * following this string should be displayed in its message area.
 */
public class ChatClient {

    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextPane textPane = new JTextPane();
    StyledDocument document = textPane.getStyledDocument();
    SimpleAttributeSet systemStandardMessageAttributeSet = new SimpleAttributeSet(); 
    SimpleAttributeSet systemJoinedMessageAttributeSet = new SimpleAttributeSet(); 
    SimpleAttributeSet systemLeftMessageAttributeSet = new SimpleAttributeSet(); 
    DefaultHighlighter.DefaultHighlightPainter Highlighter = new DefaultHighlighter.DefaultHighlightPainter(Color.red);

    /**
     * Constructs the client by laying out the GUI and registering a listener with
     * the textfield so that pressing Return in the listener sends the textfield
     * contents to the server. Note however that the textfield is initially NOT
     * editable, and only becomes editable AFTER the client receives the
     * NAMEACCEPTED message from the server.
     */
    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        textField.setEditable(false);
        textPane.setEditable(false);
        textPane.setPreferredSize(new Dimension(400, 400));

        StyleConstants.setBold(systemStandardMessageAttributeSet, true);
        StyleConstants.setForeground(systemStandardMessageAttributeSet, new Color(74, 74, 74, 255));
        StyleConstants.setBold(systemJoinedMessageAttributeSet, true);
        StyleConstants.setForeground(systemJoinedMessageAttributeSet, new Color(140, 255, 74, 255));
        StyleConstants.setBold(systemLeftMessageAttributeSet, true);
        StyleConstants.setForeground(systemLeftMessageAttributeSet, new Color(255, 74, 83, 255));

        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(textPane), BorderLayout.CENTER);
        frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void run() throws IOException {
        try {
            var socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                var line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) 
                {
                    out.println(getName());
                } 
                else if (line.startsWith("NAMEACCEPTED")) 
                {
                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                } 
                else if (line.startsWith("MESSAGE")) 
                {
                    try {
                        document.insertString(document.getLength(), line.substring(8) + "\n", null);
                    } catch(BadLocationException exception) {
                        System.out.println(exception);
                    }
                } 
                else if (line.startsWith("SYSMESSAGE")) 
                {                    
                    int sizeChat = document.getLength();
                    try {
                        document.insertString(sizeChat, line.substring(12) + "\n", null);
                        switch(line.charAt(10))
                        {
                            case 'J':
                                document.setCharacterAttributes(sizeChat, document.getLength() - sizeChat, systemJoinedMessageAttributeSet, false);
                                break;
                            case 'L':
                                document.setCharacterAttributes(sizeChat, document.getLength() - sizeChat, systemLeftMessageAttributeSet, false);
                                break;
                            default:
                                document.setCharacterAttributes(sizeChat, document.getLength() - sizeChat, systemStandardMessageAttributeSet, false);
                                break;
                        }
                    } catch(BadLocationException exception) {
                        System.out.println(exception);
                    }
                }
            }
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        var client = new ChatClient(args[0]);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}