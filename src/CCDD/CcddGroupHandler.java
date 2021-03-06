/**
 * CFS Command & Data Dictionary group handler. Copyright 2017 United States
 * Government as represented by the Administrator of the National Aeronautics
 * and Space Administration. No copyright is claimed in the United States under
 * Title 17, U.S. Code. All Other Rights Reserved.
 */
package CCDD;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import CCDD.CcddClasses.GroupInformation;
import CCDD.CcddConstants.InternalTable;
import CCDD.CcddConstants.InternalTable.GroupsColumn;

/******************************************************************************
 * CFS Command & Data Dictionary group handler class
 *****************************************************************************/
public class CcddGroupHandler
{
    private List<GroupInformation> groupInformation;

    /**************************************************************************
     * Group handler class constructor. Load and build the group information
     * class from the group definitions stored in the project database
     * 
     * @param ccddMain
     *            main class
     * 
     * @param component
     *            GUI component calling this method
     *************************************************************************/
    protected CcddGroupHandler(CcddMain ccddMain, Component component)
    {
        buildGroupInformation(ccddMain.getDbTableCommandHandler().retrieveInformationTable(InternalTable.GROUPS,
                                                                                           component));
    }

    /**************************************************************************
     * Group handler class constructor. Load and build the group information
     * class from the group definitions provided
     * 
     * @param groupInformation
     *            group information list
     *************************************************************************/
    protected CcddGroupHandler(List<String[]> groupDefinitions)
    {
        buildGroupInformation(groupDefinitions);
    }

    /**************************************************************************
     * Add a new group to the group information class
     * 
     * @param name
     *            group name
     * 
     * @param description
     *            group description
     * 
     * @param isApplication
     *            true if the group represents a CFS application
     *************************************************************************/
    protected void addGroupInformation(String name,
                                       String description,
                                       boolean isApplication)
    {
        groupInformation.add(new GroupInformation(name,
                                                  description,
                                                  isApplication,
                                                  null));
    }

    /**************************************************************************
     * Remove the specified group's information
     * 
     * @param groupName
     *            group name
     *************************************************************************/
    protected void removeGroupInformation(String groupName)
    {
        // Step through each group's information
        for (int index = 0; index < groupInformation.size(); index++)
        {
            // Check if the name matches the target name
            if (groupInformation.get(index).getName().equals(groupName))
            {
                // Remove the group's information and stop searching
                groupInformation.remove(index);
                break;
            }
        }
    }

    /**************************************************************************
     * Build the group information using the group definitions and the field
     * information in the database
     * 
     * @param groupDefinitions
     *            list of group definitions
     *************************************************************************/
    protected void buildGroupInformation(List<String[]> groupDefinitions)
    {
        groupInformation = new ArrayList<GroupInformation>();

        // Check if a group definition exists
        if (groupDefinitions != null)
        {
            // Step through the group definitions
            for (String[] groupDefn : groupDefinitions)
            {
                // Extract the link name and rate/description or member
                String groupName = groupDefn[GroupsColumn.GROUP_NAME.ordinal()];
                String groupMember = groupDefn[GroupsColumn.MEMBERS.ordinal()];

                // Check if this is a group definition entry. These are
                // indicated if the first character is a digit. A non-zero
                // value indicates that this group represents a CFS application
                if (groupMember.matches("\\d.*"))
                {
                    // Separate the CFS application identifier and description
                    // text
                    String[] appAndDesc = groupMember.split(",", 2);

                    // Create the group with its description and application
                    // status
                    groupInformation.add(new GroupInformation(groupName,
                                                              appAndDesc[1],
                                                              !appAndDesc[0].equals("0"),
                                                              null));
                }
                // This is a group's variable path
                else
                {
                    // Split the entry into the parent table and the path
                    String[] topAndPath = groupMember.toString().split(",", 2);

                    // Get the reference to this group's information
                    GroupInformation groupInfo = getGroupInformationByName(groupName);

                    // Check that the group exists
                    if (groupInfo != null)
                    {
                        // Check if this parent table has not been added to the
                        // group's table list
                        if (!groupInfo.getTables().contains(topAndPath[0]))
                        {
                            // Add the parent table to the group's table list
                            groupInfo.getTables().add(topAndPath[0]);
                        }

                        // Check if this parent table contains any paths
                        if (topAndPath.length > 1)
                        {
                            // Split the path into structure and variable pairs
                            String[] tables = topAndPath[1].split(",");

                            // Step through each table referenced in the path
                            for (int index = 1; index < tables.length; index++)
                            {
                                // Check if this table reference han't been
                                // added to the group's table list
                                if (!groupInfo.getTables().contains(tables[index]))
                                {
                                    // Add this table to the group's table list
                                    groupInfo.getTables().add(tables[index]);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**************************************************************************
     * Get the reference to a specified group's information from the supplied
     * list of group information
     * 
     * @param groupInformationList
     *            list of group information from which to extract the specific
     *            group's information
     * 
     * @param name
     *            group name
     * 
     * @return Reference to the group's information; null if the group doesn't
     *         exist
     *************************************************************************/
    protected GroupInformation getGroupInformationByName(List<GroupInformation> groupInformationList,
                                                         String name)
    {
        GroupInformation groupInfo = null;

        // Step through each group's information
        for (GroupInformation grpInfo : groupInformationList)
        {
            // Check if the group name matches the target name
            if (grpInfo.getName().equals(name))
            {
                // Store the group information reference and stop searching
                groupInfo = grpInfo;
                break;
            }
        }

        return groupInfo;
    }

    /**************************************************************************
     * Get the reference to a specified group's information
     * 
     * @param groupName
     *            group name
     * 
     * @return Reference to the group's information; null if the group doesn't
     *         exist
     *************************************************************************/
    protected GroupInformation getGroupInformationByName(String groupName)
    {
        return getGroupInformationByName(groupInformation, groupName);
    }

    /**************************************************************************
     * Get the group information list
     * 
     * @return Group information list
     *************************************************************************/
    protected List<GroupInformation> getGroupInformation()
    {
        // Sort the group information list based on the group names
        Collections.sort(groupInformation, new Comparator<Object>()
        {
            /******************************************************************
             * Compare group names
             * 
             * @param grpInfo1
             *            first group's information
             * 
             * @param grpInfo2
             *            second group's information
             * 
             * @return -1 if the first group's name is lexically less than the
             *         second group's name; 0 if the two group names are the
             *         same; 1 if the first group's name is lexically greater
             *         than the second group's name
             *****************************************************************/
            @Override
            public int compare(Object grpInfo1, Object grpInfo2)
            {
                return ((GroupInformation) grpInfo1).getName().compareTo(((GroupInformation) grpInfo2).getName());
            }
        });

        return groupInformation;
    }

    /**************************************************************************
     * Get an array containing the group names
     * 
     * @param applicationOnly
     *            true if only groups representing CFS applications should be
     *            returned
     * 
     * @return Array containing the group names
     *************************************************************************/
    protected String[] getGroupNames(boolean applicationOnly)
    {
        List<String> groupNames = new ArrayList<String>();

        // Step through each group
        for (GroupInformation groupInfo : groupInformation)
        {
            // Check if all groups are to be returned or if not, that this is
            // an application group
            if (!applicationOnly || groupInfo.isApplication())
            {
                // Add the group name to the list
                groupNames.add(groupInfo.getName());
            }
        }

        return groupNames.toArray(new String[0]);
    }

    /**************************************************************************
     * Get the description for the specified group
     * 
     * @param groupName
     *            group name
     * 
     * @return Description for the specified group; blank if the group has no
     *         description or the group doesn't exist
     *************************************************************************/
    protected String getGroupDescription(String groupName)
    {
        String description = "";

        // Get a reference to the group's information
        GroupInformation groupInfo = getGroupInformationByName(groupName);

        // Check if the group exists
        if (groupInfo != null)
        {
            // Get the group's description
            description = getGroupInformationByName(groupName).getDescription();
        }

        return description;
    }

    /**************************************************************************
     * Set the specified group's description
     *
     * @param groupName
     *            group name
     *
     * @param description
     *            group description
     *************************************************************************/
    protected void setDescription(String groupName, String description)
    {
        // Get a reference to the group's information
        GroupInformation groupInfo = getGroupInformationByName(groupName);

        // Check if the group exists
        if (groupInfo != null)
        {
            // Set the group description
            groupInfo.setDescription(description);
        }
    }

    /**************************************************************************
     * Set the specified group's CFS application status flag
     *
     * @param groupName
     *            group name
     *
     * @param isApplication
     *            true if the group represents a CFS application
     *************************************************************************/
    protected void setIsApplication(String groupName, boolean isApplication)
    {
        // Get a reference to the group's information
        GroupInformation groupInfo = getGroupInformationByName(groupName);

        // Check if the group exists
        if (groupInfo != null)
        {
            // Set the group application status flag
            groupInfo.setIsApplication(isApplication);
        }
    }
}
