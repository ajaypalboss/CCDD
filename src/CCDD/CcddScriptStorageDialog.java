/**
 * CFS Command & Data Dictionary script storage dialog. Copyright 2017 United
 * States Government as represented by the Administrator of the National
 * Aeronautics and Space Administration. No copyright is claimed in the United
 * States under Title 17, U.S. Code. All Other Rights Reserved.
 */
package CCDD;

import static CCDD.CcddConstants.LABEL_FONT_BOLD;
import static CCDD.CcddConstants.LABEL_FONT_PLAIN;
import static CCDD.CcddConstants.LABEL_HORIZONTAL_SPACING;
import static CCDD.CcddConstants.LABEL_VERTICAL_SPACING;
import static CCDD.CcddConstants.LAST_SCRIPT_PATH;
import static CCDD.CcddConstants.OK_BUTTON;
import static CCDD.CcddConstants.SCRIPTS_ICON;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import CCDD.CcddClasses.CCDDException;
import CCDD.CcddConstants.DialogOption;
import CCDD.CcddConstants.InternalTable;
import CCDD.CcddConstants.ScriptIOType;

/******************************************************************************
 * CFS Command & Data Dictionary script storage dialog class
 *****************************************************************************/
@SuppressWarnings("serial")
public class CcddScriptStorageDialog extends CcddDialogHandler
{
    // Class references
    private final CcddMain ccddMain;
    private final CcddDbTableCommandHandler dbTable;
    private final CcddDbControlHandler dbControl;
    private final CcddScriptHandler scriptHandler;
    private final CcddFileIOHandler fileIOHandler;

    // Components referenced by multiple methods
    private final ScriptIOType dialogType;

    // Array of file references containing the selected script file(s)
    // (store) or the selected script file path (retrieve)
    private File[] scriptFile;

    // Path selection field
    private JTextField pathFld;

    /**************************************************************************
     * Script storage dialog class constructor
     * 
     * @param ccddMain
     *            main class
     * 
     * @param dialogType
     *            ScriptIOType.STORE or ScriptIOType.RETRIEVE
     *************************************************************************/
    protected CcddScriptStorageDialog(CcddMain ccddMain,
                                      ScriptIOType dialogType)
    {
        this.ccddMain = ccddMain;
        this.dialogType = dialogType;

        // Create references to shorten subsequent calls
        dbTable = ccddMain.getDbTableCommandHandler();
        dbControl = ccddMain.getDbControlHandler();
        scriptHandler = ccddMain.getScriptHandler();
        fileIOHandler = ccddMain.getFileIOHandler();

        // Create the script storage dialog
        initialize();
    }

    /**************************************************************************
     * Create the script storage dialog
     *************************************************************************/
    private void initialize()
    {
        // Create the dialog based on the supplied dialog type
        switch (dialogType)
        {
            case STORE:
                // Allow the user to select the script file path(s) + name(s)
                scriptFile = new CcddDialogHandler().choosePathFile(ccddMain,
                                                                    ccddMain.getMainFrame(),
                                                                    null,
                                                                    scriptHandler.getExtensions(),
                                                                    false,
                                                                    true,
                                                                    "Select Script(s) to Store",
                                                                    LAST_SCRIPT_PATH,
                                                                    DialogOption.STORE_OPTION);

                // Check if the Cancel button wasn't selected
                if (scriptFile != null)
                {
                    // Check that a file is selected
                    if (scriptFile[0] != null && scriptFile[0].isFile())
                    {
                        // Get the script file path + name
                        String pathName = scriptFile[0].getAbsolutePath();

                        // Remove the script file name and store the script
                        // file path in the program preferences backing store
                        fileIOHandler.storePath(pathName.substring(0, pathName.lastIndexOf(File.separator)),
                                                true,
                                                LAST_SCRIPT_PATH);

                        // Step through each selected script
                        for (File file : scriptFile)
                        {
                            // Store the script in the database
                            fileIOHandler.storeScriptInDatabase(file);
                        }
                    }
                    // No script file is selected
                    else
                    {
                        // Inform the user that a script file must be selected
                        new CcddDialogHandler().showMessageDialog(ccddMain.getMainFrame(),
                                                                  "<html><b>Must select a script to store",
                                                                  "Store Script(s)",
                                                                  JOptionPane.WARNING_MESSAGE,
                                                                  DialogOption.OK_OPTION);
                    }
                }

                break;

            case RETRIEVE:
            case DELETE:
                // Set the initial layout manager characteristics
                GridBagConstraints gbc = new GridBagConstraints(0,
                                                                1,
                                                                1,
                                                                1,
                                                                1.0,
                                                                0.0,
                                                                GridBagConstraints.LINE_START,
                                                                GridBagConstraints.BOTH,
                                                                new Insets(LABEL_VERTICAL_SPACING,
                                                                           LABEL_HORIZONTAL_SPACING,
                                                                           LABEL_VERTICAL_SPACING,
                                                                           LABEL_HORIZONTAL_SPACING),
                                                                0,
                                                                0);

                // Create the panel to contain the dialog components
                JPanel dialogPnl = new JPanel(new GridBagLayout());
                dialogPnl.setBorder(BorderFactory.createEmptyBorder());

                // Get the list of script files from the database
                String[] scripts = dbTable.queryScriptTables(ccddMain.getMainFrame());
                String[][] checkBoxData = new String[scripts.length][];
                int index = 0;

                // Step through each script file
                for (String script : scripts)
                {
                    // Separate and store the database name and description
                    checkBoxData[index] = script.split(",", 2);
                    index++;
                }

                // Create a panel containing a grid of check boxes representing
                // the scripts from which to choose
                if (addCheckBoxes(null,
                                  checkBoxData,
                                  null,
                                  "Select script(s)",
                                  dialogPnl))
                {

                    // Check if more than one data field name check box exists
                    if (getCheckBoxes().length > 2)
                    {
                        // Create a Select All check box
                        final JCheckBox selectAllCb = new JCheckBox("Select all scripts",
                                                                    false);
                        selectAllCb.setFont(LABEL_FONT_BOLD);
                        selectAllCb.setBorder(BorderFactory.createEmptyBorder());

                        // Create a listener for changes to the Select All
                        // check box selection status
                        selectAllCb.addActionListener(new ActionListener()
                        {
                            /**************************************************
                             * Handle a change to the Select All check box
                             * selection status
                             *************************************************/
                            @Override
                            public void actionPerformed(ActionEvent ae)
                            {
                                // Step through each data field name check box
                                for (JCheckBox scriptCb : getCheckBoxes())
                                {
                                    // Set the check box selection status to
                                    // match the Select All check box selection
                                    // status
                                    scriptCb.setSelected(selectAllCb.isSelected());
                                }
                            }
                        });

                        // Add the Select All checkbox to the dialog panel
                        gbc.gridx = 0;
                        gbc.gridy++;
                        gbc.insets.bottom = 0;
                        dialogPnl.add(selectAllCb, gbc);
                    }

                    // Check if one or more scripts is to be retrieved
                    if (dialogType == ScriptIOType.RETRIEVE)
                    {
                        // Add the script path selection components to the
                        // dialog
                        gbc.gridy++;
                        gbc.insets.bottom = 0;
                        dialogPnl.add(createPathSelectionPanel(), gbc);

                        // Display the script retrieval dialog
                        if (showOptionsDialog(ccddMain.getMainFrame(),
                                              dialogPnl,
                                              "Retrieve Script(s)",
                                              DialogOption.RETRIEVE_OPTION,
                                              true) == OK_BUTTON)
                        {
                            // Check if no script file path is selected via the
                            // selection button
                            if (scriptFile == null)
                            {
                                // Get a file reference using the last accessed
                                // file path
                                scriptFile = new File[] {new File(ccddMain.getProgPrefs().get(LAST_SCRIPT_PATH,
                                                                                              ""))};
                            }
                            // A script file path is selected
                            else
                            {
                                // Store the script file path in the program
                                // preferences backing store
                                fileIOHandler.storePath(scriptFile[0].getAbsolutePath(),
                                                        true,
                                                        LAST_SCRIPT_PATH);

                            }

                            // Get an array containing the selected script
                            // names
                            String[] selectedScripts = getCheckBoxSelected();

                            // Step through each selected script file
                            for (String script : selectedScripts)
                            {
                                // Retrieve the selected scripts from the
                                // database and save them to the selected
                                // folder
                                fileIOHandler.retrieveScriptFromDatabase(script,
                                                                         new File(scriptFile[0].getAbsolutePath()
                                                                                  + File.separator
                                                                                  + script));
                            }
                        }
                    }
                    // One or more scripts is to be deleted
                    else
                    {
                        // Display the database deletion dialog
                        if (showOptionsDialog(ccddMain.getMainFrame(),
                                              dialogPnl,
                                              "Delete Script(s)",
                                              DialogOption.DELETE_OPTION,
                                              true) == OK_BUTTON)
                        {
                            // Get the array of selected scripts
                            String[] selectedScripts = getCheckBoxSelected();

                            // Step through each script selected
                            for (index = 0; index < selectedScripts.length; index++)
                            {
                                // Prepend the database script file identifier
                                // to the script name
                                selectedScripts[index] = InternalTable.SCRIPT.getTableName(selectedScripts[index]);
                            }

                            // Delete the selected scripts
                            dbTable.deleteTableInBackground(selectedScripts,
                                                            null,
                                                            ccddMain.getMainFrame());
                        }
                    }
                }
                // No scripts are stored in the database
                else
                {
                    // Inform the user that the project database contains no
                    // script to retrieve/delete
                    new CcddDialogHandler().showMessageDialog(ccddMain.getMainFrame(),
                                                              "<html><b>Project '</b>"
                                                                  + dbControl.getDatabase()
                                                                  + "<b>' has no scripts",
                                                              (dialogType == ScriptIOType.RETRIEVE
                                                                                                  ? "Retrieve"
                                                                                                  : "Delete")
                                                                  + " Script(s)",
                                                              JOptionPane.WARNING_MESSAGE,
                                                              DialogOption.OK_OPTION);
                }

                break;
        }
    }

    /**************************************************************************
     * Create the path selection panel
     * 
     * @return JPanel containing the script selection panel
     *************************************************************************/
    private JPanel createPathSelectionPanel()
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
                                                                   0,
                                                                   LABEL_VERTICAL_SPACING / 2,
                                                                   LABEL_HORIZONTAL_SPACING),
                                                        0,
                                                        0);

        // Create a panel for the path selection components
        JPanel pathPnl = new JPanel(new GridBagLayout());

        // Create the path selection dialog labels and fields
        JLabel scriptLbl = new JLabel("Enter or select a script storage path");
        scriptLbl.setFont(LABEL_FONT_BOLD);
        pathPnl.add(scriptLbl, gbc);

        // Create a text field for entering & displaying the script path
        pathFld = new JTextField(ccddMain.getProgPrefs().get(LAST_SCRIPT_PATH,
                                                             "").replaceAll("\\" + File.separator + "\\.$", ""));
        pathFld.setFont(LABEL_FONT_PLAIN);
        pathFld.setEditable(true);
        pathFld.setForeground(Color.BLACK);
        pathFld.setBackground(Color.WHITE);
        pathFld.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,
                                                                                             Color.LIGHT_GRAY,
                                                                                             Color.GRAY),
                                                             BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        gbc.insets.left = LABEL_HORIZONTAL_SPACING;
        gbc.gridy++;
        pathPnl.add(pathFld, gbc);

        // Create a button for choosing an output path
        JButton btnSelectPath = CcddButtonPanelHandler.createButton("Select...",
                                                                    SCRIPTS_ICON,
                                                                    KeyEvent.VK_S,
                                                                    "Open the script path selection dialog");

        // Add a listener for the Select path button
        btnSelectPath.addActionListener(new ActionListener()
        {
            /******************************************************************
             * Select a script storage path
             *****************************************************************/
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                // Allow the user to select the script storage path
                scriptFile = new CcddDialogHandler().choosePathFile(ccddMain,
                                                                    CcddScriptStorageDialog.this,
                                                                    null,
                                                                    null,
                                                                    true,
                                                                    false,
                                                                    "Select Location for Script(s)",
                                                                    LAST_SCRIPT_PATH,
                                                                    DialogOption.OK_CANCEL_OPTION);

                // Check if a script path is selected
                if (scriptFile != null && scriptFile[0] != null)
                {
                    // Display the path name in the script path field
                    pathFld.setText(scriptFile[0].getAbsolutePath());
                }
            }
        });

        // Add the select script button to the dialog
        gbc.weightx = 0.0;
        gbc.insets.right = 0;
        gbc.gridx++;
        pathPnl.add(btnSelectPath, gbc);

        return pathPnl;
    }

    /**************************************************************************
     * Verify that the dialog content is valid
     * 
     * @return true if the input values are valid
     *************************************************************************/
    @Override
    protected boolean verifySelection()
    {
        // Assume the dialog input is valid
        boolean isValid = true;

        try
        {
            // Verify the dialog content based on the supplied dialog type
            switch (dialogType)
            {
                case RETRIEVE:
                    // Check that a script is selected
                    if (getCheckBoxSelected().length == 0)
                    {
                        // Inform the user that a script must be selected
                        throw new CCDDException("Must select a script to retrieve");
                    }

                    // Check if no script path is selected
                    if (pathFld.getText().trim().isEmpty())
                    {
                        // Inform the user that a script path must be selected
                        throw new CCDDException("Must select a script location");
                    }

                    break;

                case DELETE:
                    // Check that a script is selected
                    if (getCheckBoxSelected().length == 0)
                    {
                        // Inform the user that a script must be selected
                        throw new CCDDException("Must select a script to delete");
                    }

                    break;

                case STORE:
                    break;
            }
        }
        catch (CCDDException ce)
        {
            // Inform the user that the input value is invalid
            new CcddDialogHandler().showMessageDialog(CcddScriptStorageDialog.this,
                                                      "<html><b>"
                                                          + ce.getMessage(),
                                                      "Missing Input",
                                                      JOptionPane.WARNING_MESSAGE,
                                                      DialogOption.OK_OPTION);

            // Set the flag to indicate the dialog input is invalid
            isValid = false;
        }

        return isValid;
    }
}
