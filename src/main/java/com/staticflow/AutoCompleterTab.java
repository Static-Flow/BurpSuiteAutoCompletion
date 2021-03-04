package com.staticflow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class AutoCompleterTab extends JPanel {

    private enum MODE {
        DELETE,
        ADD
    }

    private DefaultListModel<String> listerModel;
    private JButton addNewKeyword;
    private JTextField newKeywordField;
    private MODE currentMode = MODE.ADD;
    private String currentlyEdittingCompletion;
    AutoCompleterTab() {
        this.initTab();
    }

    void addKeywordToList(String keyword) {
        listerModel.addElement(keyword);
    }

    private void initTab(){
        JPanel mainPane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        listerModel = new DefaultListModel<>();
        JList<String> lister = new JList<>(listerModel);
        lister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JList list = (JList)e.getSource();
                if (e.getClickCount() == 1) {
                    currentMode = MODE.DELETE;
                    addNewKeyword.setText("Delete");
                    int index = list.locationToIndex(e.getPoint());
                    currentlyEdittingCompletion = listerModel.elementAt(index);
                    newKeywordField.setText(currentlyEdittingCompletion);

                }
            }
        });
        JScrollPane scroller = new JScrollPane(lister);
        setLayout(new BorderLayout());
        c.weighty = 0.9;
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy = 0;
        mainPane.add(scroller,c);
        c.weighty = 0.1;
        c.anchor = GridBagConstraints.SOUTH;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.9;
        newKeywordField = new JTextField(50);
        mainPane.add(newKeywordField,c);
        c.gridx = 1;
        c.gridwidth = 1;
        c.weightx = 0.1;
        c.gridy = 1;
        addNewKeyword = new JButton("Add");
        addNewKeyword.addActionListener(e -> {
            if (currentMode == MODE.ADD) {
                ExtensionState.getInstance().getKeywords().add(newKeywordField.getText().trim());
                listerModel.addElement(newKeywordField.getText().trim());
            } else if (currentMode == MODE.DELETE) {
                ExtensionState.getInstance().getKeywords().remove(newKeywordField.getText().trim());
                listerModel.removeElement(newKeywordField.getText().trim());
            }
            currentMode = MODE.ADD;
            addNewKeyword.setText("Add");
            newKeywordField.setText("");
            currentlyEdittingCompletion = "";

        });
        mainPane.add(addNewKeyword,c);
        add(mainPane,BorderLayout.CENTER);

    }
}
