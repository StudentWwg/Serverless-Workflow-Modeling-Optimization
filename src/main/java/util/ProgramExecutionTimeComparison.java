package util;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProgramExecutionTimeComparison {
    public static void WriteExecutionTimeToFile(long CPOGAExecutionTimeOfBCPO, long CPOGAExecutionTimeOfPCCO,
                                                long PRCPExecutionTimeOfBCPO, long PRCPExecutionTimeOfPCCO,
                                                long UWCExecutionTimeOfBCPO, long UWCExecutionTimeOfPCCO, int repeatedTimes){
        try{
            String filePathPrefix = new File("").getCanonicalPath() +
                    "/src/main/resources/execution_time_comparison/"+repeatedTimes+"/";

            //BCPO
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("ProgramExecutionTime_BCPO");
            HSSFRow row0 = sheet.createRow(0); //表头行
            row0.createCell(0).setCellValue("CPOGAExecutionTimeOfBCPO(ms)");
            row0.createCell(1).setCellValue("PRCPExecutionTimeOfBCPO(ms)");
            row0.createCell(2).setCellValue("UWCExecutionTimeOfBCPO(ms)");

            HSSFRow aRow = sheet.createRow(1);
            aRow.createCell(0).setCellValue(CPOGAExecutionTimeOfBCPO);
            aRow.createCell(1).setCellValue(PRCPExecutionTimeOfBCPO);
            aRow.createCell(2).setCellValue(UWCExecutionTimeOfBCPO);

            FileOutputStream out = new FileOutputStream(filePathPrefix + "ProgramExecutionTime_BCPO.xls");
            workbook.write(out);
            out.close();

            HSSFWorkbook newWorkbook = new HSSFWorkbook();
            HSSFSheet newSheet = newWorkbook.createSheet("ProgramExecutionTime_PCCO");
            row0 = newSheet.createRow(0); //表头行
            row0.createCell(0).setCellValue("CPOGAExecutionTimeOfPCCO(ms)");
            row0.createCell(1).setCellValue("PRCPExecutionTimeOfPCCO(ms)");
            row0.createCell(2).setCellValue("UWCExecutionTimeOfPCCO(ms)");

            aRow = newSheet.createRow(1);
            aRow.createCell(0).setCellValue(CPOGAExecutionTimeOfPCCO);
            aRow.createCell(1).setCellValue(PRCPExecutionTimeOfPCCO);
            aRow.createCell(2).setCellValue(UWCExecutionTimeOfPCCO);

            out = new FileOutputStream(filePathPrefix + "ProgramExecutionTime_PCCO.xls");
            newWorkbook.write(out);
            out.close();
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }

    }
}
