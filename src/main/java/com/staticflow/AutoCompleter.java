package com.staticflow;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class handles the autocomplete. it keeps a reference to the JTextArea is autocompletes for and generates a list
 * of possible candidates, updated after every letter typed.
 */
public class AutoCompleter implements DocumentListener, CaretListener{

    //The document we are autocompleting for
    private JTextArea source;
    //Our current offset position in the document
    private int pos;
    //Stateflag to determine if the last action was a backspace
    private boolean backspaceMode;
    //The suggestion frame which holds the current autocomplete candidates
    private JFrame suggestionPane;
    //List model to hold the candidate autocompletions
    private DefaultListModel<String> suggestionsModel = new DefaultListModel<>();
    //The content of the source document we will be replacing
    private String content;
    private enum MODE {
        INSERT,
        COMPLETION
    }
    private MODE mode = MODE.INSERT;

    /**
     * This listener follows the caret and updates where we should draw the suggestions box
     * @param e the carent event
     */
    @Override
    public void caretUpdate(CaretEvent e) {
        pos = e.getDot();
        System.out.println("Caret: "+pos);
        Point p = source.getCaret().getMagicCaretPosition();
        if(p != null) {
            Point np = new Point();
            np.x = p.x + source.getLocationOnScreen().x;
            np.y = p.y + source.getLocationOnScreen().y+25;
            suggestionPane.setLocation(np);
        }
    }


    /**
     * Initializes the suggestion pane and attaches our listeners
     * @param s the source to provide autocompletions for
     */
    AutoCompleter(JTextArea s) {
        this.source = s;
        this.pos = this.source.getCaret().getDot();
        this.source.addCaretListener(this);
        suggestionPane = new JFrame();
        suggestionPane.setSize(250,250);
        suggestionPane.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        suggestionPane.setUndecorated(true);
        suggestionPane.setAutoRequestFocus(false);
        JPanel pane = new JPanel(new BorderLayout());
        JList<String> suggestions = new JList<>(suggestionsModel);
        JScrollPane scroller = new JScrollPane(suggestions);
        pane.add(scroller, BorderLayout.CENTER);
        suggestionPane.add(pane);
        //Double clicks will pick the autocompletion to commit to
        suggestions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JList list = (JList)e.getSource();
                if (e.getClickCount() == 2) {

                    // Double-click detected
                    int start = getTextReplacementStart();
                    int index = list.locationToIndex(e.getPoint());
                    String selectedCompletion = suggestionsModel.elementAt(index);
                    System.out.println(start+1 + " : " + pos+1);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            source.select(start+1,pos);
                            source.replaceSelection(selectedCompletion+": ");
                            source.setCaretPosition(source.getSelectionEnd());
                            suggestionPane.setVisible(false);
                        }
                    });

                }
            }
        });

    }

    /**
     * Get's the start of the users text we are replacing
     * @return starting index of the users input
     */
    private int getTextReplacementStart() {
        int start;
        if(backspaceMode) {
            for (start = pos-2; start >= 0; start--) {
                System.out.println(content.charAt(start));
                if (Character.isWhitespace(content.charAt(start))) {
                    break;
                }
            }
        } else {
            for (start = pos-1; start >= 0; start--) {
                if (Character.isWhitespace(content.charAt(start))) {
                    break;
                }
            }
        }
        return start;
    }

    JTextArea getSource() {
        return this.source;
    }

    void detachFromSource(){
        this.suggestionPane.dispose();
        this.source.removeCaretListener(this);
        this.source.getDocument().removeDocumentListener(this);


    }

    /**
     * Searches the autocompletions for candidates. Exact matches are ignored.
     * @param search What to search for
     * @return the results that match, if any
     */
    private static ArrayList<String> prefixSearcher(String search) {
        ArrayList<String> results = new ArrayList<>();
        for(String in : ExtensionState.getInstance().getKeywords()) {
            if( !in.toLowerCase().equals(search.trim()) && in.toLowerCase().startsWith(search.trim()) ) {
                results.add(in);
            }
        }
        return results;
    }



    @Override
    public void insertUpdate(DocumentEvent e) {
        if (mode == MODE.COMPLETION) {
            mode = MODE.INSERT;
        } else {
            backspaceMode = false;
            if (Character.isWhitespace(this.source.getText().charAt(pos))) {
                suggestionPane.setVisible(false);
            } else {
                checkForCompletions();
            }
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        if (mode == MODE.COMPLETION) {
            mode = MODE.INSERT;
        } else {
            backspaceMode = true;
//            if (Character.isWhitespace(source.getText().charAt(pos))) {
//                suggestionPane.setVisible(false);
//            } else {
//                checkForCompletions();
//            }
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {

    }


    /**
     * Handles changes to the document by getting the recent word entered by the user and searching for completion candidates.
     */
    private void checkForCompletions() {
        //pos = e.getOffset();
        content = null;

        try {
            content = this.source.getText(0, pos + 1);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        System.out.println(content.charAt(pos));
        System.out.println(content);
//        if (e.getLength() != 1) {
//            return;
//        }
//
//
        int start = getTextReplacementStart();
//


//
        if (pos - start < 1 && !backspaceMode) {
            return;
        }
//
//
        String prefix = content.substring(start + 1);
        ExtensionState.getInstance().getCallbacks().printOutput("Searching for " + prefix);
        if (prefix.trim().length() == 0 || prefix.contains(":") || prefix.trim().length() == 1) {
            suggestionPane.setVisible(false);
        } else {
            ArrayList<String> matches = prefixSearcher(prefix.toLowerCase());
            ExtensionState.getInstance().getCallbacks().printOutput(Arrays.toString(matches.toArray()));
            if (matches.size() != 0) {
                SwingUtilities.invokeLater(
                        new CompletionTask(matches));
            } else {
                suggestionPane.setVisible(false);
            }
        }
    }


    /**
     * Updates the suggestion pane with the new options
     */
    private class CompletionTask
            implements Runnable {

        CompletionTask(ArrayList<String> completions) {
            mode = MODE.COMPLETION;
            suggestionsModel.removeAllElements();
            for(String completion : completions) {
                suggestionsModel.addElement(completion);
            }
        }

        @Override
        public void run() {
            suggestionPane.setVisible(true);
        }
    }


}
