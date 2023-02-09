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
    private static String[] BCPOrowKeys = {"GA", "Without BCR", "BCR RT/M", "BCR ERT/C", "BCR MAX", "UWC"};  //BCPO中的5条折线
    private static String[] PCCOrowKeys = {"GA", "Without BCR", "BCR M/RT", "BCR C/ERT", "BCR MAX", "UWC"}; //PCCO中的5条折线
    private static int numOfBCPOResults = 0;
    private static int numOfPCCOResults = 0;
    private static int numOfBetterBCPOResultsOfCPOGA = 0;
    private static int numOfBetterPCCOResultsOfCPOGA = 0;

    public static CategoryDataset createDataset(int repeatedTimes) {
        if (ResultsVisualization.OPTType.equals("BCPO")) {
            double[][] data = ResultsVisualization.getAlgorithmResults(repeatedTimes);
            return DatasetUtils.createCategoryDataset(ResultsVisualization.BCPOrowKeys, ResultsVisualization.BudgetConstraints, data);
        } else if (ResultsVisualization.OPTType.equals("PCCO")) {
            double[][] data = ResultsVisualization.getAlgorithmResults(repeatedTimes);
            return DatasetUtils.createCategoryDataset(ResultsVisualization.PCCOrowKeys, ResultsVisualization.PerformanceConstraints, data);
        }else return null;
    }

    public static JFreeChart createChart(CategoryDataset categoryDataset) {
        // 创建JFreeChart对象：ChartFactory.createLineChart
        JFreeChart jfreechart = null;
        if (ResultsVisualization.OPTType.equals("BCPO")) {
            jfreechart = ChartFactory.createLineChart("The difference of performance under budget constraint between CPOGA, PRCP in  four strategies and UWC", // 标题
                    "Budget Constraint in USD (per 1 Million Executions)", // categoryAxisLabel （category轴，横轴，X轴标签）
                    "End-to-end Response time in ms", // valueAxisLabel（value轴，纵轴，Y轴的标签）
                    categoryDataset, // dataset
                    PlotOrientation.VERTICAL, true, // legend
                    false, // tooltips
                    false); // URLs
        } else if (ResultsVisualization.OPTType.equals("PCCO")) {
            jfreechart = ChartFactory.createLineChart("The difference of cost under performance constraint between CPOGA, PRCP in  four strategies and UWC", // 标题
                    "Performance Constraint in ms", // categoryAxisLabel （category轴，横轴，X轴标签）
                    "Cost per 1 Million Executions in USD", // valueAxisLabel（value轴，纵轴，Y轴的标签）
                    categoryDataset, // dataset
                    PlotOrientation.VERTICAL, true, // legend
                    false, // tooltips
                    false); // URLs
        }
        // 使用CategoryPlot设置各种参数。以下设置可以省略。
        CategoryPlot plot = (CategoryPlot) jfreechart.getPlot();
        //设置背景颜色
        plot.setBackgroundPaint(Color.white);
        // 背景色 透明度
        plot.setBackgroundAlpha(0.5f);
        // 前景色 透明度
        plot.setForegroundAlpha(0.5f);
        // 其他设置 参考 CategoryPlot类
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultShapesVisible(true); // series 点（即数据点）可见
        renderer.setDefaultLinesVisible(true); // series 点（即数据点）间有连线可见
        renderer.setUseSeriesOffset(true); // 设置偏移量
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(false);
        return jfreechart;
    }

    public static void saveAsFile(JFreeChart chart, int repeatedTimes) {
        FileOutputStream out = null;
        try {
            String filePathPrefix = new File("").getCanonicalPath() + "/src/main/resources/opt_curve_pictures/"+repeatedTimes+"/";
            String outputPath = filePathPrefix + perfOpt.getApp().getGraph().getAPPName() + "_Optimization_Curve_" + ResultsVisualization.OPTType + ".jpeg";
            File outFile = new File(outputPath);
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(outputPath);
            // 保存为PNG
            // ChartUtilities.writeChartAsPNG(out, chart, 600, 400);
            // 保存为JPEG
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

    public static void DrawPictures(PerfOpt perfOpt, int repeatedTimes) {
        ResultsVisualization.perfOpt = perfOpt;
        ResultsVisualization.OPTType = new String("BCPO");
        CategoryDataset datasetOfBCPO = ResultsVisualization.createDataset(repeatedTimes);
        JFreeChart chartOfBCPO = ResultsVisualization.createChart(datasetOfBCPO);
        ResultsVisualization.saveAsFile(chartOfBCPO,repeatedTimes);
        ResultsVisualization.OPTType = new String("PCCO");
        CategoryDataset datasetOfPCCO = ResultsVisualization.createDataset(repeatedTimes);
        JFreeChart chartOfPCCO = ResultsVisualization.createChart(datasetOfPCCO);
        ResultsVisualization.saveAsFile(chartOfPCCO,repeatedTimes);
    }

    public static double[] AlgorithmComparisonDigitization() {
        double[] results = new double[2];
        results[0] = (double) numOfBetterBCPOResultsOfCPOGA / numOfBCPOResults;
        results[1] = (double) numOfBetterPCCOResultsOfCPOGA / numOfPCCOResults;
        return results;
    }

    public static double[][] getAlgorithmResults(int repeatedTimes){
        HSSFWorkbook workbook = null;
        HSSFSheet sheet = null;
        FileInputStream inputStream = null;
        try {
            String filePathPrefix = new File("").getCanonicalPath() + "/src/main/resources/opt_curve_data/"+repeatedTimes+"/";
            if (ResultsVisualization.OPTType.equals("BCPO")) {
                String GAFilePath = filePathPrefix + "CPOGA/" + perfOpt.getApp().getGraph().getAPPName() + "_CPOGA_BCPO.xls";
                inputStream = new FileInputStream(GAFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("CPOGA_results");
                int rowNums = sheet.getLastRowNum();  //一共6行，返回值为5。0是第一行的索引，5是最后一行的索引。  rowNums是有效行的个数
                double[][] data = new double[6][rowNums];
                for (int i = 0; i <= 4; i++)
                    data[i] = new double[rowNums];
                BudgetConstraints = new String[rowNums];  //横坐标数据，即约束
                for (int i = 1; i <= rowNums; i++) {
                    numOfBCPOResults++;
                    HSSFRow ARow = sheet.getRow(i);
                    BudgetConstraints[i - 1] = ARow.getCell(5).toString(); //索引为5的列是budgetConstraint
                    if(BudgetConstraints[i-1].length() - BudgetConstraints[i-1].indexOf(".") > 4)
                        BudgetConstraints[i - 1] = BudgetConstraints[i - 1].substring(0, BudgetConstraints[i - 1].indexOf(".") + 4);  //保留三位小数
                    data[0][i - 1] = Double.valueOf(ARow.getCell(6).toString());  //data[0]存储GA算法的结果(rt)
                }

                String PRCPFilePath = filePathPrefix + "PRCP/" + perfOpt.getApp().getGraph().getAPPName() + "_PRCP_BCPO.xls";
                inputStream = new FileInputStream(PRCPFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("PRCP_results");
                rowNums = sheet.getLastRowNum();  //有效数据行数
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[1][i - 1] = Double.valueOf(ARow.getCell(1).toString());   //data[1]存储without BCR策略的结果
                    data[2][i - 1] = Double.valueOf(ARow.getCell(2).toString());  //data[2]存储BCR RT/M策略的结果
                    data[3][i - 1] = Double.valueOf(ARow.getCell(3).toString());  //data[3]存储BCR ERT/C策略的结果
                    data[4][i - 1] = Double.valueOf(ARow.getCell(4).toString());  //data[4]存储BCR MAX策略的结果
                }

                String UWCFilePath = filePathPrefix + "UWC/" + perfOpt.getApp().getGraph().getAPPName() + "_UWC_BCPO.xls";
                inputStream = new FileInputStream(UWCFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("UWC_results");
                rowNums = sheet.getLastRowNum();  //有效数据行数
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[5][i - 1] = Double.valueOf(ARow.getCell(1).toString());   //data[6]存储UWC的结果
                    if (data[0][i - 1] <= data[1][i - 1] && data[0][i - 1] <= data[2][i - 1] && data[0][i - 1] <= data[3][i - 1] && data[0][i - 1] <= data[4][i - 1] && data[0][i - 1] <= data[5][i - 1])
                        numOfBetterBCPOResultsOfCPOGA++;
                }

                /* 结果写入excel文件*/
                workbook = new HSSFWorkbook();
                sheet = workbook.createSheet("AllAlgorithmResults");
                HSSFRow head = sheet.createRow(0);
                head.createCell(0).setCellValue("Budget Constraint");
                head.createCell(1).setCellValue("CPOGA");
                head.createCell(2).setCellValue("Without BCR");
                head.createCell(3).setCellValue("BCR RT/M");
                head.createCell(4).setCellValue("BCR ERT/C");
                head.createCell(5).setCellValue("BCR MAX");
                head.createCell(6).setCellValue("UWC");

                for(int i=0;i<rowNums;i++){
                    HSSFRow aRow = sheet.createRow(i+1);
                    aRow.createCell(0).setCellValue(BudgetConstraints[i]);
                    aRow.createCell(1).setCellValue(data[0][i]);
                    aRow.createCell(2).setCellValue(data[1][i]);
                    aRow.createCell(3).setCellValue(data[2][i]);
                    aRow.createCell(4).setCellValue(data[3][i]);
                    aRow.createCell(5).setCellValue(data[4][i]);
                    aRow.createCell(6).setCellValue(data[5][i]);
                }
                FileOutputStream outputStream = new FileOutputStream(new File(filePathPrefix + "AllAlgorithmResults_"+ perfOpt.getApp().getGraph().getAPPName() + "_BCPO.xls"));
                workbook.write(outputStream);
                outputStream.close();

                return data;
            } else if (ResultsVisualization.OPTType.equals("PCCO")) {
                String GAFilePath = filePathPrefix + "CPOGA/" + perfOpt.getApp().getGraph().getAPPName() + "_CPOGA_PCCO.xls";
                inputStream = new FileInputStream(GAFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("CPOGA_results");
                int rowNums = sheet.getLastRowNum();  //一共6行，返回值为5。0是第一行的索引，5是最后一行的索引。  rowNums是有效行的个数
                double[][] data = new double[6][rowNums];
                for (int i = 0; i <= 4; i++)
                    data[i] = new double[rowNums];
                PerformanceConstraints = new String[rowNums];  //横坐标数据，即约束
                for (int i = 1; i <= rowNums; i++) {
                    numOfPCCOResults++;
                    HSSFRow ARow = sheet.getRow(i);
                    PerformanceConstraints[i - 1] = ARow.getCell(5).toString(); //索引为5的列是performanceConstraint
                    if(PerformanceConstraints[i-1].length() - PerformanceConstraints[i-1].indexOf(".") > 4)  //小数点后不止3位
                        PerformanceConstraints[i - 1] = PerformanceConstraints[i - 1].substring(0, PerformanceConstraints[i - 1].indexOf(".") + 4);
                    data[0][i - 1] = Double.valueOf(ARow.getCell(7).toString());  //data[0]存储GA算法的结果(cost)
                }

                String PRCPFilePath = filePathPrefix + "PRCP/" + perfOpt.getApp().getGraph().getAPPName() + "_PRCP_PCCO.xls";
                inputStream = new FileInputStream(PRCPFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("PRCP_results");
                rowNums = sheet.getLastRowNum();  //有效数据行数
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[1][i - 1] = Double.valueOf(ARow.getCell(5).toString());   //data[1]存储without BCR策略的结果
                    data[2][i - 1] = Double.valueOf(ARow.getCell(6).toString());  //data[2]存储BCR RT/M策略的结果
                    data[3][i - 1] = Double.valueOf(ARow.getCell(7).toString());  //data[3]存储BCR ERT/C策略的结果
                    data[4][i - 1] = Double.valueOf(ARow.getCell(8).toString());  //data[4]存储BCR MAX策略的结果
                }

                String UWCFilePath = filePathPrefix + "UWC/" + perfOpt.getApp().getGraph().getAPPName() + "_UWC_PCCO.xls";
                inputStream = new FileInputStream(UWCFilePath);
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheet("UWC_results");
                rowNums = sheet.getLastRowNum();  //有效数据行数
                for (int i = 1; i <= rowNums; i++) {
                    HSSFRow ARow = sheet.getRow(i);
                    data[5][i - 1] = Double.valueOf(ARow.getCell(2).toString());   //data[1]存储UWC的结果
                    if (data[0][i - 1] <= data[1][i - 1] && data[0][i - 1] <= data[2][i - 1] && data[0][i - 1] <= data[3][i - 1] && data[0][i - 1] <= data[4][i - 1] && data[0][i - 1] <= data[5][i - 1])
                        numOfBetterPCCOResultsOfCPOGA++;
                }

                /* 结果写入excel文件*/
                workbook = new HSSFWorkbook();
                sheet = workbook.createSheet("AllAlgorithmResults");
                HSSFRow head = sheet.createRow(0);
                head.createCell(0).setCellValue("Performance Constarint");
                head.createCell(1).setCellValue("CPOGA");
                head.createCell(2).setCellValue("Without BCR");
                head.createCell(3).setCellValue("BCR RT/M");
                head.createCell(4).setCellValue("BCR ERT/C");
                head.createCell(5).setCellValue("BCR MAX");
                head.createCell(6).setCellValue("UWC");

                for(int i=0;i<rowNums;i++){
                    HSSFRow aRow = sheet.createRow(i+1);
                    aRow.createCell(0).setCellValue(PerformanceConstraints[i]);
                    aRow.createCell(1).setCellValue(data[0][i]);
                    aRow.createCell(2).setCellValue(data[1][i]);
                    aRow.createCell(3).setCellValue(data[2][i]);
                    aRow.createCell(4).setCellValue(data[3][i]);
                    aRow.createCell(5).setCellValue(data[4][i]);
                    aRow.createCell(6).setCellValue(data[5][i]);
                }
                FileOutputStream outputStream = new FileOutputStream(new File(filePathPrefix + "AllAlgorithmResults_" + perfOpt.getApp().getGraph().getAPPName() +"_PCCO.xls"));
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
