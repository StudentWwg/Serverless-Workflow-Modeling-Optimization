package util;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProgramExecutionTimeComparison {
    public static void WriteExecutionTimeToFile(String APPName, double[] executionTime, int iter) {
        try {
            String filePathPrefix = new File("").getCanonicalPath() +
                    "/src/main/resources/execution_time_comparison/" + iter + "/";

            //BCPO
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("ProgramExecutionTime_BCPO");
            HSSFRow row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("EASWExecutionTimeOfBCPO(ms)");
            row0.createCell(1).setCellValue("PRCPExecutionTimeOfBCPO(ms)");
            row0.createCell(2).setCellValue("UWCExecutionTimeOfBCPO(ms)");
            row0.createCell(3).setCellValue("DFBAExecutionTimeOfBCPO(ms)");

            HSSFRow aRow = sheet.createRow(1);
            aRow.createCell(0).setCellValue(executionTime[0]);
            aRow.createCell(1).setCellValue(executionTime[2]);
            aRow.createCell(2).setCellValue(executionTime[4]);
            aRow.createCell(3).setCellValue(executionTime[6]);

            FileOutputStream out = new FileOutputStream(filePathPrefix + APPName + "ProgramExecutionTime_BCPO.xls");
            workbook.write(out);
            out.close();

            HSSFWorkbook newWorkbook = new HSSFWorkbook();
            HSSFSheet newSheet = newWorkbook.createSheet("ProgramExecutionTime_PCCO");
            row0 = newSheet.createRow(0);
            row0.createCell(0).setCellValue("EASWExecutionTimeOfPCCO(ms)");
            row0.createCell(1).setCellValue("PRCPExecutionTimeOfPCCO(ms)");
            row0.createCell(2).setCellValue("UWCExecutionTimeOfPCCO(ms)");
            row0.createCell(3).setCellValue("DFBAExecutionTimeOfPCCO(ms)");

            aRow = newSheet.createRow(1);
            aRow.createCell(0).setCellValue(executionTime[1]);
            aRow.createCell(1).setCellValue(executionTime[3]);
            aRow.createCell(2).setCellValue(executionTime[5]);
            aRow.createCell(3).setCellValue(executionTime[7]);

            out = new FileOutputStream(filePathPrefix + APPName + "ProgramExecutionTime_PCCO.xls");
            newWorkbook.write(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
