/**
 * CFS Command & Data Dictionary variable assignment tree handler. Copyright
 * 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. No copyright is claimed in
 * the United States under Title 17, U.S. Code. All Other Rights Reserved.
 */
package CCDD;

import static CCDD.CcddConstants.LABEL_FONT_BOLD;
import static CCDD.CcddConstants.LABEL_FONT_PLAIN;
import static CCDD.CcddConstants.LABEL_HORIZONTAL_SPACING;
import static CCDD.CcddConstants.LABEL_VERTICAL_SPACING;
import static CCDD.CcddConstants.TLM_SCH_SEPARATOR;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import CCDD.CcddClasses.Message;
import CCDD.CcddClasses.ToolTipTreeNode;
import CCDD.CcddClasses.Variable;
import CCDD.CcddConstants.InternalTable;
import CCDD.CcddConstants.InternalTable.TlmSchedulerColumn;

/******************************************************************************
 * CFS Command & Data Dictionary assignment tree handler class
 *****************************************************************************/
@SuppressWarnings("serial")
public class CcddAssignmentTreeHandler extends CcddInformationTreeHandler
{
    // Class reference
    private final CcddLinkHandler linkHandler;

    // Rate column name currently in effect
    private String rateName;

    // Flag indicating if the assignment tree nodes are expanded or not
    private boolean isExpanded;

    // List to contain the assignment definitions retrieved from the database
    private List<String[]> assignDefinitions;

    // Flag to indicate if the assignment tree is being built
    private boolean isBuilding;

    // List containing the selected variable paths
    private List<Object[]> selectedVariablePaths;

    /**************************************************************************
     * Tree cell renderer with assignment display handling class
     *************************************************************************/
    private class AssignTreeCellRenderer extends DefaultTreeCellRenderer
    {
        /**********************************************************************
         * Use special icons to denote variables for the nodes
         *********************************************************************/
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus)
        {
            // Display the node name
            super.getTreeCellRendererComponent(tree,
                                               value,
                                               sel,
                                               expanded,
                                               leaf,
                                               row,
                                               hasFocus);

            // Check if this node represents a variable
            if (leaf)
            {
                // Set the icon for the variable node
                setVariableNodeIcon(this,
                                    (ToolTipTreeNode) value,
                                    row,
                                    linkHandler.getVariableLink(getFullVariablePath(((ToolTipTreeNode) value).getPath()),
                                                                rateName) != null);
            }

            return this;
        }
    }

    /**************************************************************************
     * Assignment tree handler class constructor
     * 
     * @param ccddMain
     *            main class
     * 
     * @param rateMsgFilter
     *            rate column name and message name, separated by a back slash
     * 
     * @param linkHandler
     *            reference to the link handler
     * 
     * @param parent
     *            GUI component calling this method
     *************************************************************************/
    CcddAssignmentTreeHandler(CcddMain ccddMain,
                              String rateMsgFilter,
                              CcddLinkHandler linkHandler,
                              Component parent)
    {
        super(ccddMain,
              InternalTable.TLM_SCHEDULER,
              rateMsgFilter, false,
              parent);

        this.linkHandler = linkHandler;
    }

    /**************************************************************************
     * Get the first node index that represents a table
     * 
     * @return First node index for a table
     *************************************************************************/
    @Override
    protected int getTableNodeLevel()
    {
        return 1;
    }

    /**************************************************************************
     * Perform initialization steps prior to building the assignment tree
     * 
     * @param ccddMain
     *            main class
     * 
     * @param assignDefinitions
     *            list containing the assignment definitions
     *************************************************************************/
    @Override
    protected void initialize(CcddMain ccddMain, List<String[]> assignDefinitions)
    {
        this.assignDefinitions = assignDefinitions;

        // Set the tree to be collapsed initially
        isExpanded = false;
    }

    /**************************************************************************
     * Build the assignment tree from the database. Retain the tree's current
     * expansion state
     * 
     * @param filterByType
     *            true if the tree is filtered by table type. This is not
     *            applicable to the assignment tree, which can only contain
     *            structure references
     * 
     * @param filterByApp
     *            true if the tree is filtered by application. This is not
     *            applicable to the assignment tree, which can only contain
     *            structure references
     * 
     * @param filterValue
     *            rate column name and message name, separated by a back slash
     * 
     * @param filterFlag
     *            flag used to filter the tree content. Not used for the
     *            assignment tree
     * 
     * @param parent
     *            GUI component calling this method
     *************************************************************************/
    @Override
    protected void buildTree(boolean filterByType,
                             boolean filterByApp,
                             String filterValue,
                             boolean filterFlag,
                             Component parent)
    {
        // Store the tree's current expansion state
        String expState = getExpansionState();

        super.buildTree(false, false, filterValue, filterFlag, parent);

        // Get the tree's root node
        ToolTipTreeNode root = getRootNode();

        // Register the tool tip manager for the assignment tree (otherwise the
        // tool tips aren't displayed)
        ToolTipManager.sharedInstance().registerComponent(this);

        // Set the flag to indicate that the assignment tree is being built.
        // This flag is used to inhibit actions involving tree selection value
        // changes during the build process
        isBuilding = true;

        // Set the renderer for the tree so that custom icons can be used for
        // the various node types
        setCellRenderer(new AssignTreeCellRenderer());

        // Check if a filter value is provided
        if (filterValue != null)
        {
            // Parent message name; remains blank if the definition is not a
            // sub-message
            String parentMessage = "";

            // Separate the rate and message name from the filter value
            String[] rateAndMessage = filterValue.split("\\" + TLM_SCH_SEPARATOR);

            // Store the rate
            rateName = rateAndMessage[0];

            // Get the sub-message separator index
            int subIndex = rateAndMessage[1].indexOf(".");

            // Check if this is a sub-message definition
            if (subIndex != -1)
            {
                // Extract the parent message name from the sub-message name
                parentMessage = rateAndMessage[1].substring(0, subIndex);
            }

            // Step through each assignment definition
            for (String[] assignDefn : assignDefinitions)
            {
                // Check if the assignment definition matches the target data
                // stream rate column name
                if (assignDefn[TlmSchedulerColumn.RATE_NAME.ordinal()].equals(rateAndMessage[0])
                    && !assignDefn[TlmSchedulerColumn.MEMBER.ordinal()].isEmpty()
                    && (assignDefn[TlmSchedulerColumn.MESSAGE_NAME.ordinal()].equals(rateAndMessage[1])
                    || assignDefn[TlmSchedulerColumn.MESSAGE_NAME.ordinal()].equals(parentMessage)))
                {
                    // Add the variable to the node
                    addNodeToInfoNode(root,
                                      assignDefn[TlmSchedulerColumn.MEMBER.ordinal()].split("\\"
                                                                                            + TLM_SCH_SEPARATOR,
                                                                                            2)[1].split(","),
                                      0);
                }
            }

            // Expand or collapse the tree based on the expansion flag
            setTreeExpansion(isExpanded);

            // Restore the expansion state
            setExpansionState(expState);
        }

        // Clear the flag that indicates the assignment tree is being built
        isBuilding = false;
    }

    /**************************************************************************
     * Update references to the specified message name with the new name. This
     * is necessary for the tree to be rebuilt following a message name change
     * 
     * @param oldName
     *            original message name
     * 
     * @param newName
     *            new message name
     *************************************************************************/
    protected void updateMessageName(String oldName, String newName)
    {
        // Step through each assignment definition
        for (String[] assignDefn : assignDefinitions)
        {
            // Check if the message names match
            if (assignDefn[1].equals(oldName))
            {
                // Change the message name from the old to the new
                assignDefn[1] = newName;
            }
        }
    }

    /**************************************************************************
     * Get the parent structure and variable path for the selected node(s)
     * 
     * @return List containing the full path array(s) for the selected
     *         variable(s)
     *************************************************************************/
    protected List<String> getSelectedVariables()
    {
        selectedVariablePaths = new ArrayList<Object[]>();

        // Check if at least one node is selected
        if (getSelectionCount() != 0)
        {
            // Step through each selected node
            for (TreePath path : getSelectionPaths())
            {
                // Check that this node represents a structure or variable, or
                // a header node one level above
                if (path.getPathCount() >= getTableNodeLevel())
                {
                    // Check if the selected variable node has children
                    addChildNodes((ToolTipTreeNode) path.getLastPathComponent(),
                                  selectedVariablePaths,
                                  new ArrayList<String>(),
                                  true);
                }
            }
        }

        List<String> selectedFullVariablePaths = new ArrayList<String>();

        // Step through the selected paths
        for (Object[] var : selectedVariablePaths)
        {
            // Add the variable's full path (with the root table) to the full
            // path list
            selectedFullVariablePaths.add(getFullVariablePath(var));
        }

        return selectedFullVariablePaths;
    }

    /**************************************************************************
     * Update the assignment definition list for the specified rate based on
     * the supplied message list
     * 
     * @param messages
     *            list of messages for the specified rate
     * 
     * @param rateName
     *            rate column name
     *************************************************************************/
    protected void updateAssignmentDefinitions(List<Message> messages,
                                               String rateName)
    {
        // Remove the existing definitions
        assignDefinitions.clear();

        // Step through each message
        for (Message message : messages)
        {
            // Step through each variable in the message
            for (Variable var : message.getVariablesWithParent())
            {
                // Add the variable to the assignment definition
                addAssignmentDefinition(message, rateName, var);
            }

            // Step through each sub-message
            for (Message subMessage : message.getSubMessages())
            {
                // Step through each variable in the sub-message
                for (Variable var : subMessage.getVariables())
                {
                    // Add the variable to the assignment definition
                    addAssignmentDefinition(subMessage, rateName, var);
                }
            }
        }
    }

    /**************************************************************************
     * Add the specified variable to the assignment definition for the
     * specified rate and message
     * 
     * @param message
     *            message for which the variable is a member
     * 
     * @param rateName
     *            rate column name
     * 
     * @param variable
     *            variable to add to the assignment definition
     *************************************************************************/
    private void addAssignmentDefinition(Message message,
                                         String rateName,
                                         Variable variable)
    {
        // Create a new array for the row
        String[] msg = new String[TlmSchedulerColumn.values().length];

        // Add the data stream, message name, message ID, and the rate
        // and variable name
        msg[TlmSchedulerColumn.RATE_NAME.ordinal()] = rateName;
        msg[TlmSchedulerColumn.MESSAGE_NAME.ordinal()] = message.getName();
        msg[TlmSchedulerColumn.MESSAGE_ID.ordinal()] = message.getID();
        msg[TlmSchedulerColumn.MEMBER.ordinal()] = variable.getRate()
                                                   + TLM_SCH_SEPARATOR
                                                   + variable.getFullName().trim();

        // Add the variable assignment to the list of definitions
        assignDefinitions.add(msg);
    }

    /**************************************************************************
     * Create an assignment tree panel. The table tree is placed in a scroll
     * pane. A check box is added that allows tree expansion/collapse
     * 
     * @param label
     *            assignment tree title
     * 
     * @param selectionMode
     *            tree item selection mode (single versus multiple)
     * 
     * @return JPanel containing the assignment tree components
     *************************************************************************/
    protected JPanel createTreePanel(int selectionMode)
    {
        // Set the initial layout manager characteristics
        GridBagConstraints gbc = new GridBagConstraints(0,
                                                        0,
                                                        1,
                                                        1,
                                                        1.0,
                                                        1.0,
                                                        GridBagConstraints.LINE_START,
                                                        GridBagConstraints.BOTH,
                                                        new Insets(LABEL_VERTICAL_SPACING / 2,
                                                                   LABEL_HORIZONTAL_SPACING / 2,
                                                                   LABEL_VERTICAL_SPACING / 2,
                                                                   LABEL_HORIZONTAL_SPACING / 2),
                                                        0,
                                                        0);

        // Set the table tree font and number of rows to display
        setFont(LABEL_FONT_PLAIN);
        setVisibleRowCount(10);

        // Set the table tree selection mode
        getSelectionModel().setSelectionMode(selectionMode);

        // Create a panel to contain the table tree
        JPanel treePnl = new JPanel(new GridBagLayout());
        treePnl.setBorder(BorderFactory.createEmptyBorder());

        // Create the tree scroll pane
        JScrollPane treeScroll = new JScrollPane(this);
        treeScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,
                                                                                                Color.LIGHT_GRAY,
                                                                                                Color.GRAY),
                                                                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        // Set the preferred width of the tree's scroll pane
        treeScroll.setPreferredSize(new Dimension(Math.min(Math.max(treeScroll.getPreferredSize().width,
                                                                    200),
                                                           400),
                                                  treeScroll.getPreferredSize().height));
        treeScroll.setMinimumSize(treeScroll.getPreferredSize());

        // Add the tree to the panel
        treePnl.add(treeScroll, gbc);

        // Add a listener for changes to the assignment tree
        addTreeSelectionListener(new TreeSelectionListener()
        {
            /******************************************************************
             * Handle a change to the assignment tree selection
             *****************************************************************/
            @Override
            public void valueChanged(TreeSelectionEvent lse)
            {
                // Check that a assignment tree (re)build isn't in progress.
                // Building the tree triggers tree selection value changes that
                // should not be processed
                if (!isBuilding)
                {
                    // Update the assignment dialog based on the assignment(s)
                    // selected
                    updateTableSelection();
                }
            }
        });

        // Create a tree expansion check box
        final JCheckBox expandChkBx = new JCheckBox("Expand all");
        expandChkBx.setBorder(BorderFactory.createEmptyBorder());
        expandChkBx.setFont(LABEL_FONT_BOLD);
        expandChkBx.setSelected(false);
        gbc.weighty = 0.0;
        gbc.gridy++;
        treePnl.add(expandChkBx, gbc);

        // Create a listener for changes in selection of the tree expansion
        // check box
        expandChkBx.addActionListener(new ActionListener()
        {
            /******************************************************************
             * Handle a change to the tree expansion check box selection
             *****************************************************************/
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                // Set the flag indicating if the tree is fully expanded
                isExpanded = expandChkBx.isSelected();

                // Set the tree expansion based on the check box status
                setTreeExpansion(isExpanded);
            }
        });

        return treePnl;
    }

    /**************************************************************************
     * Placeholder - required by information tree but unused in assignment tree
     *************************************************************************/
    @Override
    protected List<String[]> createDefinitionsFromInformation()
    {
        return null;
    }

    /**************************************************************************
     * Placeholder - required by information tree but unused in assignment tree
     *************************************************************************/
    @Override
    protected void addInformation(Object information, String nameOfCopy)
    {
    }
}
