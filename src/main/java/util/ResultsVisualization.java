package util;


import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtils;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;

import java.awt.*;
import java.io.*;

public class ResultsVisualization {
    private static PerfOpt perfOpt;
    private static String OPTType;
    private static String[] BudgetConstraints;
    private static String[] PerformanceConstraints;
    private static String[] BCPOrowKeys = {"EASW", "Without BCR", "BCR RT/M", "BCR ERT/C", "BCR MAX", "UWC", "DFBA"};
    private static String[] PCCOrowKeys = {"EASW", "Without BCR", "BCR M/RT", "BCR C/ERT", "BCR MAX", "UWC", "DFBA"};
    private static int numOfBCPOResults = 0;
    private static int numOfPCCOResults = 0;
    private static int numOfBetterBCPOResultsOfEASW = 0;
    private static int numOfBetterPCCOResultsOfEASW = 0;

    public static CategoryDataset createDataset(int iter) {
        if (ResultsVisualization.OPTType.equals("BCPO")) {
            double[][] data = ResultsVisualization.getAlgorithmResults(iter);
            return DatasetUtils.createCategoryDataset(ResultsVisualization.BCPOrowKeys, ResultsVisualization.BudgetConstraints, data);
        } else if (ResultsVisualization.OPTType.equals("PCCO")) {
            double[][] data = ResultsVisualization.getAlgorithmResults(iter);
            return DatasetUtils.createCategoryDataset(ResultsVisualization.PCCOrowKeys, ResultsVisualization.PerformanceConstraints, data);
        }else return null;
    }

    public static JFreeChart createChart(CategoryDataset categoryDataset) {
        JFreeChart jfreechart = null;
        if (ResultsVisualization.OPTType.equals("BCPO")) {
            jfreechart = ChartFactory.createLineChart(
                    "The optimal performance under budget constraint obtained from EASW, PRCP in four strategies, UWC and DFBA",
                    "Budget Constraint in USD (per 10 Million Executions)",
                    "End-to-end Response time in ms",
                    categoryDataset,
                    PlotOrientation.VERTICAL, true,
                    false,
                    false);
        } else if (ResultsVisualization.OPTType.equals("PCCO")) {
            jfreechart = ChartFactory.createLineChart(
                    "The optimal cost under performance constraint obtained from EASW, PRCP in four strategies, UWC and DFBA",
                    "Performance Constraint in ms",
                    "Cost per 10 Million Executions in USD",
                    categoryDataset,
                    PlotOrientation.VERTICAL, true,
                    false,
                    false);
        }

        CategoryPlot plot = (CategoryPlot) jfreechart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setBackgroundAlpha(0.5f);
        plot.setForegroundAlpha(0.5f);
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultLinesVisible(true);
        renderer.setUseSeriesOffset(true);
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(false);
        return jfreechart;
    }

    public static void saveAsFile(JFreeChart chart, int iter) {
        FileOutputStream out = null;
        try {
            String filePathPrefix = new File("").getCanonicalPath() + "/src/main/resources/opt_curve_pictures/" + iter + "/";
            String outputPath = filePathPrefix + perfOpt.getApp().getGraph().getAPPName() + "_Optimization_Curve_" + ResultsVisualization.OPTType + ".jpeg";
            File outFile = new File(outputPath);
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(outputPath);
            ChartUtils.writeChartAsJPEG(out, chart, 1000, 800);
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }

    public static void DrawPictures(PerfOpt perfOpt, int iter) {
        ResultsVisualization.perfOpt = perfOpt;
        ResultsVisualization.OPTType = "BCPO";
        CategoryDataset datasetOfBCPO = ResultsVisualization.createDataset(iter);
        JFreeChart chartOfBCPO = ResultsVisualization.createChart(datasetOfBCPO);
        ResultsVisualization.saveAsFile(chartOfBCPO,iter);
        ResultsVisualization.OPTType = "PCCO";
        CategoryDataset datasetOfPCCO = ResultsVisualization.createDataset(iter);
        JFreeChart chartOfPCCO = ResultsVisualization.createChart(datasetOfPCCO);
        ResultsVisualization.saveAsFile(chartOfPCCO,iter);
    }

    public static double[] AlgorithmComparisonDigitization() {
        double[] results = new double[2];
        results[0] = (double) numOfBetterBCPOResultsOfEASW / numOfBCPOResults;
        results[1] = (double) numOfBetterPCCOResultsOfEASW / numOfPCCOResults;
        return results;
    }

    public static double[][] getAlgorithmResults(int iter){
        HSSFWorkbook workbook = null;
        HSSFSheet sheet = null;
        FileInputStream inputStream = null;
        try {
            String filePathPrefix = new File("").getCanonicalPath() + "/src/main/resources/opt_curve_data/" + iter +"/";
            if (ResultsVisualization.OPTType.equals("BCPO")) {
                String EASWFilePath = filePathPrefix + "EASW/" + perfOpt.getApp().getGraph().getAPPName() + "_EASW_BCPO.xls";
                inputStream = new FileInputStream(EASWFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("EASW_results");
                int rowNums = sheet.getLastRowNum();
                double[][] data = new double[7][rowNums];
                for (int i = 0; i <= 4; i++)
                    data[i] = new double[rowNums];
                BudgetConstraints = new String[rowNums];
                for (int i = 1; i <= rowNums; i++) {
                    numOfBCPOResults++;
                    HSSFRow ARow = sheet.getRow(i);
                    BudgetConstraints[i - 1] = ARow.getCell(5).toString();
                    if(BudgetConstraints[i-1].length() - BudgetConstraints[i-1].indexOf(".") > 4)
                        BudgetConstraints[i - 1] = BudgetConstraints[i - 1].substring(0, BudgetConstraints[i - 1].indexOf(".") + 4);
                    data[0][i - 1] = Double.valueOf(ARow.getCell(6).toString());
                }

                String PRCPFilePath = filePathPrefix + "PRCP/" + perfOpt.getApp().getGraph().getAPPName() + "_PRCP_BCPO.xls";
                inputStream = new FileInputStream(PRCPFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("PRCP_results");
                rowNums = sheet.getLastRowNum();
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[1][i - 1] = Double.valueOf(ARow.getCell(1).toString());
                    data[2][i - 1] = Double.valueOf(ARow.getCell(2).toString());
                    data[3][i - 1] = Double.valueOf(ARow.getCell(3).toString());
                    data[4][i - 1] = Double.valueOf(ARow.getCell(4).toString());
                }

                String UWCFilePath = filePathPrefix + "UWC/" + perfOpt.getApp().getGraph().getAPPName() + "_UWC_BCPO.xls";
                inputStream = new FileInputStream(UWCFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("UWC_results");
                rowNums = sheet.getLastRowNum();
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[5][i - 1] = Double.valueOf(ARow.getCell(1).toString());
                }

                String DFBAFilePath = filePathPrefix + "DFBA/" + perfOpt.getApp().getGraph().getAPPName() + "_DFBA_BCPO.xls";
                inputStream = new FileInputStream(DFBAFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("DFBA_results");
                rowNums = sheet.getLastRowNum();
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[6][i - 1] = Double.valueOf(ARow.getCell(1).toString());
                    if (data[0][i - 1] <= data[1][i - 1] && data[0][i - 1] <= data[2][i - 1] && data[0][i - 1] <= data[3][i - 1] &&
                            data[0][i - 1] <= data[4][i - 1] && data[0][i - 1] <= data[5][i - 1] && data[0][i - 1] <= data[6][i - 1])
                        numOfBetterBCPOResultsOfEASW++;
                }

                workbook = new HSSFWorkbook();
                sheet = workbook.createSheet("AllAlgorithmResults");
                HSSFRow head = sheet.createRow(0);
                head.createCell(0).setCellValue("Budget Constraint");
                head.createCell(1).setCellValue("EASW");
                head.createCell(2).setCellValue("Without BCR");
                head.createCell(3).setCellValue("BCR RT/M");
                head.createCell(4).setCellValue("BCR ERT/C");
                head.createCell(5).setCellValue("BCR MAX");
                head.createCell(6).setCellValue("UWC");
                head.createCell(7).setCellValue("DFBA");

                for(int i=0;i<rowNums;i++){
                    HSSFRow aRow = sheet.createRow(i+1);
                    aRow.createCell(0).setCellValue(BudgetConstraints[i]);
                    aRow.createCell(1).setCellValue(data[0][i]);
                    aRow.createCell(2).setCellValue(data[1][i]);
                    aRow.createCell(3).setCellValue(data[2][i]);
                    aRow.createCell(4).setCellValue(data[3][i]);
                    aRow.createCell(5).setCellValue(data[4][i]);
                    aRow.createCell(6).setCellValue(data[5][i]);
                    aRow.createCell(7).setCellValue(data[6][i]);
                }
                FileOutputStream outputStream = new FileOutputStream(new File(filePathPrefix + "AllAlgorithmResults_"+
                        perfOpt.getApp().getGraph().getAPPName() + "_BCPO.xls"));
                workbook.write(outputStream);
                outputStream.close();

                return data;
            } else if (ResultsVisualization.OPTType.equals("PCCO")) {
                String EASWFilePath = filePathPrefix + "EASW/" + perfOpt.getApp().getGraph().getAPPName() + "_EASW_PCCO.xls";
                inputStream = new FileInputStream(EASWFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("EASW_results");
                int rowNums = sheet.getLastRowNum();
                double[][] data = new double[7][rowNums];
                for (int i = 0; i <= 6; i++)
                    data[i] = new double[rowNums];
                PerformanceConstraints = new String[rowNums];
                for (int i = 1; i <= rowNums; i++) {
                    numOfPCCOResults++;
                    HSSFRow ARow = sheet.getRow(i);
                    PerformanceConstraints[i - 1] = ARow.getCell(5).toString();
                    if(PerformanceConstraints[i-1].length() - PerformanceConstraints[i-1].indexOf(".") > 5)
                        PerformanceConstraints[i - 1] = PerformanceConstraints[i - 1].substring(0, PerformanceConstraints[i - 1].indexOf(".") + 5);
                    data[0][i - 1] = Double.valueOf(ARow.getCell(7).toString());
                }

                String PRCPFilePath = filePathPrefix + "PRCP/" + perfOpt.getApp().getGraph().getAPPName() + "_PRCP_PCCO.xls";
                inputStream = new FileInputStream(PRCPFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("PRCP_results");
                rowNums = sheet.getLastRowNum();
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[1][i - 1] = Double.valueOf(ARow.getCell(5).toString());
                    data[2][i - 1] = Double.valueOf(ARow.getCell(6).toString());
                    data[3][i - 1] = Double.valueOf(ARow.getCell(7).toString());
                    data[4][i - 1] = Double.valueOf(ARow.getCell(8).toString());
                }

                String UWCFilePath = filePathPrefix + "UWC/" + perfOpt.getApp().getGraph().getAPPName() + "_UWC_PCCO.xls";
                inputStream = new FileInputStream(UWCFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("UWC_results");
                rowNums = sheet.getLastRowNum();
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[5][i - 1] = Double.valueOf(ARow.getCell(2).toString());
                }

                String DFBAFilePath = filePathPrefix + "DFBA/" + perfOpt.getApp().getGraph().getAPPName() + "_DFBA_PCCO.xls";
                inputStream = new FileInputStream(DFBAFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("DFBA_results");
                rowNums = sheet.getLastRowNum();
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[6][i - 1] = Double.valueOf(ARow.getCell(2).toString());
                    if (data[0][i - 1] <= data[1][i - 1] && data[0][i - 1] <= data[2][i - 1] && data[0][i - 1] <= data[3][i - 1] &&
                            data[0][i - 1] <= data[4][i - 1] && data[0][i - 1] <= data[5][i - 1] && data[0][i - 1] <= data[6][i - 1])
                        numOfBetterPCCOResultsOfEASW++;
                }

                workbook = new HSSFWorkbook();
                sheet = workbook.createSheet("AllAlgorithmResults");
                HSSFRow head = sheet.createRow(0);
                head.createCell(0).setCellValue("Performance Constarint");
                head.createCell(1).setCellValue("EASW");
                head.createCell(2).setCellValue("Without BCR");
                head.createCell(3).setCellValue("BCR RT/M");
                head.createCell(4).setCellValue("BCR ERT/C");
                head.createCell(5).setCellValue("BCR MAX");
                head.createCell(6).setCellValue("UWC");
                head.createCell(7).setCellValue("DFBA");

                for(int i=0;i<rowNums;i++){
                    HSSFRow aRow = sheet.createRow(i+1);
                    aRow.createCell(0).setCellValue(PerformanceConstraints[i]);
                    aRow.createCell(1).setCellValue(data[0][i]);
                    aRow.createCell(2).setCellValue(data[1][i]);
                    aRow.createCell(3).setCellValue(data[2][i]);
                    aRow.createCell(4).setCellValue(data[3][i]);
                    aRow.createCell(5).setCellValue(data[4][i]);
                    aRow.createCell(6).setCellValue(data[5][i]);
                    aRow.createCell(7).setCellValue(data[6][i]);
                }
                FileOutputStream outputStream = new FileOutputStream(new File(filePathPrefix + "AllAlgorithmResults_" +
                        perfOpt.getApp().getGraph().getAPPName() +"_PCCO.xls"));
                workbook.write(outputStream);
                outputStream.close();

                return data;
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return null;
    }
}
