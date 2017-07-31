package com.core.helper;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class InteractiveExcel {
    public Workbook workbook;
    public Sheet sheet;
    public Drawing patriarch;

    public InteractiveExcel(String excelPath) {
        if (excelPath == "") {
            workbook = new HSSFWorkbook();
            return;
        }

        try {
            FileInputStream fileStream = new FileInputStream(excelPath);
            parseInputStream(fileStream);
            fileStream.close();
        } catch (Exception e) {
            workbook = new HSSFWorkbook();
        }
    }

    public InteractiveExcel() {
        workbook = new HSSFWorkbook();
    }

    public InteractiveExcel(InputStream fileContent) {
        parseInputStream(fileContent);
    }

    void parseInputStream(InputStream fileContent) {
        try {
            if (!fileContent.markSupported())
                fileContent = new PushbackInputStream(fileContent, 8);

            if (fileContent == null)
                workbook = new HSSFWorkbook();
            else if (POIFSFileSystem.hasPOIFSHeader(fileContent))
                workbook = new HSSFWorkbook(fileContent);
            else if (DocumentFactoryHelper.hasOOXMLHeader(fileContent))
                workbook = new XSSFWorkbook(fileContent);
        } catch (Exception e) {
        }
    }

    public void openOrReadFirst(String sheetName) {
        sheet = workbook.getSheet(sheetName);
        if (sheet == null)
            sheet = workbook.getSheetAt(0);
        patriarch = sheet.createDrawingPatriarch();
    }

    public void openOrReadFirst() {
        openOrReadFirst("Sheet1");
    }

    public void openOrCreateNew(String sheetName) {
        if (sheetName.equals(""))
            sheetName = "Sheet1";

        sheet = workbook.getSheet(sheetName);
        if (sheet == null)
            sheet = workbook.createSheet(sheetName);
        patriarch = sheet.createDrawingPatriarch();
    }

    public void openOrCreateNew() {
        openOrCreateNew("Sheet1");
    }


    public List<LinkedHashMap<String, Object>> readList() {
        List<LinkedHashMap<String, Object>> table = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        int cellCount = headerRow.getLastCellNum();
        for (int i = (sheet.getFirstRowNum() + 1); i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            LinkedHashMap<String, Object> dtRow = new LinkedHashMap<String, Object>();
            for (int j = 0; j < cellCount; j++) {
                if (row != null)
                    dtRow.put(getCellValue(headerRow.getCell(j)).toString(), getCellValue(row.getCell(j)));
            }
            table.add(dtRow);
        }
        return table;
    }

    Object getCellValue(Cell cell) {
        if (cell != null) {
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cell.getDateCellValue());
                    } else {
                        return new DecimalFormat().format(cell.getNumericCellValue()).replace(",", "");
                    }
                case Cell.CELL_TYPE_STRING:
                    return cell.getStringCellValue();
                default:
                    return cell.toString();
            }
        } else {
            return "";
        }
    }

    public void insertList(List<LinkedHashMap<String, Object>> dt, int rowIndex, int cellIndex, boolean onlyheader) {
        int currentRowIndex = rowIndex;
        String[] keys = dt.get(0).keySet().toArray(new String[0]);
        for (int x = 0; x < keys.length; x++) {
            insertText(currentRowIndex, cellIndex + x, keys[x]);
        }
        if (onlyheader)
            return;

        int sheetIndex = 0;
        for (int i = 0; i < dt.size(); i++) {
            currentRowIndex++;
            for (int j = 0; j < dt.get(i).size(); j++) {
                insertText(currentRowIndex, cellIndex + j, Utility.toStr(dt.get(i).get(keys[j])));
            }

            if (currentRowIndex == 65535 - rowIndex) {
                sheetIndex++;
                currentRowIndex = rowIndex;
                sheet = workbook.createSheet(workbook.getSheetAt(0).getSheetName() + "_" + sheetIndex);

                for (int x = 0; x < keys.length; x++) {
                    insertText(currentRowIndex, cellIndex + x, keys[x]);
                }
            }
        }
    }

    public void insertList(List<LinkedHashMap<String, Object>> dt, int rowIndex, int cellIndex) {
        insertList(dt, rowIndex, cellIndex, false);
    }

    public void insertList(List<LinkedHashMap<String, Object>> dt, boolean onlyheader) {
        insertList(dt, 0, 0, onlyheader);
    }

    public void insertList(List<LinkedHashMap<String, Object>> dt) {
        insertList(dt, 0, 0, false);
    }

    @SuppressWarnings("resource")
    public void insertPicture(int rowIndex, int cellIndex, String picturePath,
                              int width, int height) {
        try {
            FileInputStream fis = new FileInputStream(new File(picturePath));
            int filelong = fis.available();
            byte[] bytes = new byte[filelong];
            fis.read(bytes);
            int pictureIdx = workbook.addPicture(bytes,
                    HSSFWorkbook.PICTURE_TYPE_JPEG);
            HSSFClientAnchor anchor = new HSSFClientAnchor(0, 0, width, height,
                    (short) cellIndex, rowIndex, (short) cellIndex, rowIndex);
            Picture pict = patriarch.createPicture(anchor, pictureIdx);
            if (width == 0 || height == 0)
                pict.resize();
        } catch (Exception e) {
        }
    }

    public void insertPicture(int rowIndex, int cellIndex, String picturePatht) {
        insertPicture(rowIndex, cellIndex, picturePatht, 0, 0);
    }

    public void insertText(int rowIndex, int cellIndex, String textValue) {
        insertText(rowIndex, cellIndex, textValue, null);
    }

    public void insertText(int rowIndex, int cellIndex, String textValue,
                           HSSFCellStyle cellStyle) {
        Row row = sheet.getRow(rowIndex);
        if (row == null)
            row = sheet.createRow(rowIndex);
        Cell cell = row.getCell(cellIndex);
        if (cell == null)
            cell = row.createCell(cellIndex);
        cell.setCellValue(textValue);
        if (cellStyle != null)
            cell.setCellStyle(cellStyle);
    }

    public void saveFile(String filePath) {
        try {
            FileOutputStream file = new FileOutputStream(filePath);
            workbook.write(file);
            file.close();
        } catch (Exception e) {
        }
    }
    public String outFile(String projectPath, String path, String fileName) {
        FileOutputStream fos = null;
        try {
            File file = new File(projectPath + path);
            if (!file.exists()) {
                file.mkdir();
            }
            fos = new FileOutputStream(file.getPath() + System.getProperty("file.separator") + fileName + ".xls");
            workbook.write(fos);
            return path + "/" + fileName + ".xls";
        } catch (Exception e) {
            Utility.getLogger(this.getClass()).error("", e);
            return null;
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                Utility.getLogger(this.getClass()).error("", e);
            }
        }
    }
}
