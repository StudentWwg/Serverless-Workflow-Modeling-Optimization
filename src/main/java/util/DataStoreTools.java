package util;

import DFBA.DFBA_Result;
import PRCP.ERT_C_MEM_Config_iter;
import EASW.Chromosome;
import EASW.EASW_Result;
import EASW.EASW_SAL_OPT;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import serverlessWorkflow.graph.WVertex;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

public class DataStoreTools {
    public static void EASWDataStore(ArrayList<EASW_Result> results, EASW_SAL_OPT EASW_sal_opt, int iter) {
        String filenameprefix = "/src/main/resources/opt_curve_data/" + iter + "/EASW/App" + EASW_sal_opt.getPerfOpt().getApp().getGraph().getNode_num();
        FileOutputStream out = null;
        try {
            File file = new File("");
            String courseFile = file.getCanonicalPath();
            filenameprefix = courseFile + filenameprefix;
            if (Chromosome.getOPTType().equals("BCPO"))
                file = new File(filenameprefix + "_EASW_BCPO.xls");
            else if (Chromosome.getOPTType().equals("PCCO"))
                file = new File(filenameprefix + "_EASW_PCCO.xls");
            out = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("EASW_results");
        HSSFRow row0 = sheet.createRow(0);
        if (Chromosome.getOPTType().equals("BCPO")) {
            row0.createCell(0).setCellValue("numberOfGenes");
            row0.createCell(1).setCellValue("crossRate");
            row0.createCell(2).setCellValue("mutateRate");
            row0.createCell(3).setCellValue("numberOfGenerations");
            row0.createCell(4).setCellValue("etaM");
            row0.createCell(5).setCellValue("Budget_constraint");
            row0.createCell(6).setCellValue("BCPO_rt");
            row0.createCell(7).setCellValue("BCPO_cost");
            row0.createCell(8).setCellValue("BCPO_fitness");
            row0.createCell(9).setCellValue("BCPO_opt_mem");
        } else if (Chromosome.getOPTType().equals("PCCO")) {
            row0.createCell(0).setCellValue("numberOfGenes");
            row0.createCell(1).setCellValue("crossRate");
            row0.createCell(2).setCellValue("mutateRate");
            row0.createCell(3).setCellValue("numberOfGenerations");
            row0.createCell(4).setCellValue("etaM");
            row0.createCell(5).setCellValue("Performance_constraint");
            row0.createCell(6).setCellValue("PCCO_rt");
            row0.createCell(7).setCellValue("PCCO_cost");
            row0.createCell(8).setCellValue("PCCO_fitness");
            row0.createCell(9).setCellValue("PCCO_opt_mem");
        }

        for (int i = 0; i < results.size(); i++) {
            HSSFRow row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(results.get(i).getNumOfGenes());
            row.createCell(1).setCellValue(results.get(i).getCrossRate());
            row.createCell(2).setCellValue(results.get(i).getMutateRate());
            row.createCell(3).setCellValue(results.get(i).getNumOfGenerations());
            row.createCell(4).setCellValue(results.get(i).getEtaM());
            row.createCell(5).setCellValue(results.get(i).getConstraint());
            row.createCell(6).setCellValue(results.get(i).getRt());
            row.createCell(7).setCellValue(results.get(i).getCost());
            row.createCell(8).setCellValue(results.get(i).getFitness());
            row.createCell(9).setCellValue(DataStoreTools.MemConfigToString(results.get(i).getMemConfig()));
        }
        try {
            workbook.write(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void PRCPDataStore(ArrayList<ERT_C_MEM_Config_iter[]> results, int verticesNum, String OPTType, int iter) {
        String filenameprefix = "/src/main/resources/opt_curve_data/" + iter + "/PRCP/App" + verticesNum;
        FileOutputStream out = null;
        try {
            File file = new File("");
            String courseFile = file.getCanonicalPath();
            filenameprefix = courseFile + filenameprefix;
            if (OPTType.equals("BCPO"))
                file = new File(filenameprefix + "_PRCP_BCPO.xls");
            else if (OPTType.equals("PCCO"))
                file = new File(filenameprefix + "_PRCP_PCCO.xls");
            out = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("PRCP_results");
        HSSFRow row0 = sheet.createRow(0);
        if (OPTType.equals("BCPO")) {
            row0.createCell(0).setCellValue("Budget");
            row0.createCell(1).setCellValue("BCR_disabled_RT");
            row0.createCell(2).setCellValue("BCR_RT/M_RT");
            row0.createCell(3).setCellValue("BCR_ERT/C_RT");
            row0.createCell(4).setCellValue("BCR_MAX_RT");
            row0.createCell(5).setCellValue("BCR_disabled_Cost");
            row0.createCell(6).setCellValue("BCR_RT/M_Cost");
            row0.createCell(7).setCellValue("BCR_ERT/C_Cost");
            row0.createCell(8).setCellValue("BCR_MAX_Cost");
            row0.createCell(9).setCellValue("BCR_disabled_Config");
            row0.createCell(10).setCellValue("BCR_RT/M_Config");
            row0.createCell(11).setCellValue("BCR_ERT/C_Config");
            row0.createCell(12).setCellValue("BCR_MAX_Config");
            row0.createCell(13).setCellValue("BCR_disabled_Iterations");
            row0.createCell(14).setCellValue("BCR_RT/M_Iterations");
            row0.createCell(15).setCellValue("BCR_ERT/C_Iterations");
            row0.createCell(16).setCellValue("BCR_MAX_Iterations");
            row0.createCell(17).setCellValue("BCR_threshold");
        } else if (OPTType.equals("PCCO")) {
            row0.createCell(0).setCellValue("Performance");
            row0.createCell(1).setCellValue("BCR_disabled_RT");
            row0.createCell(2).setCellValue("BCR_RT/M_RT");
            row0.createCell(3).setCellValue("BCR_ERT/C_RT");
            row0.createCell(4).setCellValue("BCR_MAX_RT");
            row0.createCell(5).setCellValue("BCR_disabled_Cost");
            row0.createCell(6).setCellValue("BCR_RT/M_Cost");
            row0.createCell(7).setCellValue("BCR_ERT/C_Cost");
            row0.createCell(8).setCellValue("BCR_MAX_Cost");
            row0.createCell(9).setCellValue("BCR_disabled_Config");
            row0.createCell(10).setCellValue("BCR_RT/M_Config");
            row0.createCell(11).setCellValue("BCR_ERT/C_Config");
            row0.createCell(12).setCellValue("BCR_MAX_Config");
            row0.createCell(13).setCellValue("BCR_disabled_Iterations");
            row0.createCell(14).setCellValue("BCR_RT/M_Iterations");
            row0.createCell(15).setCellValue("BCR_ERT/C_Iterations");
            row0.createCell(16).setCellValue("BCR_MAX_Iterations");
            row0.createCell(17).setCellValue("BCR_threshold");
        }

        for (int i = 0; i < results.size(); i++) {
            HSSFRow row = sheet.createRow(i + 1);
            ERT_C_MEM_Config_iter[] AResult = results.get(i);
            row.createCell(0).setCellValue(AResult[0].getConstraint());
            row.createCell(1).setCellValue(AResult[0].getCurrent_avg_rt());
            row.createCell(2).setCellValue(AResult[1].getCurrent_avg_rt());
            row.createCell(3).setCellValue(AResult[2].getCurrent_avg_rt());
            row.createCell(4).setCellValue(AResult[3].getCurrent_avg_rt());
            row.createCell(5).setCellValue(AResult[0].getCurrent_cost());
            row.createCell(6).setCellValue(AResult[1].getCurrent_cost());
            row.createCell(7).setCellValue(AResult[2].getCurrent_cost());
            row.createCell(8).setCellValue(AResult[3].getCurrent_cost());
            row.createCell(9).setCellValue(DataStoreTools.MemConfigToString(AResult[0].getCurrent_mem_configuration()));
            row.createCell(10).setCellValue(DataStoreTools.MemConfigToString(AResult[1].getCurrent_mem_configuration()));
            row.createCell(11).setCellValue(DataStoreTools.MemConfigToString(AResult[2].getCurrent_mem_configuration()));
            row.createCell(12).setCellValue(DataStoreTools.MemConfigToString(AResult[3].getCurrent_mem_configuration()));
            row.createCell(13).setCellValue(AResult[0].getIterations_count());
            row.createCell(14).setCellValue(AResult[1].getIterations_count());
            row.createCell(15).setCellValue(AResult[2].getIterations_count());
            row.createCell(16).setCellValue(AResult[3].getIterations_count());
            row.createCell(17).setCellValue(AResult[0].getIterations_count());
        }
        try {
            workbook.write(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String MemConfigToString(TreeMap<WVertex, Integer> memDict) {
        StringBuffer sb = new StringBuffer("{ ");
        for (WVertex vertex : memDict.keySet())
            sb.append(vertex.toString() + " : " + memDict.get(vertex) + " ");
        sb.append(" } ");
        return sb.toString();
    }

    public static void UWCDataStore(ArrayList<ERT_C_MEM_Config_iter> results, int verticesNum, String OPTType, int iter) {
        String filenameprefix = "/src/main/resources/opt_curve_data/" + iter + "/UWC/App" + verticesNum;
        FileOutputStream out = null;
        try {
            File file = new File("");
            String courseFile = file.getCanonicalPath();
            filenameprefix = courseFile + filenameprefix;
            if (OPTType.equals("BCPO"))
                file = new File(filenameprefix + "_UWC_BCPO.xls");
            else if (OPTType.equals("PCCO"))
                file = new File(filenameprefix + "_UWC_PCCO.xls");
            out = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("UWC_results");
        HSSFRow row0 = sheet.createRow(0);
        if (OPTType.equals("BCPO")) {
            row0.createCell(0).setCellValue("Budget");
            row0.createCell(1).setCellValue("UWC_BCPO_RT");
            row0.createCell(2).setCellValue("UWC_BCPO_Cost");
            row0.createCell(3).setCellValue("UWC_BCPO_Config");
            row0.createCell(4).setCellValue("UWC_BCPO_Iterations");
        } else if (OPTType.equals("PCCO")) {
            row0.createCell(0).setCellValue("Performance");
            row0.createCell(1).setCellValue("UWC_PCCO_RT");
            row0.createCell(2).setCellValue("UWC_PCCO_Cost");
            row0.createCell(3).setCellValue("UWC_PCCO_Config");
            row0.createCell(4).setCellValue("UWC_PCCO_Iterations");
        }

        for (int i = 0; i < results.size(); i++) {
            HSSFRow row = sheet.createRow(i + 1);
            ERT_C_MEM_Config_iter AResult = results.get(i);
            row.createCell(0).setCellValue(AResult.getConstraint());
            row.createCell(1).setCellValue(AResult.getCurrent_avg_rt());
            row.createCell(2).setCellValue(AResult.getCurrent_cost());
            row.createCell(3).setCellValue(DataStoreTools.MemConfigToString(AResult.getCurrent_mem_configuration()));
            row.createCell(4).setCellValue(AResult.getIterations_count());
        }
        try {
            workbook.write(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void DFBADataStore(ArrayList<DFBA_Result> results, int verticesNum, String OPTType, int iter) {
        String filenameprefix = "/src/main/resources/opt_curve_data/" + iter + "/DFBA/App" + verticesNum;
        FileOutputStream out = null;
        try {
            File file = new File("");
            String courseFile = file.getCanonicalPath();
            filenameprefix = courseFile + filenameprefix;
            if (OPTType.equals("BCPO"))
                file = new File(filenameprefix + "_DFBA_BCPO.xls");
            else if (OPTType.equals("PCCO"))
                file = new File(filenameprefix + "_DFBA_PCCO.xls");
            out = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("DFBA_results");
        HSSFRow row0 = sheet.createRow(0);
        if (OPTType.equals("BCPO")) {
            row0.createCell(0).setCellValue("Budget");
            row0.createCell(1).setCellValue("DFBA_BCPO_RT");
            row0.createCell(2).setCellValue("DFBA_BCPO_Cost");
            row0.createCell(3).setCellValue("DFBA_BCPO_Config");
        } else if (OPTType.equals("PCCO")) {
            row0.createCell(0).setCellValue("Performance");
            row0.createCell(1).setCellValue("DFBA_PCCO_RT");
            row0.createCell(2).setCellValue("DFBA_PCCO_Cost");
            row0.createCell(3).setCellValue("DFBA_PCCO_Config");
        }

        for (int i = 0; i < results.size(); i++) {
            HSSFRow row = sheet.createRow(i + 1);
            DFBA_Result AResult = results.get(i);
            row.createCell(0).setCellValue(AResult.getConstraint());
            row.createCell(1).setCellValue(AResult.getRt());
            row.createCell(2).setCellValue(AResult.getCost());
            row.createCell(3).setCellValue(DataStoreTools.MemConfigToString(AResult.getMemConfig()));
        }
        try {
            workbook.write(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void WriteSuccessRateToFile(File[] files, double[] BCPOSuccessRate, double[] PCCOSuccessRate, int iter) {
        try {
            String filePath = new File("").getCanonicalPath() + "/src/main/resources/success_rate/" + iter + "/SuccessRate.xls";
            FileOutputStream outputStream = new FileOutputStream(filePath);
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("success_rate");

            HSSFRow row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("Application");
            row0.createCell(1).setCellValue("BCPO SuccessRate");
            row0.createCell(2).setCellValue("PCCO SuccessRate");

            for (int i = 0; i < files.length; i++) {
                HSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(files[i].getName().replace(".json", ""));
                row.createCell(1).setCellValue(BCPOSuccessRate[i]);
                row.createCell(2).setCellValue(PCCOSuccessRate[i]);
            }

            HSSFRow row = sheet.createRow(files.length + 1);
            row.createCell(0).setCellValue("Average");
            row.createCell(1).setCellValue(Arrays.stream(BCPOSuccessRate).average().getAsDouble());
            row.createCell(2).setCellValue(Arrays.stream(PCCOSuccessRate).average().getAsDouble());

            workbook.write(outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getFinalOptimizationResult() throws IOException {
        String[] OPTTypes = {"BCPO", "PCCO"};
        String filenameprefix = new File("").getCanonicalPath() +  "/src/main/resources/opt_curve_data/";
        String[] App = {"APP10", "APP16", "APP22"};
        for (String OPTType : OPTTypes) {
            for (String app : App) {
                ArrayList<Double> constraints = new ArrayList<>();
                ArrayList<Double> EASW = new ArrayList<>();
                ArrayList<Double> PRCP = new ArrayList<>();
                ArrayList<Double> UWC = new ArrayList<>();
                ArrayList<Double> DFBA = new ArrayList<>();
                for (int iter = 1; iter < 10; iter++) {
                    String fileName = filenameprefix + iter + "/AllAlgorithmResults_" + app + "_" + OPTType + ".xls";
                    FileInputStream inputStream = new FileInputStream(fileName);
                    HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
                    HSSFSheet sheet = workbook.getSheet("AllAlgorithmResults");
                    int rowNum = sheet.getLastRowNum();
                    HSSFRow aRow = null;
                    for (int row = 1; row <= rowNum; row++) {
                        aRow = sheet.getRow(row);
                        if (iter == 1){
                            HSSFCell aCell = aRow.getCell(0);
                            constraints.add(Double.valueOf(aCell.getStringCellValue()));
                        }
                        if (iter == 1) {
                            EASW.add(aRow.getCell(1).getNumericCellValue());
                            PRCP.add(Math.min(aRow.getCell(2).getNumericCellValue(), Math.min(aRow.getCell(3).getNumericCellValue(),
                                    Math.min(aRow.getCell(4).getNumericCellValue(), aRow.getCell(5).getNumericCellValue()))));
                            UWC.add(aRow.getCell(6).getNumericCellValue());
                            DFBA.add(aRow.getCell(7).getNumericCellValue());
                        } else {
                            EASW.set(row - 1, EASW.get(row - 1).doubleValue() + aRow.getCell(1).getNumericCellValue());
                            double PRCPX = Math.min(aRow.getCell(2).getNumericCellValue(), Math.min(aRow.getCell(3).getNumericCellValue(),
                                    Math.min(aRow.getCell(4).getNumericCellValue(), aRow.getCell(5).getNumericCellValue())));
                            PRCP.set(row - 1, PRCP.get(row - 1).doubleValue() + PRCPX);
                            UWC.set(row - 1, UWC.get(row - 1).doubleValue() + aRow.getCell(6).getNumericCellValue());
                            DFBA.set(row - 1, DFBA.get(row - 1).doubleValue() + aRow.getCell(7).getNumericCellValue());
                        }
                    }
                }
                for (int i = 0; i < PRCP.size(); i++)
                    PRCP.set(i, PRCP.get(i).doubleValue() / 9);
                for (int i = 0; i < EASW.size(); i++)
                    EASW.set(i, EASW.get(i).doubleValue() / 9);
                for (int i = 0; i < UWC.size(); i++)
                    UWC.set(i, UWC.get(i).doubleValue() / 9);
                for (int i = 0; i < DFBA.size(); i++)
                    DFBA.set(i, DFBA.get(i).doubleValue() / 9);

                FileOutputStream outputStream = new FileOutputStream(filenameprefix + "AllAlgorithmResults_" + app + "_" + OPTType + ".xls");
                HSSFWorkbook workbook = new HSSFWorkbook();
                HSSFSheet sheet = workbook.createSheet("AllAlgorithmResults");
                HSSFRow row0 = sheet.createRow(0);

                if(OPTType.equals("BCPO"))
                    row0.createCell(0).setCellValue("Budget Constraint");
                else if(OPTType.equals("PCCO"))
                    row0.createCell(0).setCellValue("Performance Constarint");
                row0.createCell(1).setCellValue("EASW");
                row0.createCell(2).setCellValue("PRCP");
                row0.createCell(3).setCellValue("UWC");
                row0.createCell(4).setCellValue("DFBA");

                for(int i=0;i< constraints.size();i++){
                    HSSFRow aRow = sheet.createRow(i+1);
                    aRow.createCell(0).setCellValue(constraints.get(i));
                    aRow.createCell(1).setCellValue(EASW.get(i));
                    aRow.createCell(2).setCellValue(PRCP.get(i));
                    aRow.createCell(3).setCellValue(UWC.get(i));
                    aRow.createCell(4).setCellValue(DFBA.get(i));
                }

                workbook.write(outputStream);
                outputStream.close();
            }
        }
    }
    public static void getFinalExecutionTime() throws IOException{
        String[] OPTTypes = {"BCPO", "PCCO"};
        String filenameprefix = new File("").getCanonicalPath() + "/src/main/resources/execution_time_comparison/";
        String[] App = {"APP10", "APP16", "APP22"};
        for (String OPTType : OPTTypes) {
            for (String app : App) {
                ArrayList<Double> EASWTime = new ArrayList<>();
                ArrayList<Double> PRCPTime = new ArrayList<>();
                ArrayList<Double> UWCTime = new ArrayList<>();
                ArrayList<Double> DFBATime = new ArrayList<>();
                for (int iter = 1; iter <= 9; iter++) {
                    String fileName = filenameprefix + iter + "/"+app+"ProgramExecutionTime_"+ OPTType + ".xls";
                    FileInputStream inputStream = new FileInputStream(fileName);
                    HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
                    HSSFSheet sheet = workbook.getSheet("ProgramExecutionTime_"+OPTType);
                    int rowNum = sheet.getLastRowNum();
                    HSSFRow aRow = sheet.getRow(1);
                    EASWTime.add(aRow.getCell(0).getNumericCellValue());
                    PRCPTime.add(aRow.getCell(1).getNumericCellValue());
                    UWCTime.add(aRow.getCell(2).getNumericCellValue());
                    DFBATime.add(aRow.getCell(3).getNumericCellValue());
                }
                double avgEASWTime = EASWTime.stream().mapToDouble((x) -> x).average().getAsDouble();
                double avgPRCPTime = PRCPTime.stream().mapToDouble((x) -> x).average().getAsDouble();
                double avgUWCTime = UWCTime.stream().mapToDouble((x) -> x).average().getAsDouble();
                double avgDFBATime = DFBATime.stream().mapToDouble((x) -> x).average().getAsDouble();

                FileOutputStream outputStream = new FileOutputStream(filenameprefix + app+"avgProgramExecutionTime_"+ OPTType + ".xls");
                HSSFWorkbook workbook = new HSSFWorkbook();
                HSSFSheet sheet = workbook.createSheet("ProgramExecutionTime_"+OPTType);
                HSSFRow row0 = sheet.createRow(0);

                row0.createCell(0).setCellValue("EASWExecutionTimeOf"+OPTType+"(ms)");
                row0.createCell(1).setCellValue("PRCPExecutionTimeOf"+OPTType+"O(ms)");
                row0.createCell(2).setCellValue("UWCExecutionTimeOf"+OPTType+"(ms)");
                row0.createCell(3).setCellValue("DFBAExecutionTimeOf"+OPTType+"(ms)");

                HSSFRow aRow = sheet.createRow(1);
                aRow.createCell(0).setCellValue(avgEASWTime);
                aRow.createCell(1).setCellValue(avgPRCPTime);
                aRow.createCell(2).setCellValue(avgUWCTime);
                aRow.createCell(3).setCellValue(avgDFBATime);

                workbook.write(outputStream);
                outputStream.close();
            }
        }
    }
}
