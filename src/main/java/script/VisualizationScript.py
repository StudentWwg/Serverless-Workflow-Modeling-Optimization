import os

import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import gridspec, ticker
import json

class Tools(object):
    def __init__(self):
        self.Prefix = "D:\\Serverless-PerformanceAndCost-Modeling-Optimization\\src\\main\\resources\\"
        self.perfProfilePrefix = self.Prefix + "AWSLambda_functions_perf_profile\\"
        self.optCurveDataPrefix = self.Prefix + "opt_curve_data\\"
        self.optCurvePictures = self.Prefix + "opt_curve_pictures\\"
        self.stateMachineInvokationRuntimeResultsPrefix = self.Prefix + "AWSLambda_StateMachine_invoke_results\\"
        self.stateMachineInvocationCostResultsPrefix = self.Prefix + "AWSLambda_functions_invoke_results_got_by_cloudwatchlog\\"
        self.PerfCostModelAccuracyPrefix = self.Prefix + "accuracy\\"
        self.pgs = 0.0000167
        self.ppi = 0.00000028

    def getLambdaFunctionCostPerfProfileCurve(self):
        files = [file for file in os.listdir(self.perfProfilePrefix)]
        available_mem_list = []
        for i in range(192, 1024, 64):
            available_mem_list.append(i)
        for i in range(1024, 2048, 128):
            available_mem_list.append(i)
        for i in range(2048, 4096, 256):
            available_mem_list.append(i)
        for i in range(4096, 10240, 512):
            available_mem_list.append(i)

        # performance comparation among all Lambda functions
        fig = plt.figure(figsize=(15, 10))
        ax = plt.subplot(111)
        ax.grid()  # 网格背景
        ax.set_xlim(192, 10240)  # 横坐标

        for file in files:
            function_perf_profile = []
            with open(self.perfProfilePrefix + "\\" + file, "r") as f:
                json_dict = json.load(f)
                for item in available_mem_list:
                    rt = json_dict[str(item)]
                    function_perf_profile.append(rt)
                ax.plot(available_mem_list, function_perf_profile, marker='o',
                        label=file.replace("_perf_profile.json", ""), linewidth=1,
                        markersize=3)

        ax.legend()
        ax.set_xlabel('Allocated Memory Size in MB')
        ax.set_ylabel('Duration in ms')
        fig.tight_layout()
        fig.savefig(os.getcwd() + '\\Functions_Perf_Curve\\App' + str(len(files)) + '_Performance_Curve', dpi=300)
        fig.savefig(os.getcwd() + '\\Functions_Perf_Curve\\App' + str(len(files)) + '_Performance_Curve.pdf')

        # cost and performance curve of each function
        for file in files:
            function_rt = []
            function_cost = []
            cost_rt_fig = plt.figure(figsize=(12, 8))
            rt_ax = plt.subplot(111)
            rt_ax.grid()  # 网格背景
            rt_ax.set_xlim(192, 10240)
            cost_ax = rt_ax.twinx()
            with open(self.perfProfilePrefix + "\\" + file, "r") as f:
                json_dict = json.load(f)
                for item in available_mem_list:
                    rt = json_dict[str(item)]
                    function_rt.append(rt)
                    cost = (rt * item * self.pgs / 1000 / 1024 + self.ppi) * 10000000
                    function_cost.append(cost)
                rt_ax.plot(available_mem_list, function_rt, color="blue", marker='x', label="Function Duration")
                cost_ax.plot(available_mem_list, function_cost, color="orange", marker="x", label="Function Cost")
                rt_ax.set_xlabel('Allocated Memory in MB')
                rt_ax.set_ylabel('Function Duration in ms')
                cost_ax.set_ylabel('Cost per 10 Million Invocations in USD')
                rt_ax.legend(loc='upper left', bbox_to_anchor=(0.06, 1))
                cost_ax.legend(loc='upper left', bbox_to_anchor=(0.06, 0.92))
                cost_rt_fig.savefig(
                    os.getcwd() + "\\Cost_Performance_Curve\\function" + file.replace("_perf_profile.json",
                                                                                      "") + '_Duration_Cost_Mem_Curve',
                    dpi=1200, bbox_inches='tight')
                cost_rt_fig.savefig(
                    os.getcwd() + "\\Cost_Performance_Curve\\function" + file.replace("_perf_profile.json",
                                                                                      "") + '_Duration_Cost_Mem_Curve.pdf',
                    bbox_inches='tight')

    def drawCostandPerformanceTrendsofF1F2F4(self):
        files = ["f1_perf_profile.json", "f2_perf_profile.json", "f4_perf_profile.json"]
        available_mem_list = []
        for i in range(192, 1024, 64):
            available_mem_list.append(i)
        for i in range(1024, 2048, 128):
            available_mem_list.append(i)
        for i in range(2048, 4096, 256):
            available_mem_list.append(i)
        for i in range(4096, 10240, 512):
            available_mem_list.append(i)

        plt.rcParams['figure.figsize'] = (23.0, 10.0)

        i = 1
        for file in files:
            if i == 1:
                rt_ax = plt.subplot(131)
            elif i == 2:
                rt_ax = plt.subplot(132)
            else:
                rt_ax = plt.subplot(133)
            plt.title("Function " + str(file).replace("_perf_profile.json", ""))
            function_rt = []
            function_cost = []
            rt_ax.grid()  # 网格背景
            rt_ax.set_xlim(192, 10240)
            cost_ax = rt_ax.twinx()
            with open(self.perfProfilePrefix + "\\" + file, "r") as f:
                json_dict = json.load(f)
                for item in available_mem_list:
                    rt = json_dict[str(item)]
                    function_rt.append(rt)
                    cost = (rt * item * self.pgs / 1000 / 1024 + self.ppi) * 10000000
                    function_cost.append(cost)
                rt_ax.plot(available_mem_list, function_rt, color="blue", marker='x', label="Function Duration")
                cost_ax.plot(available_mem_list, function_cost, color="orange", marker="x", label="Function Cost")
                if 'f1' in file:
                    rt_ax.set_ylabel('Function Duration in ms')
                if 'f4' in file:
                    cost_ax.set_ylabel('Cost per 10 Million Invocations in USD')
                rt_ax.legend(loc='upper left', bbox_to_anchor=(0.06, 1))
                cost_ax.legend(loc='upper left', bbox_to_anchor=(0.06, 0.92))

            i = i + 1

        plt.tight_layout()
        # plt.show()
        plt.savefig(os.getcwd() + "\\CostAndPerformanceTrendofF1F2F4\\CostandPerformanceTrendsofF1F2F4", dpi=300)

    def getOptCurvePictures(self):
        app_list = ["App10", "App16", "App22"]
        gs = gridspec.GridSpec(2, 4)
        # PCCO
        fig1 = plt.figure(figsize=(13, 10))
        for app in app_list:
            # 设置figure为3行4列，APP8~APP14占据每一行的(0,1)和(2,3)位置，APP16占据最后一行的(1,2)位置
            if app == "App10":
                ax = plt.subplot(gs[0, :2])
                ax.set_title("(a) APP10")
            elif app == "App16":
                ax = plt.subplot(gs[0, 2:])
                ax.set_title("(b) APP16")
            elif app == "App22":
                ax = plt.subplot(gs[1, 1:3])
                ax.set_title("(c) APP22")

            ax.grid()

            filePath = self.optCurveDataPrefix + "AllAlgorithmResults_" + app + "_BCPO.xls"
            optimizationResults = pd.read_excel(open(filePath, "rb"), sheet_name="AllAlgorithmResults")

            budgetConstraints = optimizationResults["Budget Constraint"].values
            budgetConstraints = [item / 1000 for item in budgetConstraints]
            ax.xaxis.set_major_formatter(ticker.FormatStrFormatter('%.2f'))

            EASWRt = [item / 1000 for item in optimizationResults["EASW"].values]
            ax.plot(budgetConstraints, EASWRt, marker='o', label="EASW", linewidth=1, markersize=3)

            PRCPRt = [item / 1000 for item in optimizationResults["PRCP"].values]
            ax.plot(budgetConstraints, PRCPRt, marker='o', label="PRCP", linewidth=1, markersize=3)

            UWCRt = [item / 1000 for item in optimizationResults["UWC"].values]
            ax.plot(budgetConstraints, UWCRt, marker='o', label="UWC", linewidth=1, markersize=3)

            DFBARt = [item / 1000 for item in optimizationResults["DFBA"].values]
            ax.plot(budgetConstraints, DFBARt, marker='o', label="DFBA", linewidth=1, markersize=3)

            ax.legend()
            ax.set_xlabel('Budget Constraints in USD (per 1 Thousand Invocations)')
            if app == "App10" or app == "App22":
                ax.set_ylabel('End-to-end Response time in Second')

        plt.show()
        plt.title("Budget-constrainted performance optimization of three applications")
        fig1.tight_layout()
        fig1.savefig(os.getcwd() + '\\opt_data_pictures\\' + 'BCPO_Optimization_Curve.jpg', dpi=300)

        # BCPO
        fig2 = plt.figure(figsize=(13, 10))
        for app in app_list:
            if app == "App10":
                ax = plt.subplot(gs[0, :2])
                ax.set_title("(a) APP10")
            elif app == "App16":
                ax = plt.subplot(gs[0, 2:])
                ax.set_title("(b) APP16")
            elif app == "App22":
                ax = plt.subplot(gs[1, 1:3])
                ax.set_title("(c) APP22")

            ax.grid()

            filePath = self.optCurveDataPrefix + "AllAlgorithmResults_" + app + "_PCCO.xls"
            optimizationResults = pd.read_excel(open(filePath, "rb"), sheet_name="AllAlgorithmResults")

            performanceConstraints = optimizationResults["Performance Constarint"].values
            performanceConstraints = [item / 1000 for item in performanceConstraints]
            ax.xaxis.set_major_formatter(ticker.FormatStrFormatter('%.2f'))

            EASWCost = [item / 1000 for item in optimizationResults["EASW"].values]
            ax.plot(performanceConstraints, EASWCost, marker='o', label="EASW", linewidth=1, markersize=3)

            PRCPCost = [item / 1000 for item in optimizationResults["PRCP"].values]
            ax.plot(performanceConstraints, PRCPCost, marker='o', label="PRCP", linewidth=1, markersize=3)

            UWCCost = [item / 1000 for item in optimizationResults["UWC"].values]
            ax.plot(performanceConstraints, UWCCost, marker='o', label="UWC", linewidth=1, markersize=3)

            DFBACost = [item / 1000 for item in optimizationResults["DFBA"].values]
            ax.plot(performanceConstraints, DFBACost, marker='o', label="DFBA", linewidth=1, markersize=3)

            ax.legend()
            ax.set_xlabel('Performance Constraints in Second')
            if app == "App10" or app == "App22":
                ax.set_ylabel('Cost per 1 Thousand Invocations in USD')

        plt.show()
        plt.title("Performance-constrainted cost optimization of three applications")
        fig2.tight_layout()
        fig2.savefig(os.getcwd() + '\\opt_data_pictures\\' + 'PCCO_Optimization_Curve.jpg', dpi=300)
