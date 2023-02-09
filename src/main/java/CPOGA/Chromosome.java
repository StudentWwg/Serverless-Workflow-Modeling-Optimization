package CPOGA;

import serverlessWorkflow.graph.WVertex;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;

import java.util.Random;
import java.util.TreeMap;

public class Chromosome {
    private int ID; //基因编号
    private double rt;
    private double cost;
    private double fitness;
    private TreeMap<WVertex, Integer> memConfig;
    private static PerfOpt perfOpt;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public double getRt() {
        return rt;
    }

    public void setRt(double rt) {
        this.rt = rt;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public TreeMap<WVertex, Integer> getMemConfig() {
        return memConfig;
    }

    public void setMemConfig(TreeMap<WVertex, Integer> memConfig) {
        this.memConfig = memConfig;
    }

    public static PerfOpt getPerfOpt() {
        return perfOpt;
    }

    public static void setPerfOpt(PerfOpt perfOpt) {
        Chromosome.perfOpt = perfOpt;
    }

    public static String getOPTType() {
        return OPTType;
    }

    public static void setOPTType(String OPTType) {
        Chromosome.OPTType = OPTType;
    }

    private static String OPTType;



    public Chromosome(int i, WVertex[] vertices) {
        this.ID = i;
        this.fitness = -1;
        this.rt = this.cost = Double.MAX_VALUE;
        if (vertices != null)
            this.GenerateMemConfig(vertices);  //根据问题生成两种不同的极端内存配置方案
    }

    public void GenerateMemConfig(WVertex[] vertices) {  //生成各个结点的初始mem
        this.memConfig = new TreeMap<>();
        Random rand = new Random();
        double randNum = rand.nextDouble(1);
        if (randNum <= 0.5) {    //随机生成两种极端情况的内容配置
            for (int i = 0; i < vertices.length; i++)
                memConfig.put(vertices[i], 192);
        } else {
            for (int i = 0; i < vertices.length; i++)
                memConfig.put(vertices[i], 3008);
        }
    }

    public void GetFitness(double constraint) {
        if (Chromosome.OPTType.equals("BCPO")) {
            if (this.cost <= constraint)
                this.fitness = 1 / this.rt;
            else
                this.fitness = (-1) * this.cost / constraint;
        } else if (Chromosome.OPTType.equals("PCCO")) {
            if (this.rt <= constraint)
                this.fitness = 1 / this.cost;
            else
                this.fitness = (-1) * this.rt / constraint;
        }
        this.fitness = this.fitness * 10000;  //转换成可观测值
    }

    public void TranslateDNA() {
        Chromosome.perfOpt.update_App_workflow_mem_rt_cost(this.memConfig);
        this.cost = Chromosome.perfOpt.getApp().GetAverageCost();
        this.rt = Chromosome.perfOpt.getApp().GetAverageRT();
    }

    public void CrossOver(CPOGA_SAL_OPT CPOGA_sal_opt, double constraint) {
        Random rand = new Random();
        double crossOverRate = rand.nextDouble(1);
        if(crossOverRate <= CPOGA_sal_opt.getCROSS_RATE()){
            int mom;
            do{
                mom = rand.nextInt(CPOGA_sal_opt.getGenes().size());
            }while (mom == (this.ID-1));

            Chromosome child = new Chromosome(this.ID, null);
            child.memConfig = new TreeMap<WVertex, Integer>();
            for (WVertex vertex : this.memConfig.keySet())
                child.memConfig.put(vertex, this.memConfig.get(vertex));  //让两个基因保存个结点的Integer对象不同

            Integer[] memOfMom = CPOGA_sal_opt.getGenes().get(mom).memConfig.values().toArray(new Integer[0]);
            WVertex[] vertices = child.memConfig.keySet().toArray(new WVertex[0]);

            int crossPointLeft = rand.nextInt(child.memConfig.size());  //交换片段的最左端
            int crossPointRight = rand.nextInt(child.memConfig.size());  //交换片段的最右端
            if (crossPointLeft > crossPointRight) {  //确保最左端的index小于等于最右端的index
                int swapTemp = crossPointLeft;
                crossPointLeft = crossPointRight;
                crossPointRight = swapTemp;
            }

            for (int i = crossPointLeft; i <= crossPointRight; i++) {
                child.memConfig.replace(vertices[i], memOfMom[i]);
            }
            this.isExcellent(child, constraint);
        }
    }

    public void Mutate(CPOGA_SAL_OPT CPOGA_sal_opt, double constraint) {
        Chromosome child = new Chromosome(-1, null);
        child.memConfig = new TreeMap<WVertex, Integer>();
        for (WVertex vertex : this.memConfig.keySet())
            child.memConfig.put(vertex, this.memConfig.get(vertex));
        Integer[] mems = child.memConfig.values().toArray(new Integer[0]);
        WVertex[] vertices = child.memConfig.keySet().toArray(new WVertex[0]);
        for (int i = 0; i < vertices.length; i++) {
            double rate = new Random().nextDouble(1);
            if (rate <= CPOGA_sal_opt.getMUTATE_RATE()) {
                int leftBound = 192;
                int rightBound = 3008;
                int newGene = this.Polynomial_mutation(vertices[i], mems[i], leftBound, rightBound, constraint, CPOGA_sal_opt);
                child.memConfig.replace(vertices[i], newGene);
            }
        }
        this.isExcellent(child, constraint);  //子代更优秀就进行进化
    }

    public int Polynomial_mutation(WVertex vertex, int y, int left, int right, double constraint, CPOGA_SAL_OPT CPOGA_sal_opt) {
        double delta1 = ((double) y - left) / (right - left), delta2 = ((double) right - y) / (right - left);
        double u = 0;  // u > 0.5  δ>0  mem增大   u < 0.5  δ<0  mem减小
        double mut_pow = 1.0 / (CPOGA_sal_opt.getETA_M_() + 1.0);
        double xy, val, deltaq;
        int newMem = y;
        int num = 0;
        for (; num < 10; num++) {
            if ((Chromosome.OPTType.equals("PCCO")) && this.rt < constraint || (Chromosome.OPTType.equals("BCPO") && this.cost >= constraint))
                u = new Random().nextDouble(0.5);
            else if ((Chromosome.OPTType.equals("PCCO") && this.rt >= constraint) || (Chromosome.OPTType.equals("BCPO") && this.cost < constraint))
                u = new Random().nextDouble(0.5) + 0.5;

            if (u < 0.5) {
                xy = 1.0 - delta1;
                val = 2.0 * u + (1.0 - 2.0 * u) * Math.pow(xy, (1.0 + CPOGA_sal_opt.getETA_M_()));
                deltaq = Math.pow(val, mut_pow) - 1.0;
                newMem = (int) (y + deltaq * (right - left));
            } else {
                xy = 1.0 - delta2;
                val = 2.0 * (1.0 - u) + 2.0 * (u - 0.5) * Math.pow(xy, (1.0 + CPOGA_sal_opt.getETA_M_()));
                deltaq = 1.0 - Math.pow(val, mut_pow);
                newMem = (int) (y + deltaq * (right - left));
            }
            if (newMem < left)
                newMem = left;
            if (newMem > right)
                newMem = right;

            if ((Chromosome.OPTType.equals("PCCO") && this.rt < constraint) || (Chromosome.OPTType.equals("BCPO") && this.cost >= constraint)) {
                if (perfOpt.getApp().GetVertexCostInMem(vertex, newMem) < perfOpt.getApp().GetVertexCostInMem(vertex, y))
                    break;
            } else if ((Chromosome.OPTType.equals("PCCO") && this.rt >= constraint) || (Chromosome.OPTType.equals("BCPO") && this.cost < constraint)) {
                if (vertex.getPerf_profile().get(newMem) < vertex.getPerf_profile().get(y))
                    break;
            }
        }
        if (num == 10) {
            newMem = y;
        }
        return newMem;
    }

    public void isExcellent(Chromosome child, double constraint) {
        child.TranslateDNA();
        child.GetFitness(constraint);
        if (this.fitness < child.fitness) {
            this.memConfig = child.memConfig;
            this.rt = child.rt;
            this.cost = child.cost;
            this.fitness = child.fitness;
        }
    }
}