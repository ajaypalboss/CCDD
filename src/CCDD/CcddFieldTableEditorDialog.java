/**
 * CFS Command & Data Dictionary data field table editor dialog. Copyright 2017
 * United States Government as represented by the Administrator of the National
 * Aeronautics and Space Administration. No copyright is claimed in the United
 * States under Title 17, U.S. Code. All Other Rights Reserved.
 */
package CCDD;

import static CCDD.CcddConstants.CLOSE_ICON;
import static CCDD.CcddConstants.DELETE_ICON;
import static CCDD.CcddConstants.FOCUS_COLOR;
import static CCDD.CcddConstants.LABEL_FONT_BOLD;
import static CCDD.CcddConstants.LABEL_FONT_PLAIN;
import static CCDD.CcddConstants.LABEL_HORIZONTAL_SPACING;
import static CCDD.CcddConstants.LABEL_VERTICAL_SPACING;
import static CCDD.CcddConstants.OK_BUTTON;
import static CCDD.CcddConstants.OK_ICON;
import static CCDD.CcddConstants.PRINT_ICON;
import static CCDD.CcddConstants.REDO_ICON;
import static CCDD.CcddConstants.SELECTED_BACK_COLOR;
import static CCDD.CcddConstants.STORE_ICON;
import static CCDD.CcddConstants.TABLE_BACK_COLOR;
import static CCDD.CcddConstants.TABLE_ICON;
import static CCDD.CcddConstants.UNDO_ICON;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;

import CCDD.CcddClasses.CellSelectionHandler;
import CCDD.CcddClasses.FieldInformation;
import CCDD.CcddClasses.ValidateCellActionListener;
import CCDD.CcddConstants.DialogOption;
import CCDD.CcddConstants.FieldTableEditorColumnInfo;
import CCDD.CcddConstants.InputDataType;
import CCDD.CcddConstants.InternalTable;
import CCDD.CcddConstants.InternalTable.FieldsColumn;
import CCDD.CcddConstants.TableSelectionMode;
import CCDD.CcddConstants.TableTreeType;
import CCDD.CcddEditorPanelHandler.UndoableTextField;
import CCDD.CcddTableTypeHandler.TypeDefinition;

/******************************************************************************
 * CFS Command & Data Dictionary data field table editor dialog class
 *****************************************************************************/
@SuppressWarnings("serial")
public class CcddFieldTableEditorDialog extends CcddFrameHandler
{
    // Class references
    private final CcddMain ccddMain;
    private final CcddDbTableCommandHandler dbTable;
    private CcddFieldHandler fieldHandler;
    private final CcddTableTypeHandler tableTypeHandler;
    private CcddDialogHandler selectDlg;
    private CcddTableTreeHandler tableTree;

    // Components that need to be accessed by multiple methods
    private CcddJTableHandler dataFieldTable;

    // Data field information
    private List<String[]> dataFields;

    // Array of data field owner names that aren't structure tables
    private List<String> nonStructureTableNames;

    // Table column names
    private String[] columnNames;

    // List of columns representing boolean data fields
    private List<Integer> checkBoxColumns;

    // Flag that indicates if any of the tables with data fields to display are
    // children of another table, and therefore have a structure path
    private boolean isPath;

    // Table instance model data. Committed copy is the table information as it
    // exists in the database and is used to determine what changes have been
    // made to the table since the previous database update
    private Object[][] committedData;

    // List of data field content changes to process
    private final List<String[]> fieldModifications;

    // List of data field deletions to process
    private final List<String[]> fieldDeletions;

    // Cell selection container
    private CellSelectionHandler selectedCells;

    // Node selection change in progress flag
    private boolean isNodeSelectionChanging;

    // Dialog title
    private static final String DIALOG_TITLE = "Show/Edit Data Fields";

    /**************************************************************************
     * Data field table editor dialog class constructor
     * 
     * @param ccddMain
     *            main class
     *************************************************************************/
    protected CcddFieldTableEditorDialog(CcddMain ccddMain)
    {
        this.ccddMain = ccddMain;
        dbTable = ccddMain.getDbTableCommandHandler();
        tableTypeHandler = ccddMain.getTableTypeHandler();

        selectDlg = null;

        // Initialize the list of table content changes and deletions
        fieldModifications = new ArrayList<String[]>();
        fieldDeletions = new ArrayList<String[]>();

        // Load the data field information from the database
        getDataFieldInformation();

        // Allow the user to select the data fields to display in the table
        if (selectDataFields())
        {
            // Create the cell selection container
            selectedCells = new CellSelectionHandler();

            // Create the data field editor dialog
            initialize();
        }
    }

    /**************************************************************************
     * Get a reference to the data field table editor's table
     * 
     * @return Reference to the data field table editor table
     *************************************************************************/
    protected CcddJTableHandler getTable()
    {
        return dataFieldTable;
    }

    /**************************************************************************
     * Get the data field information from the database
     *************************************************************************/
    private void getDataFieldInformation()
    {
        // Get the array containing the data fields
        dataFields = dbTable.retrieveInformationTable(InternalTable.FIELDS,
                                                      CcddFieldTableEditorDialog.this);

        // Create a list to contain all non-structure table names
        nonStructureTableNames = new ArrayList<String>();

        // Step through each table type
        for (TypeDefinition typeDefn : tableTypeHandler.getTypeDefinitions())
        {
            // Check if the type doesn't represent a structure
            if (!typeDefn.isStructure())
            {
                // Add all table names of this type to the list of
                // non-structure tables
                nonStructureTableNames.addAll(Arrays.asList(dbTable.queryTablesOfTypeList(typeDefn.getName(),
                                                                                          CcddFieldTableEditorDialog.this)));
            }
        }
    }

    /**************************************************************************
     * Get the latest data field information and update the data field editor
     * table
     *************************************************************************/
    protected void reloadDataFieldTable()
    {
        // Load the data field information from the database
        getDataFieldInformation();

        // Load the data field information into the data field editor table
        dataFieldTable.loadAndFormatData();
    }

    /**************************************************************************
     * Perform the steps needed following execution of database table changes
     * 
     * @param commandError
     *            false if the database commands successfully completed; true
     *************************************************************************/
    protected void doDataFieldUpdatesComplete(boolean commandError)
    {
        // Check that no error occurred performing the database commands
        if (!commandError)
        {
            // Clear the cells selected for deletion
            selectedCells.clear();

            // Load the data field information into the data field editor table
            reloadDataFieldTable();

            // Step through the open editor dialogs
            for (CcddTableEditorDialog editorDialog : ccddMain.getTableEditorDialogs())
            {
                // Step through each individual editor in this editor dialog
                for (CcddTableEditorHandler editor : editorDialog.getTableEditors())
                {
                    List<String> redrawnTables = new ArrayList<String>();

                    // Step through the data field deletions
                    for (String[] del : fieldDeletions)
                    {
                        // Check if the table names and paths match, and that
                        // this table hasn't already had a deletion applied
                        if (del[0].equals(editor.getTableInformation().getTablePath())
                            && !redrawnTables.contains(del[0]))
                        {
                            // Add the table's name and path to the list of
                            // updated tables. This is used to prevent updating
                            // the data fields for a table multiple times
                            redrawnTables.add(del[0]);

                            // Rebuild the table's data field information
                            editor.getTableInformation().getFieldHandler().setFieldDefinitions(dataFields);
                            editor.getTableInformation().getFieldHandler().buildFieldInformation(del[0]);

                            // Store the data field information in the
                            // committed information so that this value change
                            // is ignored when updating or closing the table
                            editor.getCommittedTableInformation().getFieldHandler().setFieldInformation(editor.getTableInformation().getFieldHandler().getFieldInformationCopy());

                            // Rebuild the table's editor panel which contains
                            // the data fields
                            editor.createDataFieldPanel();

                            // Force the table editor to redraw in order for
                            // the field updates to appear
                            editor.getTableEditor().repaint();
                        }
                    }

                    // Step through the data field value modifications
                    for (String[] mod : fieldModifications)
                    {
                        // Check if the table names and paths match and that
                        // this table hasn't already been updated by having a
                        // deletion applied
                        if (mod[0].equals(editor.getTableInformation().getTablePath())
                            && !redrawnTables.contains(mod[0]))
                        {
                            // Get the reference to the modified field
                            FieldInformation fieldInfo = editor.getTableInformation().getFieldHandler().getFieldInformationByName(mod[0],
                                                                                                                                  mod[1]);

                            // Update the field's value. Also update the value
                            // in the committed information so that this value
                            // change is ignored when updating or closing the
                            // table
                            fieldInfo.setValue(mod[2]);
                            editor.getCommittedTableInformation().getFieldHandler().getFieldInformationByName(mod[0],
                                                                                                              mod[1]).setValue(mod[2]);

                            // Check that this isn't a boolean input (check
                            // box) data field
                            if (fieldInfo.getInputType() != InputDataType.BOOLEAN)
                            {
                                // Display the updated value in the text field
                                ((UndoableTextField) fieldInfo.getInputFld()).setText(mod[2]);
                            }
                        }
                    }
                }
            }

            // Clear the undo/redo cell edits stack
            dataFieldTable.getUndoManager().discardAllEdits();
        }
    }

    /**************************************************************************
     * Create the data field editor dialog
     *************************************************************************/
    private void initialize()
    {
        // Set the initial layout manager characteristics
        GridBagConstraints gbc = new GridBagConstraints(0,
                                                        0,
                                                        1,
                                                        1,
                                                        1.0,
                                                        1.0,
                                                        GridBagConstraints.LINE_START,
                                                        GridBagConstraints.HORIZONTAL,
                                                        new Insets(LABEL_VERTICAL_SPACING,
                                                                   0,
                                                                   0,
                                                                   0),
                                                        LABEL_HORIZONTAL_SPACING,
                                                        0);

        // Create a panel to contain the dialog components
        JPanel dialogPnl = new JPanel(new GridBagLayout());
        dialogPnl.setBorder(BorderFactory.createEmptyBorder());

        // Create the table to display the structure tables and their
        // corresponding user-selected data fields
        dataFieldTable = new CcddJTableHandler()
        {
            /******************************************************************
             * Allow resizing of the any column unless it displays a check box
             *****************************************************************/
            @Override
            protected boolean isColumnResizable(int column)
            {
                return !checkBoxColumns.contains(column);
            }

            /******************************************************************
             * Allow multiple line display in all columns except those
             * displaying check boxes
             *****************************************************************/
            @Override
            protected boolean isColumnMultiLine(int column)
            {
                return !checkBoxColumns.contains(column);
            }

            /******************************************************************
             * Allow editing of the table cells in the specified columns only
             *****************************************************************/
            @Override
            public boolean isCellEditable(int row, int column)
            {
                // Convert the coordinates from view to model
                row = convertRowIndexToModel(row);
                column = convertColumnIndexToModel(column);

                // Return true if this is not the owner or path column, or if
                // the table does not have the field specified by the column
                return column != FieldTableEditorColumnInfo.OWNER.ordinal()
                       && column != FieldTableEditorColumnInfo.PATH.ordinal()
                       && fieldHandler.getFieldInformationByName(getOwnerWithPath(getModel().getValueAt(row,
                                                                                                        FieldTableEditorColumnInfo.OWNER.ordinal()).toString(),
                                                                                  getModel().getValueAt(row,
                                                                                                        FieldTableEditorColumnInfo.PATH.ordinal()).toString()),
                                                                 columnNames[column]) != null;
            }

            /******************************************************************
             * Allow pasting data into the data field cells
             *****************************************************************/
            @Override
            protected boolean isDataAlterable(Object[] rowData,
                                              int row,
                                              int column)
            {
                return isCellEditable(convertRowIndexToView(row),
                                      convertColumnIndexToView(column));
            }

            /******************************************************************
             * Validate changes to the data field value cells; e.g., verify
             * cell content and, if found invalid, revert to the original value
             * 
             * @param tableData
             *            list containing the table data row arrays
             * 
             * @param row
             *            table model row number
             * 
             * @param column
             *            table model column number
             * 
             * @param oldValue
             *            original cell contents
             * 
             * @param newValue
             *            new cell contents
             * 
             * @param showMessage
             *            unused
             * 
             * @param isMultiple
             *            unused
             * 
             * @return Value of ShowMessage
             ****************************************************************/
            @Override
            protected Boolean validateCellContent(List<Object[]> tableData,
                                                  int row,
                                                  int column,
                                                  Object oldValue,
                                                  Object newValue,
                                                  Boolean showMessage,
                                                  boolean isMultiple)
            {
                // Reset the flag that indicates the last edited cell's content
                // is invalid
                setLastCellValid(true);

                // Create a string version of the new value
                String newValueS = newValue.toString();

                // Check that the new cell value isn't blank
                if (!newValueS.isEmpty())
                {
                    // Get the owner name, with path if applicable
                    String ownerAndPath = getOwnerWithPath(tableData.get(row)[FieldTableEditorColumnInfo.OWNER.ordinal()].toString(),
                                                           tableData.get(row)[FieldTableEditorColumnInfo.PATH.ordinal()].toString());

                    // Get the reference to the data field
                    FieldInformation fieldInfo = fieldHandler.getFieldInformationByName(ownerAndPath,
                                                                                        columnNames[column]);

                    // Check that the data field was found
                    if (fieldInfo != null)
                    {
                        // Check if the value entered doesn't match the pattern
                        // selected by the user for this data field
                        if (!fieldInfo.getInputType().getInputMatch().isEmpty()
                            && !newValueS.matches(fieldInfo.getInputType().getInputMatch()))
                        {
                            // Set the flag that indicates the last edited
                            // cell's content is invalid
                            setLastCellValid(false);

                            // Inform the user that the data field contents is
                            // invalid
                            new CcddDialogHandler().showMessageDialog(CcddFieldTableEditorDialog.this,
                                                                      "<html><b>Invalid characters in field '</b>"
                                                                          + fieldInfo.getFieldName()
                                                                          + "<b>'; "
                                                                          + fieldInfo.getInputType().getInputName().toLowerCase()
                                                                          + " expected",
                                                                      "Invalid "
                                                                          + fieldInfo.getInputType().getInputName(),
                                                                      JOptionPane.WARNING_MESSAGE,
                                                                      DialogOption.OK_OPTION);

                            // Restore the cell contents to its original value
                            tableData.get(row)[column] = oldValue;
                            getUndoManager().undoRemoveEdit();
                        }
                    }
                }

                return showMessage;
            }

            /******************************************************************
             * Load the data field data into the table and format the table
             * cells
             *****************************************************************/
            @Override
            protected void loadAndFormatData()
            {
                // Get the default column names and tool tip text for the
                // data field editor table
                String[] toolTips = new String[columnNames.length];
                toolTips[FieldTableEditorColumnInfo.OWNER.ordinal()] = FieldTableEditorColumnInfo.OWNER.getToolTip();
                toolTips[FieldTableEditorColumnInfo.PATH.ordinal()] = FieldTableEditorColumnInfo.PATH.getToolTip();

                // Create lists for any columns to be hidden and to be
                // displayed as check boxes
                List<Integer> hiddenColumns = new ArrayList<Integer>();
                checkBoxColumns = new ArrayList<Integer>();

                // Get the owners, paths, data field values values, and check
                // box columns based on the specified data fields
                committedData = getDataFieldsToDisplay();

                // Check if none of the tables to display have paths
                if (!isPath)
                {
                    // Hide the structure path column
                    hiddenColumns.add(FieldTableEditorColumnInfo.PATH.ordinal());
                }

                // Place the data into the table model along with the column
                // names, set up the editors and renderers for the table cells,
                // set up the table grid lines, and calculate the minimum width
                // required to display the table information
                setUpdatableCharacteristics(committedData,
                                            columnNames,
                                            null,
                                            hiddenColumns.toArray(new Integer[0]),
                                            checkBoxColumns.toArray(new Integer[0]),
                                            toolTips,
                                            true,
                                            true,
                                            true,
                                            true);
            }

            /******************************************************************
             * Override prepareRenderer to allow adjusting the background
             * colors of table cells
             *****************************************************************/
            @Override
            public Component prepareRenderer(TableCellRenderer renderer,
                                             int row,
                                             int column)
            {
                JComponent comp = (JComponent) super.prepareRenderer(renderer,
                                                                     row,
                                                                     column);

                // Get the column index in model coordinates
                int columnModel = convertColumnIndexToModel(column);

                // Check if the cell doesn't have the focus or is selected. The
                // focus and selection highlight colors override the invalid
                // highlight color
                if (comp.getBackground() != FOCUS_COLOR
                    && comp.getBackground() != SELECTED_BACK_COLOR
                    && columnModel != FieldTableEditorColumnInfo.OWNER.ordinal())
                {
                    // Get the row index in model coordinates
                    int rowModel = convertRowIndexToModel(row);

                    // Get a reference to the table model to shorten subsequent
                    // calls
                    TableModel tableModel = getModel();

                    // Get the contents of the owner and path columns
                    String ownerValue = tableModel.getValueAt(rowModel,
                                                              FieldTableEditorColumnInfo.OWNER.ordinal()).toString().trim();
                    String pathValue = tableModel.getValueAt(rowModel,
                                                             FieldTableEditorColumnInfo.PATH.ordinal()).toString();

                    // Check if this is the structure path column
                    if (columnModel == FieldTableEditorColumnInfo.PATH.ordinal())
                    {
                        // Check if the cell is blank, and that the owner is
                        // not a structure table or a group
                        if (pathValue.isEmpty()
                            && (nonStructureTableNames.contains(ownerValue)
                            || ownerValue.startsWith(CcddFieldHandler.getFieldGroupName(""))))
                        {
                            // Set the cell's background color to indicate
                            // the structure path isn't applicable for this
                            // table
                            comp.setBackground(Color.LIGHT_GRAY);
                        }
                    }
                    // Check if this table has the data field identified by
                    // the column
                    else if (fieldHandler.getFieldInformationByName(getOwnerWithPath(ownerValue,
                                                                                     pathValue),
                                                                    columnNames[columnModel]) != null)
                    {
                        // Check if the cell is a data field selected for
                        // removal
                        if (selectedCells.contains(row, columnModel))
                        {
                            // Change the cell's colors to indicate the data
                            // field represented by the cell is selected for
                            // removal
                            comp.setForeground(Color.GRAY);
                            comp.setBackground(Color.RED);
                        }
                        // The cell isn't selected for removal
                        else
                        {

                            // Get the text in the cell
                            String value = tableModel.getValueAt(rowModel,
                                                                 columnModel).toString();

                            // Step through each row in the table
                            for (int checkRow = 0; checkRow < tableModel.getRowCount(); checkRow++)
                            {
                                // Check if this isn't the same row as the one
                                // being updated, that the cell isn't blank,
                                // and that the text matches that in another
                                // row of the same column
                                if (rowModel != checkRow
                                    && !value.isEmpty()
                                    && tableModel.getValueAt(checkRow,
                                                             columnModel).toString().equals(value))
                                {
                                    // Change the cell's background color to
                                    // indicate it has the same value as
                                    // another cell in the same column
                                    comp.setBackground(Color.YELLOW);
                                    break;
                                }
                            }
                        }
                    }
                    // The table indicated by this row does not have a data
                    // field as identified by the column
                    else
                    {
                        // Set the cell's background color to indicate the
                        // data field doesn't exist for this table
                        comp.setBackground(Color.LIGHT_GRAY);
                    }
                }

                return comp;
            }

            /******************************************************************
             * Handle a change to the table's content
             *****************************************************************/
            @Override
            protected void processTableContentChange()
            {
                // Add or remove the change indicator based on whether any
                // unstored changes exist
                setTitle(DIALOG_TITLE
                         + (dataFieldTable.isTableChanged(committedData)
                                                                        ? "*"
                                                                        : ""));
            }
        };

        // Place the table into a scroll pane
        JScrollPane scrollPane = new JScrollPane(dataFieldTable);

        // Set up the field table parameters
        dataFieldTable.setFixedCharacteristics(scrollPane,
                                               false,
                                               ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
                                               TableSelectionMode.SELECT_BY_CELL,
                                               true,
                                               TABLE_BACK_COLOR,
                                               true,
                                               true,
                                               LABEL_FONT_PLAIN,
                                               true);

        // Discard the edits created by adding the columns initially
        dataFieldTable.getUndoManager().discardAllEdits();

        // Define the panel to contain the table
        JPanel tablePnl = new JPanel();
        tablePnl.setLayout(new BoxLayout(tablePnl, BoxLayout.X_AXIS));
        tablePnl.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        tablePnl.add(scrollPane);

        // Add the table to the dialog
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        dialogPnl.add(tablePnl, gbc);

        // Create the button panel
        JPanel buttonPnl = new JPanel();

        // Define the buttons for the lower panel:
        // Select data fields button
        JButton btnSelect = CcddButtonPanelHandler.createButton("Select",
                                                                OK_ICON,
                                                                KeyEvent.VK_L,
                                                                "Select new data fields");

        // Add a listener for the Select button
        btnSelect.addActionListener(new ValidateCellActionListener(dataFieldTable)
        {
            /******************************************************************
             * Select the data fields and update the data field editor table
             *****************************************************************/
            @Override
            protected void performAction(ActionEvent ae)
            {
                // Allow the user to select new data fields. Confirm discarding
                // pending changes if any exist
                if ((!isFieldTableChanged()
                    || new CcddDialogHandler().showMessageDialog(CcddFieldTableEditorDialog.this,
                                                                 "<html><b>Discard changes?",
                                                                 "Discard Changes",
                                                                 JOptionPane.QUESTION_MESSAGE,
                                                                 DialogOption.OK_CANCEL_OPTION) == OK_BUTTON)
                    && selectDataFields())
                {
                    // Clear any removal selections
                    selectedCells.clear();

                    // Reload the data field table
                    dataFieldTable.loadAndFormatData();

                    // Check if the field table editor dialog is already open
                    if (CcddFieldTableEditorDialog.this.isVisible())
                    {
                        // Reposition the field table editor dialog
                        positionFieldEditorDialog();
                    }
                }
            }
        });

        // Delete data fields button
        JButton btnRemove = CcddButtonPanelHandler.createButton("Remove",
                                                                DELETE_ICON,
                                                                KeyEvent.VK_R,
                                                                "Remove the selected data field(s) from their table(s)");

        // Add a listener for the Remove button
        btnRemove.addActionListener(new ValidateCellActionListener(dataFieldTable)
        {
            /******************************************************************
             * Toggle the removal state of the selected data field(s)
             *****************************************************************/
            @Override
            protected void performAction(ActionEvent ae)
            {
                toggleRemoveFields();
            }
        });

        // Open tables button
        JButton btnOpen = CcddButtonPanelHandler.createButton("Open",
                                                              TABLE_ICON,
                                                              KeyEvent.VK_O,
                                                              "Open the table(s) associated with the selected data field(s)");

        // Add a listener for the Open button
        btnOpen.addActionListener(new ValidateCellActionListener(dataFieldTable)
        {
            /******************************************************************
             * Open the table(s) associated with the selected data field(s)
             *****************************************************************/
            @Override
            protected void performAction(ActionEvent ae)
            {
                openTables();
            }
        });

        // Print data field editor table button
        JButton btnPrint = CcddButtonPanelHandler.createButton("Print",
                                                               PRINT_ICON,
                                                               KeyEvent.VK_P,
                                                               "Print the data field editor table");

        // Add a listener for the Print button
        btnPrint.addActionListener(new ValidateCellActionListener(dataFieldTable)
        {
            /******************************************************************
             * Print the data field editor table
             *****************************************************************/
            @Override
            protected void performAction(ActionEvent ae)
            {
                dataFieldTable.printTable("Data Field Contents",
                                          null,
                                          CcddFieldTableEditorDialog.this,
                                          PageFormat.LANDSCAPE);
            }
        });

        // Undo button
        JButton btnUndo = CcddButtonPanelHandler.createButton("Undo",
                                                              UNDO_ICON,
                                                              KeyEvent.VK_Z,
                                                              "Undo the last edit action");

        // Create a listener for the Undo command
        btnUndo.addActionListener(new ValidateCellActionListener(dataFieldTable)
        {
            /******************************************************************
             * Undo the last cell edit
             *****************************************************************/
            @Override
            protected void performAction(ActionEvent ae)
            {
                dataFieldTable.getUndoManager().undo();
            }
        });

        // Redo button
        JButton btnRedo = CcddButtonPanelHandler.createButton("Redo",
                                                              REDO_ICON,
                                                              KeyEvent.VK_Y,
                                                              "Redo the last undone edit action");

        // Create a listener for the Redo command
        btnRedo.addActionListener(new ValidateCellActionListener(dataFieldTable)
        {
            /******************************************************************
             * Redo the last cell edit that was undone
             *****************************************************************/
            @Override
            protected void performAction(ActionEvent ae)
            {
                dataFieldTable.getUndoManager().redo();
            }
        });

        // Define the buttons for the lower panel:
        // Store data field values button
        JButton btnStore = CcddButtonPanelHandler.createButton("Store",
                                                               STORE_ICON,
                                                               KeyEvent.VK_S,
                                                               "Store data field changes");

        // Add a listener for the Store button
        btnStore.addActionListener(new ValidateCellActionListener(dataFieldTable)
        {
            /******************************************************************
             * Store changes to the data field values
             *****************************************************************/
            @Override
            protected void performAction(ActionEvent ae)
            {
                // Only update the table in the database if a cell's content
                // has changed and the user confirms the action
                if (isFieldTableChanged()
                    && new CcddDialogHandler().showMessageDialog(CcddFieldTableEditorDialog.this,
                                                                 "<html><b>Store changes in database?",
                                                                 "Store Changes",
                                                                 JOptionPane.QUESTION_MESSAGE,
                                                                 DialogOption.OK_CANCEL_OPTION) == OK_BUTTON)
                {
                    // Build the update lists
                    buildUpdates();

                    // Store the changes to the data fields in the database
                    dbTable.modifyDataFields(fieldModifications,
                                             fieldDeletions,
                                             CcddFieldTableEditorDialog.this);
                }
            }
        });

        // Close button
        JButton btnClose = CcddButtonPanelHandler.createButton("Close",
                                                               CLOSE_ICON,
                                                               KeyEvent.VK_C,
                                                               "Close the data field editor dialog");

        // Add a listener for the Close button
        btnClose.addActionListener(new ValidateCellActionListener(dataFieldTable)
        {
            /******************************************************************
             * Close the data field editor dialog
             *****************************************************************/
            @Override
            protected void performAction(ActionEvent ae)
            {
                windowCloseButtonAction();
            }
        });

        // Add the buttons to the panel
        buttonPnl.add(btnSelect);
        buttonPnl.add(btnOpen);
        buttonPnl.add(btnUndo);
        buttonPnl.add(btnStore);
        buttonPnl.add(btnRemove);
        buttonPnl.add(btnPrint);
        buttonPnl.add(btnRedo);
        buttonPnl.add(btnClose);

        // Distribute the buttons across two rows
        setButtonRows(2);

        // Display the data field editor table dialog
        createFrame(ccddMain.getMainFrame(),
                    dialogPnl,
                    buttonPnl,
                    DIALOG_TITLE,
                    null);

        // Reposition the field table editor dialog
        positionFieldEditorDialog();
    }

    /**************************************************************************
     * Handle the frame close button press event
     *************************************************************************/
    @Override
    protected void windowCloseButtonAction()
    {
        // Check if the contents of the last cell edited in the editor table is
        // validated and that the table has no uncommitted changes. If changes
        // exist then confirm discarding the changes
        if (dataFieldTable.isLastCellValid()
            && (!isFieldTableChanged()
            || new CcddDialogHandler().showMessageDialog(CcddFieldTableEditorDialog.this,
                                                         "<html><b>Discard changes?",
                                                         "Discard Changes",
                                                         JOptionPane.QUESTION_MESSAGE,
                                                         DialogOption.OK_CANCEL_OPTION) == OK_BUTTON))
        {
            // Close the dialog
            closeFrame();
        }
    }

    /**************************************************************************
     * Reposition the field table editor dialog relative to the location of the
     * (now invisible) field selection dialog
     *************************************************************************/
    private void positionFieldEditorDialog()
    {
        // Get the location of the field selection dialog
        Point p1 = selectDlg.getLocation();

        // Get the sizes of the editor and selection dialogs
        Dimension d1 = selectDlg.getSize();
        Dimension d2 = CcddFieldTableEditorDialog.this.getSize();

        // Move the editor dialog so that it's centered over the selection
        // dialog's last location
        CcddFieldTableEditorDialog.this.setLocation(new Point(p1.x + d1.width / 2 - d2.width / 2,
                                                              p1.y + d1.height / 2 - d2.height / 2));
    }

    /**************************************************************************
     * Toggle the removal state of the selected data field cells in the editor
     * table
     *************************************************************************/
    private void toggleRemoveFields()
    {
        // Step through each row in the table
        for (int row = 0; row < dataFieldTable.getRowCount(); row++)
        {
            // Step through each column in the table
            for (int column = 0; column < dataFieldTable.getModel().getColumnCount(); column++)
            {
                // Check if this is not the table name or path column
                if (column != FieldTableEditorColumnInfo.OWNER.ordinal()
                    && column != FieldTableEditorColumnInfo.PATH.ordinal())
                {
                    // Check if the cell at these coordinates is selected
                    if (dataFieldTable.isCellSelected(row,
                                                      dataFieldTable.convertColumnIndexToView(column)))
                    {
                        // Flag to indicate if a selection or deselection
                        // occurred
                        boolean isSelected;

                        // Check if the cell wasn't already selected
                        if (!selectedCells.contains(row, column))
                        {
                            // Flag the data field represented by these
                            // coordinates for removal
                            selectedCells.add(row, column);

                            isSelected = true;
                        }
                        // The cell was already selected
                        else
                        {
                            // Remove the data field represented by these
                            // coordinates from the list
                            selectedCells.remove(row, column);

                            isSelected = false;
                        }

                        // Update the undo manager with this event. Get the
                        // listeners for this event
                        UndoableEditListener listeners[] = getListeners(UndoableEditListener.class);

                        // Check if there is an edit listener registered
                        if (listeners != null)
                        {
                            // Create the edit event to be passed to the
                            // listeners
                            UndoableEditEvent editEvent = new UndoableEditEvent(this,
                                                                                new CellSelectEdit(row,
                                                                                                   column,
                                                                                                   isSelected));

                            // Step through the registered listeners
                            for (UndoableEditListener listener : listeners)
                            {
                                // Inform the listener that an update occurred
                                listener.undoableEditHappened(editEvent);
                            }
                        }
                    }
                }
            }
        }

        // End the editing sequence
        dataFieldTable.getUndoManager().endEditSequence();

        // Clear the cell selection so that the remove highlight is visible
        dataFieldTable.clearSelection();
    }

    /**************************************************************************
     * Open the table(s) associated with the selected data fields
     *************************************************************************/
    private void openTables()
    {
        List<String> tablePaths = new ArrayList<String>();

        // Step through each row in the table
        for (int row = 0; row < dataFieldTable.getRowCount(); row++)
        {
            // Step through each column in the table
            for (int column = 0; column < dataFieldTable.getColumnCount(); column++)
            {
                // Get the owner for this row
                String owner = dataFieldTable.getModel().getValueAt(row,
                                                                    FieldTableEditorColumnInfo.OWNER.ordinal()).toString().trim();

                // Check if the cell at these coordinates is selected and that
                // the data field for this row belongs to a table (versus a
                // group or type)
                if (dataFieldTable.isCellSelected(row, column)
                    && !owner.contains(":"))
                {
                    // Get the structure path for this row
                    String path = dataFieldTable.getModel().getValueAt(row,
                                                                       FieldTableEditorColumnInfo.PATH.ordinal()).toString();

                    // Add the table path to the list and stop checking the
                    // columns in this row
                    tablePaths.add(getOwnerWithPath(owner, path));
                    break;
                }
            }
        }

        // Check if any table/field is selected
        if (!tablePaths.isEmpty())
        {
            // Load the selected table's data into a table editor
            dbTable.loadTableDataInBackground(tablePaths.toArray(new String[0]),
                                              null);
        }
    }

    /**************************************************************************
     * Build the data field array
     * 
     * @return Array containing the data field owner names and corresponding
     *         user-selected data field values
     *************************************************************************/
    private Object[][] getDataFieldsToDisplay()
    {
        isPath = false;
        List<Object[]> ownerDataFields = new ArrayList<Object[]>();
        List<String> filterTables = new ArrayList<String>();

        // Get the paths to the tables selected in the table tree
        TreePath[] treePaths = tableTree.getSelectionPaths();

        // Check if a path is selected in the tree
        if (treePaths != null)
        {
            // Step through each selected table in the tree
            for (TreePath treePath : treePaths)
            {
                // Add the full table path, minus the data types, to the list
                filterTables.add(tableTree.getFullVariablePath(treePath.getPath()));
            }
        }

        // Create a field handler and populate it with the field definitions
        // for all of the tables and groups in the database
        fieldHandler = new CcddFieldHandler();
        fieldHandler.buildFieldInformation(dataFields.toArray(new String[0][0]),
                                           null);
        List<FieldInformation> fieldInformation = fieldHandler.getFieldInformation();

        // Sort the field information by owner name so that sequence order of
        // the data field values is based on the owners' alphabetical order
        Collections.sort(fieldInformation, new Comparator<FieldInformation>()
        {
            /******************************************************************
             * Compare the owner names of two field definitions. Force lower
             * case to eliminate case differences in the comparison
             *****************************************************************/
            @Override
            public int compare(FieldInformation fld1, FieldInformation fld2)
            {
                return fld1.getOwnerName().toLowerCase().compareTo(fld2.getOwnerName().toLowerCase());
            }
        });

        // Step through each defined data field
        for (int index = 0; index < fieldInformation.size(); index++)
        {
            // Get the reference to the field information
            FieldInformation fieldInfo = fieldInformation.get(index);

            // Get the data field owner's name
            String ownerName = fieldInfo.getOwnerName();

            // Check that this is not a default table type field and if the
            // field is in a table selected by the user (if no table is
            // selected then all tables are considered to match)
            if (!ownerName.startsWith(CcddFieldHandler.getFieldTypeName(""))
                && (filterTables.isEmpty()
                || filterTables.contains(ownerName)))
            {
                String pathName = "";

                // Get the index of the last comma in the field table path &
                // name
                int commaIndex = ownerName.lastIndexOf(",");

                // Check if a comma was found in the table path & name
                if (commaIndex != -1)
                {
                    // Extract the path name from the table path and name
                    pathName = ownerName.substring(0, commaIndex);

                    // Count the number of commas in the path name, which
                    // indicates the structure nest level
                    int depth = pathName.split(",").length;

                    // Set the indentation
                    String indent = "";

                    // Step through each nest level
                    for (int count = 0; count < depth; count++)
                    {
                        // Add spaces to the indentation. This aids in
                        // identifying the structure members
                        indent += "  ";
                    }

                    // Remove the path and leave only the table name
                    ownerName = indent + ownerName.substring(commaIndex + 1);

                    // Add spaces after any remaining commas in the path
                    pathName = pathName.replaceAll(",", ", ");
                }

                int dataFieldIndex = -1;

                // Step through each column
                for (int fieldIndex = 0; fieldIndex < columnNames.length; fieldIndex++)
                {
                    // Check if the column name matches the data field name
                    if (fieldInfo.getFieldName().equals(columnNames[fieldIndex]))
                    {
                        // Set the index to the matching data field column
                        dataFieldIndex = fieldIndex;

                        // Check if the data field input type is boolean and
                        // hasn't already been added to the list
                        if (fieldInfo.getInputType() == InputDataType.BOOLEAN
                            && !checkBoxColumns.contains(fieldIndex))
                        {
                            // Store the column index in the check box column
                            // list
                            checkBoxColumns.add(fieldIndex);
                        }

                        break;
                    }
                }

                // Check if a target data field is present
                if (dataFieldIndex != -1)
                {
                    boolean isFound = false;

                    // Step through the owners added to this point
                    for (Object[] ownerDataFld : ownerDataFields)
                    {
                        // Check if the owner name and path for the data field
                        // matches
                        if (ownerDataFld[FieldTableEditorColumnInfo.OWNER.ordinal()].equals(ownerName)
                            && ownerDataFld[FieldTableEditorColumnInfo.PATH.ordinal()].equals(pathName))
                        {
                            // Store the data field value in the existing list
                            // item and stop searching
                            ownerDataFld[dataFieldIndex] = fieldInfo.getInputType() == InputDataType.BOOLEAN
                                                                                                            ? Boolean.valueOf(fieldInfo.getValue())
                                                                                                            : fieldInfo.getValue();
                            isFound = true;
                            break;
                        }
                    }

                    // Check if the owner isn't already in the list
                    if (!isFound)
                    {
                        // Create a new row for the owner
                        Object[] newTable = new Object[columnNames.length];
                        Arrays.fill(newTable, "");

                        // Insert the owner name, path, and the data field
                        // value into the new row
                        newTable[FieldTableEditorColumnInfo.OWNER.ordinal()] = ownerName;
                        newTable[FieldTableEditorColumnInfo.PATH.ordinal()] = pathName;
                        newTable[dataFieldIndex] = fieldInfo.getInputType() == InputDataType.BOOLEAN
                                                                                                    ? Boolean.valueOf(fieldInfo.getValue())
                                                                                                    : fieldInfo.getValue();

                        // Add the new row to the list
                        ownerDataFields.add(newTable);

                        // Check if this owner has a path (ie.e, it's a
                        // structure table)
                        if (!pathName.isEmpty())
                        {
                            // Set the flag to indicate at least one of the
                            // owners has a path
                            isPath = true;
                        }
                    }
                }
            }
        }

        // Since all of the check box columns are now determined, step through
        // each of them so that any that have a blank value can be set to false
        // (a legal boolean value)
        for (Integer c : checkBoxColumns)
        {
            // Set through each row in the table
            for (Object[] ownerDataField : ownerDataFields)
            {
                // Check if the check box value is blank
                if (ownerDataField[c] == "")
                {
                    // Set the check box value to false
                    ownerDataField[c] = false;
                }
            }
        }

        return ownerDataFields.toArray(new Object[0][0]);
    }

    /**************************************************************************
     * Compare the current table data to the committed table data and create
     * lists of the changed values necessary to update the table data fields in
     * the database to match the current values
     *************************************************************************/
    private void buildUpdates()
    {
        // Get the table data array
        Object[][] tableData = dataFieldTable.getTableData(true);

        // Remove existing changes, if any
        fieldModifications.clear();

        // Step through each row of the current data
        for (int row = 0; row < tableData.length; row++)
        {
            // Step through each row of the current data
            for (int column = 0; column < tableData[row].length; column++)
            {
                // Check that this isn't the table owner or path columns
                if (column != FieldTableEditorColumnInfo.OWNER.ordinal()
                    && column != FieldTableEditorColumnInfo.PATH.ordinal())
                {
                    // Get the table name, with path if applicable
                    String tableAndPath = getOwnerWithPath(tableData[row][FieldTableEditorColumnInfo.OWNER.ordinal()].toString(),
                                                           tableData[row][FieldTableEditorColumnInfo.PATH.ordinal()].toString());

                    // Check if this field is selected for removal
                    if (selectedCells.contains(row, column))
                    {
                        // Add the field removal information to the list
                        fieldDeletions.add(new String[] {tableAndPath,
                                                         dataFieldTable.getModel().getColumnName(column)});
                    }
                    // Check if the current and committed column values differ
                    else if (!tableData[row][column].equals(committedData[row][column]))
                    {
                        // Add the data field modification information to the
                        // list
                        fieldModifications.add(new String[] {tableAndPath,
                                                             columnNames[column],
                                                             tableData[row][column].toString()});
                    }
                }
            }
        }
    }

    /**************************************************************************
     * Verify input fields
     * 
     * @return Always return false so that the dialog doesn't close
     *************************************************************************/
    @Override
    protected boolean verifySelection()
    {
        return false;
    }

    /**************************************************************************
     * Display a dialog for the user to select the data fields to display in
     * the editor table
     * 
     * @return true if the user presses the selection dialog Okay button with
     *         at least one field selected; false otherwise
     *************************************************************************/
    private boolean selectDataFields()
    {
        boolean isSelected = false;

        // Create an empty border to surround the panels
        Border emptyBorder = BorderFactory.createEmptyBorder();

        // Set the initial layout manager characteristics
        GridBagConstraints gbc = new GridBagConstraints(0,
                                                        1,
                                                        1,
                                                        1,
                                                        1.0,
                                                        0.0,
                                                        GridBagConstraints.LINE_START,
                                                        GridBagConstraints.BOTH,
                                                        new Insets(LABEL_VERTICAL_SPACING / 2,
                                                                   LABEL_HORIZONTAL_SPACING,
                                                                   0,
                                                                   0),
                                                        0,
                                                        0);

        // Create a selection dialog, a panel for the table selection tree, and
        // a panel to contain a check box for each unique data field name
        selectDlg = new CcddDialogHandler();
        JPanel selectPnl = new JPanel(new GridBagLayout());
        JPanel fieldPnl = new JPanel(new GridBagLayout());
        selectPnl.setBorder(emptyBorder);
        fieldPnl.setBorder(emptyBorder);

        // Create a panel containing a grid of check boxes representing the
        // data fields from which to choose
        if (selectDlg.addCheckBoxes(null,
                                    getDataFieldNames(),
                                    null,
                                    "Select data fields to display/edit",
                                    fieldPnl))
        {
            // Check if more than one data field name check box exists
            if (selectDlg.getCheckBoxes().length > 2)
            {
                // Create a Select All check box
                final JCheckBox selectAllCb = new JCheckBox("Select all data fields",
                                                            false);
                selectAllCb.setFont(LABEL_FONT_BOLD);
                selectAllCb.setBorder(emptyBorder);

                // Create a listener for changes to the Select All check box
                // selection status
                selectAllCb.addActionListener(new ActionListener()
                {
                    /**********************************************************
                     * Handle a change to the Select All check box selection
                     * status
                     *********************************************************/
                    @Override
                    public void actionPerformed(ActionEvent ae)
                    {
                        // Step through each data field name check box
                        for (JCheckBox fieldCb : selectDlg.getCheckBoxes())
                        {
                            // Set the check box selection status to match the
                            // Select All check box selection status
                            fieldCb.setSelected(selectAllCb.isSelected());
                        }
                    }
                });

                // Add the Select All checkbox to the field name panel
                gbc.gridy++;
                fieldPnl.add(selectAllCb, gbc);
            }

            // Check if data fields are selected already (i.e., this method is
            // called via the Select button)
            if (columnNames != null)
            {
                // Step through the list of check boxes representing the data
                // fields
                for (JCheckBox cb : selectDlg.getCheckBoxes())
                {
                    // Select the data field check box if the field was
                    // displayed when the Select button was pressed
                    cb.setSelected(Arrays.asList(columnNames).contains(cb.getText()));
                }
            }

            // Build the table tree showing both table prototypes and table
            // instances; i.e., parent tables with their child tables (i.e.,
            // parents with children)
            tableTree = new CcddTableTreeHandler(ccddMain,
                                                 new CcddGroupHandler(ccddMain,
                                                                      CcddFieldTableEditorDialog.this),
                                                 TableTreeType.PROTOTYPE_AND_INSTANCE,
                                                 true,
                                                 false,
                                                 CcddFieldTableEditorDialog.this)
            {
                /**************************************************************
                 * Respond to changes in selection of a node in the table tree
                 *************************************************************/
                @Override
                protected void updateTableSelection()
                {
                    // Check that a node selection change is not in progress
                    if (!isNodeSelectionChanging)
                    {
                        // Set the flag to prevent variable tree updates
                        isNodeSelectionChanging = true;

                        // Deselect any nodes that don't represent a table or
                        // the level immediately above the table level
                        clearNonTableNodes(1);

                        // Reset the flag to allow variable tree updates
                        isNodeSelectionChanging = false;
                    }
                }
            };

            // Add the tree to the selection panel
            gbc.insets.top = LABEL_VERTICAL_SPACING;
            gbc.weighty = 1.0;
            gbc.gridy = 0;
            selectPnl.add(tableTree.createTreePanel("Tables",
                                                    TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION,
                                                    ccddMain.getMainFrame()),
                          gbc);

            // Add the field panel to the selection panel
            gbc.insets.top = 0;
            gbc.insets.left = 0;
            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            gbc.gridx++;
            selectPnl.add(fieldPnl, gbc);

            // Display the data field selection dialog
            if (selectDlg.showOptionsDialog((isVisible()
                                                        ? CcddFieldTableEditorDialog.this
                                                        : ccddMain.getMainFrame()),
                                            selectPnl,
                                            "Select Data Field(s)",
                                            DialogOption.OK_CANCEL_OPTION,
                                            true) == OK_BUTTON)
            {
                // Check that at least one field check box is selected
                if (selectDlg.getCheckBoxSelected().length != 0)
                {
                    isSelected = true;

                    // Create a list for the column names. Add the default
                    // columns (table name and path)
                    List<String> columnNamesList = new ArrayList<String>();
                    columnNamesList.add(FieldTableEditorColumnInfo.OWNER.getColumnName());
                    columnNamesList.add(FieldTableEditorColumnInfo.PATH.getColumnName());

                    // Step through each selected data field name
                    for (String name : selectDlg.getCheckBoxSelected())
                    {
                        // Add the data field name to the column name list
                        columnNamesList.add(name);
                    }

                    // Convert the list of column names to an array
                    columnNames = columnNamesList.toArray(new String[0]);
                }
                // No check box is selected
                else
                {
                    // Inform the user that a field must be selected
                    new CcddDialogHandler().showMessageDialog((isVisible()
                                                                          ? CcddFieldTableEditorDialog.this
                                                                          : ccddMain.getMainFrame()),
                                                              "<html><b>Must select at least one data field",
                                                              "No Data Field Selected",
                                                              JOptionPane.WARNING_MESSAGE,
                                                              DialogOption.OK_OPTION);
                }
            }
        }
        // No data field exists to choose
        else
        {
            // Inform the user that no data field is defined
            new CcddDialogHandler().showMessageDialog((isVisible()
                                                                  ? CcddFieldTableEditorDialog.this
                                                                  : ccddMain.getMainFrame()),
                                                      "<html><b>No data field exists",
                                                      "No Data Field",
                                                      JOptionPane.WARNING_MESSAGE,
                                                      DialogOption.OK_OPTION);
        }

        return isSelected;
    }

    /**************************************************************************
     * Get the unique project data field names in alphabetical order
     * 
     * @return Array containing the unique project data field names in
     *         alphabetical order
     *************************************************************************/
    private String[][] getDataFieldNames()
    {
        List<String[]> nameList = new ArrayList<String[]>();

        // Step through each database
        for (String[] dataField : dataFields)
        {
            // Check that this isn't a default data field
            if (!dataField[FieldsColumn.OWNER_NAME.ordinal()].startsWith(CcddFieldHandler.getFieldTypeName("")))
            {
                boolean found = false;

                // Step through the list of data fields in the list to this
                // point
                for (String[] item : nameList)
                {
                    // Check if the current data field's name is in the list
                    if (item[0].equals(dataField[FieldsColumn.FIELD_NAME.ordinal()]))
                    {
                        // Set the flag indicating the data field name is
                        // already in the list and stop searching
                        found = true;
                        break;
                    }
                }

                // Check if the data field name isn't in the list
                if (!found)
                {
                    // Add the data field name to the list. Set the description
                    // field to null
                    nameList.add(new String[] {dataField[FieldsColumn.FIELD_NAME.ordinal()],
                                               null});
                }
            }
        }

        // Convert the list of unique data field names into an array
        String[][] nameArray = nameList.toArray(new String[0][0]);

        // Sort the data field array based on the field name
        Arrays.sort(nameArray, new Comparator<String[]>()
        {
            /******************************************************************
             * Sort the data field array based on the field name (first column)
             *****************************************************************/
            @Override
            public int compare(final String[] entry1, final String[] entry2)
            {
                return entry1[0].compareTo(entry2[0]);
            }
        });

        return nameArray;
    }

    /**************************************************************************
     * Get the owner name with path, if applicable (child tables of a structure
     * table have a path)
     * 
     * @param ownerName
     *            table or group owner name
     * 
     * @param path
     *            table path; blank if none
     * 
     * @return Table or group name with path, if applicable
     *************************************************************************/
    private String getOwnerWithPath(String ownerName, String path)
    {
        // Remove and leading spaces used for indenting child structure names
        ownerName = ownerName.trim();

        // Check if the owner has a path
        if (!path.isEmpty())
        {
            // Prepend the path to the table name
            ownerName = path.replaceAll(" ", "") + "," + ownerName;
        }

        return ownerName;
    }

    /**************************************************************************
     * Determine if any unsaved changes exist in the data field table editor
     * 
     * @return true if changes exist that haven't been saved; false if there
     *         are no unsaved changes
     *************************************************************************/
    protected boolean isFieldTableChanged()
    {
        return !selectedCells.getSelectedCells().isEmpty()
               || dataFieldTable.isTableChanged(committedData);
    }

    /**************************************************************************
     * Cell selection edit event handler class. This handles undo and redo of
     * cell selection events related to removing data field represented by the
     * selected cell
     *************************************************************************/
    private class CellSelectEdit extends AbstractUndoableEdit
    {
        private final int row;
        private final int column;
        private final boolean isSelected;

        /**********************************************************************
         * Cell selection edit event handler constructor
         * 
         * @param row
         *            select cell row index
         * 
         * @param column
         *            select cell column index
         * 
         * @param isSelected
         *            true if the cell is selected
         *********************************************************************/
        private CellSelectEdit(int row,
                               int column,
                               boolean isSelected)
        {
            this.row = row;
            this.column = column;
            this.isSelected = isSelected;

            // Add the cell selection edit to the undo stack
            dataFieldTable.getUndoManager().addEditSequence(this);
        }

        /**********************************************************************
         * Replace the current cell selection state with the old state
         *********************************************************************/
        @Override
        public void undo() throws CannotUndoException
        {
            super.undo();

            // Select the cell where the change was undone
            setSelectedCell(!isSelected);
        }

        /**********************************************************************
         * Replace the current cell selection state with the new state
         *********************************************************************/
        @Override
        public void redo() throws CannotUndoException
        {
            super.redo();

            // Select the cell where the change was redone
            setSelectedCell(isSelected);
        }

        /**********************************************************************
         * Set the selected cell
         * 
         * @param selectState
         *            state to which the cell should be set; true to show the
         *            cell selected and false for deselected
         *********************************************************************/
        private void setSelectedCell(boolean selectState)
        {
            // Check if the cell wasn't already selected
            if (selectState)
            {
                // Flag the data field represented by these coordinates for
                // removal
                selectedCells.add(row, column);
            }
            // The cell was already selected
            else
            {
                // Remove the data field represented by these coordinates from
                // the list
                selectedCells.remove(row, column);
            }

            // Force the table to redraw so that the selection state is
            // displayed correctly
            dataFieldTable.repaint();
        }
    }
}
