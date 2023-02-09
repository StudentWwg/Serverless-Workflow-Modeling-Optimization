package util;

import PRCPG.ERT_C_MEM_Config_iter;
import CPOGA.Chromosome;
import CPOGA.CPOGA_Result;
import CPOGA.CPOGA_SAL_OPT;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import serverlessWorkflow.graph.WVertex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Vector;

public class DataStoreTools {
    public static void CPOGADataStore(ArrayList<CPOGA_Result> results, CPOGA_SAL_OPT CPOGA_sal_opt, int repeatedTimes) {
        String filenameprefix = "/src/main/resources/opt_curve_data/"+repeatedTimes+"/CPOGA/App" + String.valueOf(CPOGA_sal_opt.getPerfOpt().getApp().getGraph().getDirectedGraph().vertexSet().size());
        FileOutputStream out = null;
        try {
            File file = new File("");
            String courseFile = file.getCanonicalPath();  //项目路径
            filenameprefix = courseFile + filenameprefix;
            if (Chromosome.getOPTType().equals("BCPO"))  //执行程序前一定要先创建excel文件
                file = new File(filenameprefix + "_CPOGA_BCPO.xls");
            else if (Chromosome.getOPTType().equals("PCCO"))
                file = new File(filenameprefix + "_CPOGA_PCCO.xls");
            out = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("CPOGA_results");
        HSSFRow row0 = sheet.createRow(0); //表头行
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
            //System.out.println("写入成功");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void PRCPDataStore(ArrayList<ERT_C_MEM_Config_iter[]> results, int verticesNum, String OPTType,int repeatedTimes) {
        String filenameprefix = "/src/main/resources/opt_curve_data/"+repeatedTimes+"/PRCP/App" + String.valueOf(verticesNum);
        FileOutputStream out = null;
        try {
            File file = new File("");
            String courseFile = file.getCanonicalPath();  //项目路径
            filenameprefix = courseFile + filenameprefix;
            if (OPTType.equals("BCPO"))  //执行程序前一定要先创建excel文件
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
        HSSFRow row0 = sheet.createRow(0); //表头行
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
            System.out.println("写入成功");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String MemConfigToString(TreeMap<WVertex, Integer> memDict) {  //将各个vertex的mem配置转换成字符串
        StringBuffer sb = new StringBuffer("{ ");
        for (WVertex vertex : memDict.keySet())
            sb.append(vertex.toString() + " : " + memDict.get(vertex) + " ");
        sb.append(" } ");
        return sb.toString();
    }

    public static void UWCDataStore(ArrayList<ERT_C_MEM_Config_iter> results, int verticesNum, String OPTType,int repeatedTimes){
        String filenameprefix = "/src/main/resources/opt_curve_data/"+repeatedTimes+"/UWC/App" + String.valueOf(verticesNum);
        FileOutputStream out = null;
        try {
            File file = new File("");
            String courseFile = file.getCanonicalPath();  //项目路径
            filenameprefix = courseFile + filenameprefix;
            if (OPTType.equals("BCPO"))  //执行程序前一定要先创建excel文件
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
        HSSFRow row0 = sheet.createRow(0); //表头行
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
            System.out.println("写入成功");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void WriteSuccessRateToFile(double[][] successRate){
        try {
            double avgSuccessRateOfBCPO = 0, avgSuccessRateOfPCCO=0;
            String filePath = new File("").getCanonicalPath() + "/src/main/resources/success_rate/SuccessRate.xls";
            FileOutputStream outputStream = new FileOutputStream(filePath);
            HSSFWorkbook workbook =new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("success_rate");

            HSSFRow row0 = sheet.createRow(0); //表头行
            row0.createCell(0).setCellValue("Iteration");
            row0.createCell(1).setCellValue("BCPO SuccessRate");
            row0.createCell(2).setCellValue("PCCO SuccessRate");
            for(int i=0;i< successRate.length;i++){
                HSSFRow row = sheet.createRow(i+1);
                row.createCell(0).setCellValue(i+1);
                row.createCell(1).setCellValue(successRate[i][0]);
                row.createCell(2).setCellValue(successRate[i][1]);
                avgSuccessRateOfBCPO += successRate[i][0];
                avgSuccessRateOfPCCO += successRate[i][1];
            }

            avgSuccessRateOfBCPO = avgSuccessRateOfBCPO/successRate.length;
            avgSuccessRateOfPCCO = avgSuccessRateOfPCCO/successRate.length;
            HSSFRow row = sheet.createRow(successRate.length+1);
            row.createCell(0).setCellValue("汇总");
            row.createCell(1).setCellValue(avgSuccessRateOfBCPO);
            row.createCell(2).setCellValue(avgSuccessRateOfPCCO);
            workbook.write(outputStream);
            outputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
