package moe.imvery.utils.xlsx2json;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Feliciano on 6/1/2016.
 */
public class ExcelParser {

    /**
     * Parse the whole sheet of a workbook
     * @param workbook
     * @param configName
     * @return
     */
    public static JSONArray parseSheet( Workbook workbook, String configName ) {
        // Iterate through the rows.
        JSONArray rows = new JSONArray();

        // Get the Sheet by name.
        Sheet sheet = workbook.getSheet(configName);

        int typeRowIndex = 0, nameRowIndex = 1;

        // Fetch the type row.
        ArrayList<ParsedCellType> types = new ArrayList<>();
        if ( !sheet.getRow(typeRowIndex).getCell(0).getStringCellValue().equalsIgnoreCase("basic") ) {
            // If the primary key doesn't have a type defined "Basic", then we'll think all the columns are basic type,
            // and the first row is name row.
            typeRowIndex = 0;
            nameRowIndex = 0;

            Row typeRow = sheet.getRow(typeRowIndex);
            for (Iterator<Cell> cellsIT = typeRow.cellIterator(); cellsIT.hasNext(); )
            {
                Cell cell = cellsIT.next();
                types.add(ParsedCellType.BASIC);
            }
        } else {
            // Else read the type of each column
            Row typeRow = sheet.getRow(typeRowIndex);
            for (Iterator<Cell> cellsIT = typeRow.cellIterator(); cellsIT.hasNext(); )
            {
                Cell cell = cellsIT.next();
                String cellType = cell.getStringCellValue();
                types.add(ParsedCellType.fromString(cellType));
            }
        }

        // Fetch the name row.
        ArrayList<String> keys = new ArrayList<>();
        Row nameRow = sheet.getRow(nameRowIndex);
        for (Iterator<Cell> cellsIT = nameRow.cellIterator(); cellsIT.hasNext(); )
        {
            Cell cell = cellsIT.next();
            keys.add(cell.getStringCellValue());
        }

        // Parse each row.
        for (Iterator<Row> rowsIT = sheet.rowIterator(); rowsIT.hasNext(); )
        {
            Row row = rowsIT.next();

            if ( row.getRowNum() <= nameRowIndex )
                continue;

            // Iterate through the cells.
            JSONObject jsonRow = parseRow(row, keys, types);

            rows.put( jsonRow );
        }

        return rows;
    }

    /* TODO WIP
    public static Row findRowByColumn( Sheet sheet ) {
        for (Iterator<Row> rowsIT = sheet.rowIterator(); rowsIT.hasNext(); )
        {
            Row row = rowsIT.next();
        }
    }
    */

    /**
     * Parse a row of the sheet
     * @param row The target row to parse
     * @param keys The names
     * @param types The data types
     * @return A parsed JSONObject
     */
    public static JSONObject parseRow(Row row, ArrayList<String> keys, ArrayList<ParsedCellType> types) {
        JSONObject jsonRow = new JSONObject();

        //Parse each cell
        for ( Iterator<Cell> cellsIT = row.cellIterator(); cellsIT.hasNext(); )
        {
            Cell cellValue = cellsIT.next();
            int index = cellValue.getColumnIndex();
            String key = keys.get( index );
            ParsedCellType type = types.get( index );

            ArrayList result;
            JSONArray jsonArray;

            switch (type) {
                case BASIC:
                    switch (cellValue.getCellType())
                    {
                        case Cell.CELL_TYPE_NUMERIC:
                            jsonRow.put( key, cellValue.getNumericCellValue() );
                            break;
                        case Cell.CELL_TYPE_BOOLEAN:
                            jsonRow.put( key, cellValue.getBooleanCellValue() );
                            break;
                        default:
                            jsonRow.put( key, cellValue.getStringCellValue() );
                            break;
                    };
                    break;

                case ARRAY_STRING:
                    result = ExcelParser.<ArrayList<String>>parseCellData(type, cellValue);
                    jsonArray = new JSONArray(result);
                    jsonRow.put( key, jsonArray );
                    break;

                case ARRAY_BOOLEAN:
                    result = ExcelParser.<ArrayList<Boolean>>parseCellData(type, cellValue);
                    jsonArray = new JSONArray(result);
                    jsonRow.put( key, jsonArray );
                    break;

                case ARRAY_DOUBLE:
                    result = ExcelParser.<ArrayList<Double>>parseCellData(type, cellValue);
                    jsonArray = new JSONArray(result);
                    jsonRow.put( key, jsonArray );
                    break;

                case REFERENCE:
                    // TODO Deal with the reference type
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported type " + type + " found");
            }

        }
        return jsonRow;
    }

    /**
     * Parse a cell of the row
     * @param type The data type
     * @param cell The cell to be parsed
     * @param <W> The return data type
     * @return Parsed data
     * @throws NumberFormatException Numeric data parse failed
     */
    public static <W> W parseCellData(ParsedCellType type, Cell cell) throws NumberFormatException {
        Object object = null;

        String cellValue = cell.getStringCellValue();
        String[] items;

        switch (type) {
            case ARRAY_STRING:
                items = cellValue.split(",");
                ArrayList<String> arrayString = new ArrayList<>();
                for (String item : items) {
                    item = item.trim();
                    arrayString.add(item);
                }
                object = arrayString;
                break;

            case ARRAY_BOOLEAN:
                items = cellValue.split(",");
                ArrayList<Boolean> arrayBoolean = new ArrayList<>();
                for (String item : items) {
                    item = item.trim();
                    arrayBoolean.add(Boolean.parseBoolean(item));
                }
                object = arrayBoolean;
                break;

            case ARRAY_DOUBLE:
                items = cellValue.split(",");
                ArrayList<Double> arrayDouble = new ArrayList<>();
                for (String item : items) {
                    item = item.trim();
                    arrayDouble.add(Double.parseDouble(item));
                }
                object = arrayDouble;
                break;
        }

        return (W) object;
    }

}