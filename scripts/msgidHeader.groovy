/******************************************************************************
 * Description: Output a message ID header file
 * 
 * This Groovy script generates a message ID header file from the supplied
 * table information
 *****************************************************************************/
import CCDD.CcddScriptDataAccessHandler


// Get the number of structure and command table rows
def numStructRows = ccdd.getStructureTableNumRows()
def numCommandRows = ccdd.getCommandTableNumRows()

// Check if no structure data is supplied
if (numStructRows == 0)
{
    showErrorDialog("No structure data supplied to script " + ccdd.getScriptName())
}
// Check if no command data is supplied
else if (numCommandRows == 0)
{
    showErrorDialog("No command data supplied to script " + ccdd.getScriptName())
}
// Structure and command data is supplied
else
{
    // Combine the parent structure table's "System" data field value with
    // "_msgids" to create the file identifier name
    def msgIDName = ccdd.getTableDataFieldValue(ccdd.getParentStructureTableName(), "System") + "_msgids"
    
    // Build the output file name
    def outputFile = msgIDName + ".h"
    
    // Open the output file
    def file = ccdd.openOutputFile(outputFile)
    
    // Check if the output file successfully opened
    if (file != null)
    {
        // Add a header to the output file
        ccdd.writeToFileLn(file,
                           "/* Created : "
                           + ccdd.getDateAndTime()
                           + "\n   User    : "
                           + ccdd.getUser()
                           + "\n   Project : "
                           + ccdd.getProject()
                           + "\n   Script  : "
                           + ccdd.getScriptName()
                           + "\n   Table(s): "
                           + ccdd.getStructureTableNames().join(",\n             ")
                           + ccdd.getCommandTableNames().join(",\n             ")
                           + " */\n")

        ccdd.writeToFileLn(file, "#ifndef _" + msgIDName + "_H_")
        ccdd.writeToFileLn(file, "#define _" + msgIDName + "_H_")
        ccdd.writeToFileLn(file, "")
        
        // Get the number of header files to include
        def numInclude = ccdd.getTableNumRows("header")
        
        // Check if there are any header files to include
        if (numInclude != 0)
        {
            // Step through each header file name
            for (def row = 0; row < numInclude; row++)
            {
                // Output the include header statement
                ccdd.writeToFileLn(file, "#include " + ccdd.getTableData("header", "header file", row))
            }
        
            ccdd.writeToFileLn(file, "")
        }
        
        ccdd.writeToFileLn(file, "/* Define message IDs */")
        
        // Step through each command
        for (def row = 0; row < numCommandRows; row++)
        {
            // Output the command ID name and ID value
            ccdd.writeToFileLn(file,
                               "#define " +
                               ccdd.getCommandTableData("command name", row) +
                               "  " +
                               ccdd.getTableDataFieldValue("application id", ccdd.getStructureTableNameByRow(row)))
        }
        
        ccdd.writeToFileLn(file, "")
        ccdd.writeToFileLn(file, "typedef struct")
        ccdd.writeToFileLn(file, "{")
            
        // Step through each row in the table
        for (def row = 0; row < numStructRows; row++)
        {
            // Check if the structure name in the row matches the current structure
            if (ccdd.getParentStructureTableName().equals(ccdd.getStructureTableNameByRow(row)))
            {
                // Get the variable name for this row in the structure
                def variableName = ccdd.getStructureTableData("variable name", row)
                
                // Check if this is not an array member; array definitions are
                // output, but not members
                if (!variableName.endsWith("]"))
                {
                    // Output the data type and variable name
                    ccdd.writeToFile(file, 
                                     "  " +
                                     ccdd.getStructureTableData("data type", row) +
                                     "  " +
                                     variableName)
                    
                    // Get the index of the array size column
                    def arraySize = ccdd.getStructureTableData("array size", row)
                    
                    // Check if the array size is provided
                    if (arraySize != null && !arraySize.isEmpty())
                    {
                        // Output the array size
                        ccdd.writeToFile(file, "[" + arraySize +"]")
                    }
                    // No array size for this row
                    else 
                    {
                        // Get the index of the bit length column
                        def bitLength = ccdd.getStructureTableData("bit length", row)
                    
                        // Check if the bit length is provided
                        if (bitLength != null && !bitLength.isEmpty())
                        {
                            // Output the bit length
                            ccdd.writeToFile(file, ":" + bitLength)
                        }
                    }
                    
                    ccdd.writeToFileLn(file, ";")
                }
            }
        }
        
        ccdd.writeToFileLn(file, "} " + ccdd.getParentStructureTableName() + ";")
        ccdd.writeToFileLn(file, "")
        ccdd.writeToFileLn(file, "#endif")
        
        // Close the output file
        ccdd.closeFile(file)
    }
    // The output file cannot be opened
    else
    {
        // Display an error dialog
        ccdd.showErrorDialog("<html><b>Error opening output file '</b>" + outputFile + "<b>'")
    }
}