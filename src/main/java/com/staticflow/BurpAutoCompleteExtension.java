package com.staticflow;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionStateListener;
import burp.ITab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.util.Arrays;


public class BurpAutoCompleteExtension  implements IBurpExtender , AWTEventListener, IExtensionStateListener, ITab {


    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks iBurpExtenderCallbacks) {
        ExtensionState.setCallbacks(iBurpExtenderCallbacks);
        Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.KEY_EVENT_MASK);
        iBurpExtenderCallbacks.registerExtensionStateListener(this);
        iBurpExtenderCallbacks.addSuiteTab(this);
    }


    @Override
    public void extensionUnloaded() {
        ExtensionState.getInstance().getCallbacks().printOutput("removing listeners");
        System.out.println(Arrays.toString(Toolkit.getDefaultToolkit().getAWTEventListeners()));

        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        System.out.println(Arrays.toString(Toolkit.getDefaultToolkit().getAWTEventListeners()));
        for(AutoCompleter listener : ExtensionState.getInstance().getListeners()) {
            listener.detachFromSource();
            listener.getSource().getDocument().removeDocumentListener(listener);
        }
    }


    /**
     * This hooks keyboard events for the entire application. Only textareas are considered. Practically, this includes
     * Repeater, Intruder, and any extension which uses JTextArea.
     * @param event keyboard event
     */
    @Override
    public void eventDispatched(AWTEvent event) {
        if(event.getSource() instanceof JTextArea) {
            JTextArea source = ((JTextArea)event.getSource());
            if(source.getClientProperty("hasListener") ==  null || !((Boolean) source.getClientProperty("hasListener"))) {
                ExtensionState.getInstance().getCallbacks().printOutput("Adding Listener");
                AutoCompleter t = new AutoCompleter(source);
                source.getDocument().addDocumentListener(t);
                source.putClientProperty("hasListener",true);
                ExtensionState.getInstance().addListener(t);
            }
        }
    }

    @Override
    public String getTabCaption() {
        return "Autocompleter";
    }

    @Override
    public Component getUiComponent() {
        return ExtensionState.getInstance().getAutoCompleterTab();
    }
}


