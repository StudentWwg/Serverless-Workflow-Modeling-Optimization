package EASW;

import serverlessWorkflow.graph.WVertex;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;
import util.DataStoreTools;

import java.util.*;

public class EASW_SAL_OPT {
    private int N_GENES;
    private double CROSS_RATE;
    private int N_GENERATIONS;
    private double ETA_M_;
    private double MUTATIONRATE;
    private ArrayList<Chromosome> chromosomes;
    private PerfOpt perfOpt;


    private HashMap<WVertex, HashMap<Integer, Double>> costProfile = new HashMap<>();

    public double getCROSS_RATE() {
        return CROSS_RATE;
    }

    public double getMUTATIONRATE() {
        return MUTATIONRATE;
    }

    public double getETA_M_() {
        return ETA_M_;
    }

    public ArrayList<Chromosome> getChromosomes() {
        return chromosomes;
    }

    public PerfOpt getPerfOpt() {
        return perfOpt;
    }

    public EASW_SAL_OPT(int N_GENES, double CROSS_RATE, double MUTATION_RATE, int N_GENERATIONS,
                         double ETA_M_, PerfOpt perfOpt) {
        this.N_GENES = N_GENES;
        this.CROSS_RATE = CROSS_RATE;
        this.MUTATIONRATE = MUTATION_RATE;
        this.N_GENERATIONS = N_GENERATIONS;
        this.ETA_M_ = ETA_M_;
        this.perfOpt = perfOpt;
        Chromosome.setPerfOpt(this.perfOpt);
        this.chromosomes = new ArrayList<>();

        Set<WVertex> vertices = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet();
        for (WVertex aVertex : vertices) {
            this.costProfile.put(aVertex, new HashMap<>());
            for (int mem : aVertex.getPerf_profile().keySet())
                this.costProfile.get(aVertex).put(mem, perfOpt.getApp().GetVertexCostInMem(aVertex, mem));
        }
    }

    public long EASWSearch(double[] constraintList, String OPTType, int iter) {
        Chromosome.setOPTType(OPTType);
        ArrayList<EASW_Result> results = new ArrayList<>();
        long EASWStartTime = System.currentTimeMillis();
        Chromosome lastBestChromosome = null;
        for (int numOfConstraint = 0; numOfConstraint < constraintList.length; numOfConstraint++) {
            WVertex[] vertices = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
            this.chromosomes.clear();
            for (int i = 0; i < this.N_GENES; i++)
                this.chromosomes.add(new Chromosome(i + 1, vertices));
            if (lastBestChromosome != null)
                this.chromosomes.set(new Random().nextInt(this.chromosomes.size()), lastBestChromosome);

            double constraint = constraintList[numOfConstraint];
            int iteration = 0;
            int bestIndex = 0;
            while (iteration < this.N_GENERATIONS) {
                double[] fitness = new double[this.chromosomes.size()];
                for (int i = 0; i < this.chromosomes.size(); i++) {
                    this.chromosomes.get(i).TranslateDNA();
                    this.chromosomes.get(i).GetFitness(constraint);
                    fitness[i] = this.chromosomes.get(i).getFitness();
                }

                bestIndex = getBestOffspring(fitness, Chromosome.getOPTType(), constraint);

                if (iteration == (this.N_GENERATIONS - 1) && Chromosome.getOPTType().equals("BCPO")) {
                    System.out.printf("Repeatable times: %d, Budget_constraint: %f ms,  best fit: %f,  time: %f ms, cost: %f USD\n",iter, constraint, fitness[bestIndex],
                            this.chromosomes.get(bestIndex).getRt(), this.chromosomes.get(bestIndex).getCost());
                } else if (iteration == (this.N_GENERATIONS - 1) && Chromosome.getOPTType().equals("PCCO")) {
                    System.out.printf("Repeatable times: %d, Performance_constraint: %f ms,  best fit: %f,  time: %f ms, cost: %f USD\n",iter, constraint, fitness[bestIndex],
                            this.chromosomes.get(bestIndex).getRt(), this.chromosomes.get(bestIndex).getCost());
                }

                this.Evolve(constraint);
                iteration++;
            }
            lastBestChromosome = this.chromosomes.get(bestIndex);

            System.out.print("The iteration is over! Optimized Memory Configuration: ");
            TreeMap<WVertex, Integer> best_mem_config = this.chromosomes.get(bestIndex).getMemConfig();
            for (WVertex vertex : best_mem_config.keySet()) {
                System.out.print(vertex.toString() + " : " + best_mem_config.get(vertex) + "  ");
            }
            System.out.println();
            results.add(new EASW_Result(this.N_GENES, this.CROSS_RATE, this.MUTATIONRATE, this.N_GENERATIONS, this.ETA_M_, constraint,
                    this.chromosomes.get(bestIndex).getRt(), this.chromosomes.get(bestIndex).getCost(), this.chromosomes.get(bestIndex).getFitness(),
                    this.chromosomes.get(bestIndex).getMemConfig()));
        }
        long EASWEndTime = System.currentTimeMillis();

        DataStoreTools.EASWDataStore(results, this, iter);
        return (EASWEndTime - EASWStartTime) / constraintList.length;
    }

    public void Evolve(double constraint) {
        for (int i = 0; i < this.chromosomes.size(); i++) {
            this.chromosomes.get(i).CrossOver(this, constraint);
            this.chromosomes.get(i).Mutate(this, constraint);
        }
    }

    public int getBestOffspring(double[] fitness, String OPTType, double constarint) {
        double maxValue = fitness[0];
        int bestIndex = 0;
        for (int i = 0; i < fitness.length; i++) {
            if (fitness[i] > maxValue) {
                maxValue = fitness[i];
                bestIndex = i;
            } else if (fitness[i] == maxValue) {
                if (OPTType.equals("BCPO") && this.chromosomes.get(bestIndex).getCost() < constarint &&
                        this.chromosomes.get(i).getCost() < this.chromosomes.get(bestIndex).getCost())
                    bestIndex = i;
                else if (OPTType.equals("PCCO") && this.chromosomes.get(bestIndex).getRt() < constarint &&
                        this.chromosomes.get(i).getRt() < this.chromosomes.get(bestIndex).getRt())
                    bestIndex = i;
            }
        }
        return bestIndex;
    }
}