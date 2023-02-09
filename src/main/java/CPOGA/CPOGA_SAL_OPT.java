package CPOGA;

import serverlessWorkflow.graph.WVertex;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;
import util.DataStoreTools;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Vector;

public class CPOGA_SAL_OPT {
    private int N_GENES;  //基因个数
    private double CROSS_RATE;  //交叉概率
    private double MUTATE_RATE;  //变异概率
    private int N_GENERATIONS;  //迭代次数
    private double ETA_M_;  // 用于多项式变异 该参数值越小，基因变异的范围就越大
    private int BUDGET_NUMBER;
    private int PERFORMANCE_NUMBER;
    private ArrayList<Chromosome> genes;
    private PerfOpt perfOpt;

    public int getN_GENES() {
        return N_GENES;
    }

    public void setN_GENES(int n_GENES) {
        N_GENES = n_GENES;
    }

    public double getCROSS_RATE() {
        return CROSS_RATE;
    }

    public void setCROSS_RATE(double CROSS_RATE) {
        this.CROSS_RATE = CROSS_RATE;
    }

    public double getMUTATE_RATE() {
        return MUTATE_RATE;
    }

    public void setMUTATE_RATE(double MUTATE_RATE) {
        this.MUTATE_RATE = MUTATE_RATE;
    }

    public int getN_GENERATIONS() {
        return N_GENERATIONS;
    }

    public void setN_GENERATIONS(int n_GENERATIONS) {
        N_GENERATIONS = n_GENERATIONS;
    }

    public double getETA_M_() {
        return ETA_M_;
    }

    public void setETA_M_(double ETA_M_) {
        this.ETA_M_ = ETA_M_;
    }

    public int getBUDGET_NUMBER() {
        return BUDGET_NUMBER;
    }

    public void setBUDGET_NUMBER(int BUDGET_NUMBER) {
        this.BUDGET_NUMBER = BUDGET_NUMBER;
    }

    public int getPERFORMANCE_NUMBER() {
        return PERFORMANCE_NUMBER;
    }

    public void setPERFORMANCE_NUMBER(int PERFORMANCE_NUMBER) {
        this.PERFORMANCE_NUMBER = PERFORMANCE_NUMBER;
    }

    public ArrayList<Chromosome> getGenes() {
        return genes;
    }

    public void setGenes(ArrayList<Chromosome> genes) {
        this.genes = genes;
    }

    public PerfOpt getPerfOpt() {
        return perfOpt;
    }

    public void setPerfOpt(PerfOpt perfOpt) {
        this.perfOpt = perfOpt;
    }

    public CPOGA_SAL_OPT(int N_GENES, double CROSS_RATE, double MUTATE_RATE, int N_GENERATIONS,
                         double ETA_M_, int BUDGET_NUMBER, int PERFORMANCE_NUMBER, PerfOpt perfOpt) {
        this.N_GENES = N_GENES;
        this.CROSS_RATE = CROSS_RATE;
        this.MUTATE_RATE = MUTATE_RATE;
        this.N_GENERATIONS = N_GENERATIONS;
        this.ETA_M_ = ETA_M_;
        this.BUDGET_NUMBER = BUDGET_NUMBER;
        this.PERFORMANCE_NUMBER = PERFORMANCE_NUMBER;
        this.perfOpt = perfOpt;
        Chromosome.setPerfOpt(this.perfOpt);
        this.genes = new ArrayList<>();
    }

    public long CPOGASearch(double[] constraintList, String OPTType, int repeatedTimes) {
        Chromosome.setOPTType(OPTType);
        ArrayList<CPOGA_Result> results = new ArrayList<>();  //保存每一次迭代的结果
        long CPOGAStartTime = System.currentTimeMillis();
        for (int numOfConstraint = 0; numOfConstraint < constraintList.length; numOfConstraint++) {
            WVertex[] vertices = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]); //每次都重新生成gene'
            this.genes.clear();
            for (int i = 0; i < this.N_GENES; i++) {  //初始化染色体
                this.genes.add(new Chromosome(i + 1, vertices));
            }

            double constraint = constraintList[numOfConstraint];
            int iter = 0;
            int bestIndex = 0; // 最优基因的索引
            while (iter < this.N_GENERATIONS) {
                double[] fitness = new double[this.genes.size()];
                for (int i = 0; i < this.genes.size(); i++) {
                    this.genes.get(i).TranslateDNA();
                    this.genes.get(i).GetFitness(constraint);
                    fitness[i] = this.genes.get(i).getFitness();
                } //for

                bestIndex = getBestOffspring(fitness, Chromosome.getOPTType(), constraint);

                if (Chromosome.getOPTType().equals("BCPO")) {
                    System.out.printf("Repeated times : %d, Gen: %d,  budget_constraint: %f,  best fit: %f,  time: %f, cost: %f\n",
                            repeatedTimes, iter, constraint, fitness[bestIndex], this.genes.get(bestIndex).getRt(), this.genes.get(bestIndex).getCost());
                } else if (Chromosome.getOPTType().equals("PCCO")) {
                    System.out.printf("Repeated times : %d, Gen: %d,  performance_constraint: %f,  best fit: %f,  time: %f, cost: %f\n",
                            repeatedTimes, iter, constraint, fitness[bestIndex], this.genes.get(bestIndex).getRt(), this.genes.get(bestIndex).getCost());
                }

                this.Evolve(constraint);
                iter++;
            }// while

            System.out.print("The iteration is over! Optimized Memory Configuration: ");
            TreeMap<WVertex, Integer> best_mem_config = this.genes.get(bestIndex).getMemConfig();
            for (WVertex vertex : best_mem_config.keySet()) {
                System.out.print(vertex.toString() + " : " + best_mem_config.get(vertex) + "  ");
            }
            results.add(new CPOGA_Result(this.N_GENES, this.CROSS_RATE, this.MUTATE_RATE, this.N_GENERATIONS, this.ETA_M_, constraint,
                    this.genes.get(bestIndex).getRt(), this.genes.get(bestIndex).getCost(), this.genes.get(bestIndex).getFitness(),
                    this.genes.get(bestIndex).getMemConfig()));
        }//for
        long CPOGAEndTime = System.currentTimeMillis();

        DataStoreTools.CPOGADataStore(results, this, repeatedTimes);
        return (CPOGAEndTime-CPOGAStartTime) / constraintList.length;
    }

    public void Evolve(double constraint) {
        for (int i = 0; i < this.genes.size(); i++) {
            this.genes.get(i).CrossOver(this, constraint);
            this.genes.get(i).Mutate(this, constraint);
        }
    }

    public int getBestOffspring(double[] fitness, String OPTType, double constarint) {
        double maxValue = fitness[0];
        int bestIndex = 0;
        for (int i = 0; i < fitness.length; i++) {
            if (fitness[i] > maxValue) {
                maxValue = fitness[i];
                bestIndex = i;  //bestIndex是拥有最大适应度的基因在genes中的索引
            } else if (fitness[i] == maxValue) {
                if (OPTType.equals("BCPO") && this.genes.get(bestIndex).getCost() < constarint &&
                        this.genes.get(i).getCost() < this.genes.get(bestIndex).getCost())
                    bestIndex = i;
                else if (OPTType.equals("PCCO") && this.genes.get(bestIndex).getRt() < constarint &&
                        this.genes.get(i).getRt() < this.genes.get(bestIndex).getRt())
                    bestIndex = i;
            }
        }
        return bestIndex;
    }
}