/**
 * CFS Command & Data Dictionary field handler. Copyright 2017 United States
 * Government as represented by the Administrator of the National Aeronautics
 * and Space Administration. No copyright is claimed in the United States under
 * Title 17, U.S. Code. All Other Rights Reserved.
 */
package CCDD;

import static CCDD.CcddConstants.GROUP_DATA_FIELD_IDENT;
import static CCDD.CcddConstants.TYPE_DATA_FIELD_IDENT;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;

import CCDD.CcddClasses.FieldInformation;
import CCDD.CcddConstants.FieldEditorColumnInfo;
import CCDD.CcddConstants.InputDataType;
import CCDD.CcddConstants.InternalTable;
import CCDD.CcddConstants.InternalTable.FieldsColumn;
import CCDD.CcddEditorPanelHandler.UndoableTextField;

/******************************************************************************
 * CFS Command & Data Dictionary field handler class
 *****************************************************************************/
public class CcddFieldHandler
{
    // List of field definitions
    private List<String[]> fieldDefinitions;

    // List of field information
    private List<FieldInformation> fieldInformation;

    /**************************************************************************
     * Field handler class constructor
     *************************************************************************/
    CcddFieldHandler()
    {
    }

    /**************************************************************************
     * Field handler class constructor
     * 
     * @param ccddMain
     *            main class
     * 
     * @param ownerName
     *            name of the data field owner; null to build the information
     *            for all data fields
     * 
     * @param parent
     *            GUI component calling this method
     *************************************************************************/
    CcddFieldHandler(CcddMain ccddMain, String ownerName, Component parent)
    {
        // Load the data field definitions from the database
        fieldDefinitions = ccddMain.getDbTableCommandHandler().retrieveInformationTable(InternalTable.FIELDS,
                                                                                        parent);

        // Use the field definitions to create the data field information
        buildFieldInformation(fieldDefinitions.toArray(new String[0][0]),
                              ownerName);
    }

    /**************************************************************************
     * Get the data field definitions
     *
     * @return data field definitions
     *************************************************************************/
    protected List<String[]> getFieldDefinitions()
    {
        return fieldDefinitions;
    }

    /**************************************************************************
     * Set the data field definitions
     *
     * @param data
     *            field definitions
     *************************************************************************/
    protected void setFieldDefinitions(List<String[]> fieldDefinitions)
    {
        this.fieldDefinitions = fieldDefinitions;
    }

    /**************************************************************************
     * Get the data field information
     *
     * @return data field information
     *************************************************************************/
    protected List<FieldInformation> getFieldInformation()
    {
        return fieldInformation;
    }

    /**************************************************************************
     * Create a copy of the data field information
     *
     * @return Copy of the data field information
     *************************************************************************/
    protected List<FieldInformation> getFieldInformationCopy()
    {
        return getFieldInformationCopy(fieldInformation);
    }

    /**************************************************************************
     * Static method to create a copy of the supplied data field information
     *
     * @return Copy of the supplied data field information
     *************************************************************************/
    protected static List<FieldInformation> getFieldInformationCopy(List<FieldInformation> fieldInfo)
    {
        List<FieldInformation> fldInfo = new ArrayList<FieldInformation>();

        // Step through each field
        for (FieldInformation info : fieldInfo)
        {
            // Add the field to the copy
            fldInfo.add(new FieldInformation(info.getOwnerName(),
                                             info.getFieldName(),
                                             info.getDescription(),
                                             info.getSize(),
                                             info.getInputType(),
                                             info.isRequired(),
                                             info.getApplicabilityType(),
                                             info.getValue()));
        }

        return fldInfo;
    }

    /**************************************************************************
     * Set the data field information
     *
     * @param fieldInfo
     *            data field information
     *************************************************************************/
    protected void setFieldInformation(List<FieldInformation> fieldInfo)
    {
        fieldInformation = fieldInfo;
    }

    /**************************************************************************
     * Get the data field information for a specified field
     *
     * @param ownerName
     *            name of the data field owner (table name, including the path
     *            if this table references a structure, group name, or table
     *            type name)
     *
     * @param fieldName
     *            name of the field for which to get the field information
     *            (case insensitive)
     *
     * @return Reference to the data field information for the specified field;
     *         null if the field doesn't exist
     *************************************************************************/
    protected FieldInformation getFieldInformationByName(String ownerName,
                                                         String fieldName)
    {
        FieldInformation fieldInfo = null;

        // Step through each field
        for (FieldInformation info : fieldInformation)
        {
            // Check if the owner and field names match the ones supplied (case
            // insensitive)
            if (info.getOwnerName().equalsIgnoreCase(ownerName)
                && info.getFieldName().equalsIgnoreCase(fieldName))
            {
                // Store the field information reference and stop searching
                fieldInfo = info;
                break;
            }
        }

        return fieldInfo;
    }

    /**************************************************************************
     * Build the data field information from the field definitions
     *
     * @param ownerName
     *            name of the data field owner (table name, including the path
     *            if this table references a structure, group name, or table
     *            type name); null to get all data fields
     *************************************************************************/
    protected void buildFieldInformation(String ownerName)
    {
        buildFieldInformation(fieldDefinitions.toArray(new String[0][0]),
                              ownerName);
    }

    /**************************************************************************
     * Build the data field information from the field definitions provided
     *
     * @param fieldDefinitions
     *            array of field definitions; null if no definitions exist
     *            (this produces an empty field information list)
     *
     * @param ownerName
     *            name of the data field owner (table name, including the path
     *            if this table references a structure, group name, or table
     *            type name); null to get all data fields
     *************************************************************************/
    protected void buildFieldInformation(Object[][] fieldDefinitions,
                                         String ownerName)
    {
        // Check if the field information doesn't exist
        if (fieldInformation == null)
        {
            // Create storage for the field information
            fieldInformation = new ArrayList<FieldInformation>();
        }
        // The field information exists
        else
        {
            // Clear the fields from the list
            fieldInformation.clear();
        }

        // Check if the field definitions exist
        if (fieldDefinitions != null)
        {
            // Step through each field definition
            for (Object[] fieldDefn : fieldDefinitions)
            {
                // Check if the table (prototype.variable)/group name matches
                // the owner name; if no owner name is provided then get the
                // fields for all tables and groups
                if (ownerName == null
                    || ownerName.isEmpty()
                    || ownerName.equalsIgnoreCase(fieldDefn[FieldsColumn.OWNER_NAME.ordinal()].toString()))
                {
                    // Store the field information
                    addField(fieldDefn[FieldsColumn.OWNER_NAME.ordinal()].toString(),
                             fieldDefn[FieldsColumn.FIELD_NAME.ordinal()].toString(),
                             fieldDefn[FieldsColumn.FIELD_DESC.ordinal()].toString(),
                             Integer.valueOf(fieldDefn[FieldsColumn.FIELD_SIZE.ordinal()].toString()),
                             fieldDefn[FieldsColumn.FIELD_TYPE.ordinal()].toString(),
                             Boolean.valueOf(fieldDefn[FieldsColumn.FIELD_REQUIRED.ordinal()].toString()),
                             fieldDefn[FieldsColumn.FIELD_APPLICABILITY.ordinal()].toString(),
                             fieldDefn[FieldsColumn.FIELD_VALUE.ordinal()].toString());
                }
            }
        }
    }

    /**************************************************************************
     * Build the data field definitions from the data field editor provided
     *
     * @param fieldData
     *            array of data field editor data
     *
     * @param ownerName
     *            name of the data field owner (table name, including the path
     *            if this table references a structure, group name, or table
     *            type name)
     * 
     * @return Data field definitions array
     *************************************************************************/
    protected Object[][] buildFieldDefinition(Object[][] fieldData,
                                              String ownerName)
    {
        Object[][] fieldDefinition = new Object[0][0];

        // Check if any data fields are defined
        if (fieldData.length != 0)
        {
            // Create the field definition array with an extra column for the
            // table name
            fieldDefinition = new Object[fieldData.length][fieldData[0].length + 1];

            // Step through each row in the editor data array
            for (int row = 0; row < fieldData.length; row++)
            {
                // Add the table name (with path, if applicable) to the field
                // definition
                fieldDefinition[row][FieldsColumn.OWNER_NAME.ordinal()] = ownerName;

                // Copy the editor data to the field definition
                fieldDefinition[row][FieldsColumn.FIELD_NAME.ordinal()] = fieldData[row][FieldEditorColumnInfo.NAME.ordinal()];
                fieldDefinition[row][FieldsColumn.FIELD_DESC.ordinal()] = fieldData[row][FieldEditorColumnInfo.DESCRIPTION.ordinal()];
                fieldDefinition[row][FieldsColumn.FIELD_SIZE.ordinal()] = fieldData[row][FieldEditorColumnInfo.SIZE.ordinal()];
                fieldDefinition[row][FieldsColumn.FIELD_TYPE.ordinal()] = fieldData[row][FieldEditorColumnInfo.INPUT_TYPE.ordinal()];
                fieldDefinition[row][FieldsColumn.FIELD_REQUIRED.ordinal()] = fieldData[row][FieldEditorColumnInfo.REQUIRED.ordinal()];
                fieldDefinition[row][FieldsColumn.FIELD_APPLICABILITY.ordinal()] = fieldData[row][FieldEditorColumnInfo.APPLICABILITY.ordinal()];
                fieldDefinition[row][FieldsColumn.FIELD_VALUE.ordinal()] = fieldData[row][FieldEditorColumnInfo.VALUE.ordinal()];
            }
        }

        return fieldDefinition;
    }

    /**************************************************************************
     * Get the array of data field definitions
     *
     * @param isEditor
     *            true if the array is for use in the field editor. The field
     *            editor doesn't use the owner name in the array
     *
     * @return Object array containing the data field definitions
     *************************************************************************/
    protected Object[][] getFieldDefinitionArray(boolean isEditor)
    {
        List<Object[]> definitions = new ArrayList<Object[]>();

        // Step through each row
        for (FieldInformation fieldInfo : fieldInformation)
        {
            // Create storage for a single field definition
            Object[] row = new Object[isEditor
                                              ? FieldEditorColumnInfo.values().length
                                              : FieldsColumn.values().length];

            // Check if the array is for the field editor
            if (isEditor)
            {
                // Store the field definition in the proper order
                row[FieldEditorColumnInfo.NAME.ordinal()] = fieldInfo.getFieldName();
                row[FieldEditorColumnInfo.DESCRIPTION.ordinal()] = fieldInfo.getDescription();
                row[FieldEditorColumnInfo.INPUT_TYPE.ordinal()] = fieldInfo.getInputType().getInputName();
                row[FieldEditorColumnInfo.SIZE.ordinal()] = fieldInfo.getSize();
                row[FieldEditorColumnInfo.REQUIRED.ordinal()] = fieldInfo.isRequired();
                row[FieldEditorColumnInfo.APPLICABILITY.ordinal()] = fieldInfo.getApplicabilityType().getApplicabilityName();
                row[FieldEditorColumnInfo.VALUE.ordinal()] = fieldInfo.getValue();
            }
            // The array is not for the field editor
            else
            {
                // Store the field definition in the proper order
                row[FieldsColumn.OWNER_NAME.ordinal()] = fieldInfo.getOwnerName();
                row[FieldsColumn.FIELD_NAME.ordinal()] = fieldInfo.getFieldName();
                row[FieldsColumn.FIELD_DESC.ordinal()] = fieldInfo.getDescription();
                row[FieldsColumn.FIELD_TYPE.ordinal()] = fieldInfo.getInputType().getInputName();
                row[FieldsColumn.FIELD_SIZE.ordinal()] = fieldInfo.getSize();
                row[FieldsColumn.FIELD_REQUIRED.ordinal()] = fieldInfo.isRequired();
                row[FieldsColumn.FIELD_APPLICABILITY.ordinal()] = fieldInfo.getApplicabilityType().getApplicabilityName();
                row[FieldsColumn.FIELD_VALUE.ordinal()] = fieldInfo.getValue();
            }

            // Add the field definition to the list
            definitions.add(row);
        }

        return definitions.toArray(new Object[0][0]);
    }

    /**************************************************************************
     * Get the data field definitions
     *
     * @return String list containing the data field definitions
     *************************************************************************/
    protected List<String[]> getFieldDefinitionList()
    {
        // Create storage for the field definitions
        List<String[]> definitions = new ArrayList<String[]>();

        // Step through each row
        for (FieldInformation fieldInfo : fieldInformation)
        {
            // Create storage for a single field definition
            String[] row = new String[FieldsColumn.values().length];

            // Store the field definition in the proper order
            row[FieldsColumn.OWNER_NAME.ordinal()] = fieldInfo.getOwnerName();
            row[FieldsColumn.FIELD_NAME.ordinal()] = fieldInfo.getFieldName();
            row[FieldsColumn.FIELD_DESC.ordinal()] = fieldInfo.getDescription();
            row[FieldsColumn.FIELD_TYPE.ordinal()] = fieldInfo.getInputType().getInputName();
            row[FieldsColumn.FIELD_SIZE.ordinal()] = String.valueOf(fieldInfo.getSize());
            row[FieldsColumn.FIELD_REQUIRED.ordinal()] = String.valueOf(fieldInfo.isRequired());
            row[FieldsColumn.FIELD_APPLICABILITY.ordinal()] = fieldInfo.getApplicabilityType().getApplicabilityName();
            row[FieldsColumn.FIELD_VALUE.ordinal()] = fieldInfo.getValue();

            // Add the field definition to the list
            definitions.add(row);
        }

        return definitions;
    }

    /**************************************************************************
     * Add a field. This method is not applicable if the fields for all tables
     * and group are loaded
     *
     * @param ownerName
     *            name of the table/group to which the field is a member
     *
     * @param name
     *            name of the new field
     *
     * @param description
     *            field description
     *
     * @param size
     *            field display size in characters
     *
     * @param type
     *            input data type
     *
     * @param isRequired
     *            true if a value if required in this field
     *
     * @param applicability
     *            all, parent, or child to indicate all tables, parent tables
     *            only, or child tables only, respectively
     *
     * @param value
     *            data field value
     *************************************************************************/
    protected void addField(String ownerName,
                            String name,
                            String description,
                            int size,
                            String type,
                            boolean isRequired,
                            String applicability,
                            String value)
    {
        fieldInformation.add(new FieldInformation(ownerName,
                                                  name,
                                                  description,
                                                  size,
                                                  type,
                                                  isRequired,
                                                  applicability,
                                                  value));
    }

    /**************************************************************************
     * Update an existing data field's information
     *
     * @param updateInfo
     *            updated field information used to replace the existing field
     *            information
     *
     * @return true if the a matching owner and field exists for the provided
     *         field information update
     *************************************************************************/
    protected boolean updateField(FieldInformation updateInfo)
    {
        boolean isUpdate = false;

        // Get the reference to the field information for the specified
        // owner/field combination
        FieldInformation fieldInfo = getFieldInformationByName(updateInfo.getOwnerName(),
                                                               updateInfo.getFieldName());

        // Check if the owner/field combination exists and if the field differs
        // from the updated one
        if (fieldInfo != null
            && (!fieldInfo.getDescription().equals(updateInfo.getDescription())
                || !fieldInfo.getInputType().equals(updateInfo.getInputType())
                || fieldInfo.getSize() != updateInfo.getSize()
                || fieldInfo.isRequired() != updateInfo.isRequired()
                || !fieldInfo.getValue().equals(updateInfo.getValue())))
        {
            // Get the position of the field within the list
            int index = fieldInformation.indexOf(fieldInfo);

            // Remove the existing field from the list
            fieldInformation.remove(fieldInfo);

            // Add the updated field information to the list at the same
            // position as the old field
            fieldInformation.add(index, updateInfo);

            // Set the flag to indicate a match exists
            isUpdate = true;
        }

        return isUpdate;
    }

    /**************************************************************************
     * Change the owner name for the data fields
     *
     * @param newName
     *            new owner name
     *
     * @return List of field definitions with the updated owner name
     *************************************************************************/
    protected List<String[]> renameFieldTable(String newName)
    {
        // Step through each field
        for (int index = 0; index < fieldInformation.size(); index++)
        {
            // Set the owner name to the new name
            fieldInformation.get(index).setOwnerName(newName);
        }

        return getFieldDefinitionList();
    }

    /**************************************************************************
     * Count the number of the specified field type that exists in the field
     * information
     *
     * @param fieldType
     *            FieldInputType
     *
     * @return The number of the specified field type that exists in the field
     *         information
     *************************************************************************/
    protected int getFieldTypeCount(InputDataType fieldType)
    {
        int count = 0;

        // Step through each field definition
        for (FieldInformation fieldInfo : fieldInformation)
        {
            // Check if the field type matches the specified type
            if (fieldInfo.getInputType().equals(fieldType))
            {
                // Increment the type counter
                count++;
            }
        }

        return count;
    }

    /**************************************************************************
     * Prepend the table type indicator to the table type name for use in
     * identifying default data fields in the fields table
     *
     * @param tableType
     *            table type name
     *
     * @return Table type name with the table type indicator prepended
     *************************************************************************/
    protected static String getFieldTypeName(String tableType)
    {
        return TYPE_DATA_FIELD_IDENT + tableType;
    }

    /**************************************************************************
     * Prepend the group indicator to the group name for use in identifying
     * group data fields in the fields table
     *
     * @param groupName
     *            group name
     *
     * @return Group name with the group indicator prepended
     *************************************************************************/
    protected static String getFieldGroupName(String groupName)
    {
        return GROUP_DATA_FIELD_IDENT + groupName;
    }

    /**************************************************************************
     * Clear the values from all fields
     *************************************************************************/
    protected void clearFieldValues()
    {
        // Step through each field
        for (int index = 0; index < fieldInformation.size(); index++)
        {
            // Check if this is a boolean input (check box) data field
            if (fieldInformation.get(index).getInputType() == InputDataType.BOOLEAN)
            {
                // Set the field value to 'false'
                fieldInformation.get(index).setValue("false");

                // Set the check box
                ((JCheckBox) fieldInformation.get(index).getInputFld()).setSelected(false);
            }
            // Not a boolean input (check box) data field
            else
            {
                // Clear the field value
                fieldInformation.get(index).setValue("");

                // Get the reference to the text field
                UndoableTextField inputFld = (UndoableTextField) fieldInformation.get(index).getInputFld();

                inputFld.setText("");

                // Call the data field input verifier to set the background
                // color
                inputFld.getInputVerifier().verify(inputFld);
            }
        }
    }
}
