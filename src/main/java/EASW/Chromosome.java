package EASW;

import serverlessWorkflow.graph.WVertex;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;

import java.util.*;

public class Chromosome {
    private int ID;
    private double rt;
    private double cost;
    private double fitness;
    private TreeMap<WVertex, Integer> memConfig;
    private static PerfOpt perfOpt;
    private static String OPTType;

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

    public TreeMap<WVertex, Integer> getMemConfig() {
        return memConfig;
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

    public Chromosome(int i, WVertex[] vertices) {
        this.ID = i;
        this.fitness = -1;
        this.rt = this.cost = Double.MAX_VALUE;
        if (vertices != null)
            this.GenerateMemConfig(vertices);
    }

    public void GenerateMemConfig(WVertex[] vertices) {
        this.memConfig = new TreeMap<>();
        Random rand = new Random();
        double randNum = rand.nextDouble(1);
        if (randNum <= 0.5) {
            if(OPTType.equals("BCPO")){
                TreeMap<WVertex,Integer> memConfiguration = perfOpt.getMinimumCostConfiguration();
                for(Map.Entry<WVertex,Integer> item : memConfiguration.entrySet())
                    memConfig.put(item.getKey(), item.getValue());
            }else if(OPTType.equals("PCCO")){
                TreeMap<WVertex,Integer> memConfiguration = perfOpt.getBestPerformanceConfiguration();
                for(Map.Entry<WVertex,Integer> item : memConfiguration.entrySet())
                    memConfig.put(item.getKey(), item.getValue());
            }
        }else {
            for (int i = 0; i < vertices.length; i++)
                memConfig.put(vertices[i], rand.nextInt(192,10241));
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
        this.fitness = this.fitness * 10000;
    }

    public void TranslateDNA() {
        Chromosome.perfOpt.update_App_workflow_mem_rt_cost(this.memConfig);
        this.cost = Chromosome.perfOpt.getApp().GetAverageCost();
        this.rt = Chromosome.perfOpt.getApp().GetAverageRT();
    }

    public void CrossOver(EASW_SAL_OPT EASW_sal_opt, double constraint) {
        Random rand = new Random();
        double crossOverRate = rand.nextDouble(1);
        if (crossOverRate <= EASW_sal_opt.getCROSS_RATE()) {
            int mom;
            do {
                mom = rand.nextInt(EASW_sal_opt.getChromosomes().size());
            } while (mom == (this.ID - 1));

            Chromosome child1 = new Chromosome(this.ID, null);
            child1.memConfig = new TreeMap<WVertex, Integer>();
            Chromosome child2 = new Chromosome(this.ID, null);
            child2.memConfig = new TreeMap<WVertex, Integer>();
            for (WVertex vertex : this.memConfig.keySet())
                child1.memConfig.put(vertex, this.memConfig.get(vertex));
            for (WVertex vertex : EASW_sal_opt.getChromosomes().get(mom).memConfig.keySet())
                child2.memConfig.put(vertex, EASW_sal_opt.getChromosomes().get(mom).memConfig.get(vertex));

            Integer[] memOfDad = this.memConfig.values().toArray(new Integer[0]);
            Integer[] memOfMom = EASW_sal_opt.getChromosomes().get(mom).memConfig.values().toArray(new Integer[0]);
            WVertex[] verticesOfChild1 = child1.memConfig.keySet().toArray(new WVertex[0]);
            WVertex[] verticesOfChild2 = child2.memConfig.keySet().toArray(new WVertex[0]);

            int crossPointLeft = rand.nextInt(child1.memConfig.size());
            int crossPointRight = rand.nextInt(child1.memConfig.size());
            if (crossPointLeft > crossPointRight) {
                int swapTemp = crossPointLeft;
                crossPointLeft = crossPointRight;
                crossPointRight = swapTemp;
            }

            for (int i = crossPointLeft; i <= crossPointRight; i++) {
                child1.memConfig.replace(verticesOfChild1[i], memOfMom[i]);
                child2.memConfig.replace(verticesOfChild2[i], memOfDad[i]);
            }

            child1.TranslateDNA();
            child1.getFitness();
            child1.isExcellent(child2, constraint);
            this.isExcellent(child1, constraint);
        }
    }

    public void Mutate(EASW_SAL_OPT EASW_sal_opt, double constraint) {
        Chromosome child = new Chromosome(-1, null);
        child.memConfig = new TreeMap<WVertex, Integer>();
        for (WVertex vertex : this.memConfig.keySet())
            child.memConfig.put(vertex, this.memConfig.get(vertex));
        WVertex[] vertices = child.memConfig.keySet().toArray(new WVertex[0]);
        for (int i = 0; i < vertices.length; i++) {
            double rate = new Random().nextDouble(1);
            if (rate <= EASW_sal_opt.getMUTATIONRATE()) {
                int leftBound = 192;
                TreeMap<WVertex,Integer> bestConfig = perfOpt.getBestPerformanceConfiguration();
                int rightBound = bestConfig.get(vertices[i]);

                int newGene = this.Polynomial_mutation(vertices[i], vertices[i].getMem(), leftBound, rightBound, constraint, EASW_sal_opt);
                child.memConfig.replace(vertices[i], newGene);
            }
        }
        this.isExcellent(child, constraint);
    }

    public int Polynomial_mutation(WVertex vertex, int y, int left, int right, double constraint, EASW_SAL_OPT EASW_sal_opt) {
        double delta1 = ((double) y - left) / (right - left), delta2 = ((double) right - y) / (right - left);
        double u = 0;
        double mut_pow = 1.0 / (EASW_sal_opt.getETA_M_() + 1.0);
        double maxPerturbance = right - left;
        double xy, val, deltaq;
        int newMem = y;
        int num = 0;
        for (; num < 10; num++) {
            if ((Chromosome.OPTType.equals("PCCO") && this.rt < constraint) || (Chromosome.OPTType.equals("BCPO") && this.cost >= constraint))
                u = new Random().nextDouble(0.5);
            else if ((Chromosome.OPTType.equals("PCCO") && this.rt >= constraint) || (Chromosome.OPTType.equals("BCPO") && this.cost >= constraint))
                u = new Random().nextDouble(0.5) + 0.5;

            if (u <= 0.5) {
                xy = 1.0 - delta1;
                val = 2.0 * u + (1.0 - 2.0 * u) * Math.pow(xy, (1.0 + EASW_sal_opt.getETA_M_()));
                deltaq = Math.pow(val, mut_pow) - 1.0;
                newMem = (int) (y + deltaq * maxPerturbance);
            } else {
                xy = 1.0 - delta2;
                val = 2.0 * (1.0 - u) + 2.0 * (u - 0.5) * Math.pow(xy, (1.0 + EASW_sal_opt.getETA_M_()));
                deltaq = 1.0 - Math.pow(val, mut_pow);
                newMem = (int) (y + deltaq * maxPerturbance);
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