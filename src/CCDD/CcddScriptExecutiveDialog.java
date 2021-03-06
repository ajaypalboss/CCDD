/**
 * CFS Command & Data Dictionary script executive dialog. Copyright 2017 United
 * States Government as represented by the Administrator of the National
 * Aeronautics and Space Administration. No copyright is claimed in the United
 * States under Title 17, U.S. Code. All Other Rights Reserved.
 */
package CCDD;

import static CCDD.CcddConstants.CLOSE_ICON;
import static CCDD.CcddConstants.EXECUTE_ALL_ICON;
import static CCDD.CcddConstants.EXECUTE_ICON;
import static CCDD.CcddConstants.LABEL_FONT_BOLD;
import static CCDD.CcddConstants.LABEL_HORIZONTAL_SPACING;
import static CCDD.CcddConstants.LABEL_VERTICAL_SPACING;
import static CCDD.CcddConstants.OK_BUTTON;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import CCDD.CcddConstants.DialogOption;

/******************************************************************************
 * CFS Command & Data Dictionary script executive dialog class
 *****************************************************************************/
@SuppressWarnings("serial")
public class CcddScriptExecutiveDialog extends CcddDialogHandler
{
    // Class references
    private final CcddMain ccddMain;
    private final CcddScriptHandler scriptHandler;
    private CcddTableTreeHandler tableTree;

    // Components referenced by multiple methods
    private JButton btnExecute;
    private JButton btnExecuteAll;
    private JButton btnClose;

    /**************************************************************************
     * Script executive dialog class constructor
     * 
     * @param ccddMain
     *            main class
     *************************************************************************/
    protected CcddScriptExecutiveDialog(CcddMain ccddMain)
    {
        this.ccddMain = ccddMain;

        // Create reference to shorten subsequent calls
        scriptHandler = ccddMain.getScriptHandler();

        // Create the script executive dialog
        initialize();
    }

    /**************************************************************************
     * Create the script executive dialog
     *************************************************************************/
    private void initialize()
    {
        // Set the initial layout manager characteristics
        GridBagConstraints gbc = new GridBagConstraints(0,
                                                        0,
                                                        1,
                                                        1,
                                                        1.0,
                                                        0.0,
                                                        GridBagConstraints.LINE_START,
                                                        GridBagConstraints.BOTH,
                                                        new Insets(LABEL_VERTICAL_SPACING / 2,
                                                                   LABEL_HORIZONTAL_SPACING,
                                                                   0,
                                                                   LABEL_HORIZONTAL_SPACING),
                                                        0,
                                                        0);

        // Create a panel to hold the components of the dialog
        JPanel dialogPnl = new JPanel(new GridBagLayout());
        dialogPnl.setBorder(BorderFactory.createEmptyBorder());

        // Create the table group selection dialog labels and fields
        JLabel scriptLbl = new JLabel("Select script association(s) to execute");
        scriptLbl.setFont(LABEL_FONT_BOLD);
        dialogPnl.add(scriptLbl, gbc);

        // Create the list to display the stored script associations and add
        // it to the dialog
        gbc.weighty = 1.0;
        gbc.insets.top = 0;
        gbc.gridy++;
        dialogPnl.add(scriptHandler.createScriptAssociationPanel(null,
                                                                 10,
                                                                 false,
                                                                 CcddScriptExecutiveDialog.this),
                      gbc);

        // Create the button panel
        JPanel buttonPnl = new JPanel();

        // Execute selected script association(s) button
        btnExecute = CcddButtonPanelHandler.createButton("Execute",
                                                         EXECUTE_ICON,
                                                         KeyEvent.VK_E,
                                                         "Execute the selected script association(s)");

        // Add a listener for the Execute button
        btnExecute.addActionListener(new ActionListener()
        {
            /******************************************************************
             * Execute the selected script association(s)
             *****************************************************************/
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                // Check if at least one script association is selected
                if (!scriptHandler.getAssociationsList().isSelectionEmpty())
                {
                    scriptHandler.executeScriptAssociations(CcddScriptExecutiveDialog.this,
                                                            tableTree);
                }
            }
        });

        // Execute all script associations button
        btnExecuteAll = CcddButtonPanelHandler.createButton("Execute All",
                                                            EXECUTE_ALL_ICON,
                                                            KeyEvent.VK_A,
                                                            "Execute all of the script associations");

        // Add a listener for the Execute All button
        btnExecuteAll.addActionListener(new ActionListener()
        {
            /******************************************************************
             * Execute all of the script associations
             *****************************************************************/
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                // Check if at least one script association exists
                if (!scriptHandler.getAssociationsModel().isEmpty()
                    && new CcddDialogHandler().showMessageDialog(CcddScriptExecutiveDialog.this,
                                                                 "<html><b>Execute all script associations?",
                                                                 "Execute All",
                                                                 JOptionPane.QUESTION_MESSAGE,
                                                                 DialogOption.OK_CANCEL_OPTION) == OK_BUTTON)
                {
                    // Select all script associations
                    scriptHandler.getAssociationsList().setSelectionInterval(0,
                                                                             scriptHandler.getAssociationsModel().size()
                                                                             - 1);

                    // Execute all of the script associations
                    scriptHandler.executeScriptAssociations(CcddScriptExecutiveDialog.this,
                                                            tableTree);
                }
            }
        });

        // Close button
        btnClose = CcddButtonPanelHandler.createButton("Close",
                                                       CLOSE_ICON,
                                                       KeyEvent.VK_C,
                                                       "Close the script executive");

        // Add a listener for the Close button
        btnClose.addActionListener(new ActionListener()
        {
            /******************************************************************
             * Close the script execution dialog
             *****************************************************************/
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                closeDialog();
            }
        });

        // Add buttons to the button panel
        buttonPnl.add(btnExecute);
        buttonPnl.add(btnExecuteAll);
        buttonPnl.add(btnClose);

        // Display the script execution dialog
        showOptionsDialog(ccddMain.getMainFrame(),
                          dialogPnl,
                          buttonPnl,
                          "Execute Script(s)",
                          true);
    }
}
