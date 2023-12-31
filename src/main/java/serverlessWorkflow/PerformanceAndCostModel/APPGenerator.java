package serverlessWorkflow.PerformanceAndCostModel;

import serverlessWorkflow.graph.WVertex;

import java.util.Random;

public class APPGenerator {
    private int[] mem_list;
    private int seed;
    private int[] model_type;
    private int[] m1_a;
    private double[] m1_b;
    private int[] m1_c;
    private double[] m1_d;
    private double[] m2_k;
    private double[] m2_b;

    public APPGenerator(int seed, String type, int[] mem_list) {
        this.seed = seed;
        if (mem_list == null)
            mem_list = new int[]{192, 256, 320, 384, 448, 512, 576, 640, 704, 768,
                    832, 896, 960, 1024, 1088, 1152, 1216, 1280, 1344, 1408, 1472, 1536,
                    1600, 1664, 1728, 1792, 1856, 1920, 1984, 2048, 2112, 2176, 2240, 2304,
                    2368, 2432, 2496, 2560, 2624, 2688, 2752, 2816, 2880, 2944, 3008};
        else {
            this.mem_list = new int[mem_list.length];
            for (int i = 0; i < mem_list.length; i++)
                this.mem_list[i] = mem_list[i];
        }
        Random rand = new Random();
        rand.setSeed(this.seed);
        this.model_type = new int[100];
        if (type.equals("mixed")) {
            for (int i = 0; i < model_type.length; i++)
                model_type[i] = rand.nextInt(3);
        } else if (type.equals("linear")) {
            for (int i = 0; i < model_type.length; i++)
                model_type[i] = 1;
        } else if (type.equals("4PL")) {
            for (int i = 0; i < model_type.length; i++)
                model_type[i] = 2;
        }
        this.m1_a = new int[100];
        for (int i = 0; i < m1_a.length; i++)
            this.m1_a[i] = rand.nextInt(1000) + 1000;
        this.m1_b = new double[100];
        for (int i = 0; i < m1_b.length; i++)
            this.m1_b[i] = rand.nextDouble() * 2 + 0.1;
        this.m1_c = new int[100];
        for (int i = 0; i < m1_c.length; i++)
            this.m1_c[i] = rand.nextInt(450) + 50;
        this.m1_d = new double[100];
        for (int i = 0; i < m1_d.length; i++)
            m1_d[i] = (rand.nextDouble() * 0.5 + 0.01) * m1_a[i];
        this.m2_k = new double[100];
        for (int i = 0; i < m2_k.length; i++)
            m2_k[i] = rand.nextDouble() - 1;
        this.m2_b = new double[100];
        for (int i = 0; i < m2_b.length; i++)
            m2_b[i] = Math.abs(3000 * m2_k[i]) + (double) rand.nextInt(900) + 100;
    }

    public void get_rt_mem_data(int num, WVertex[] vertexs) {
        for (int n = 1; n <= num; n++) {
            double ydata = 1.0;
            for (int i = 0; i < this.mem_list.length; i++) {
                if (this.model_type[n] == 2)
                    ydata = ((double) this.m1_a[n] - this.m1_d[n]) / (1.0 + Math.pow(this.mem_list[i] / (double) this.m1_c[n], this.m1_b[n])) + this.m1_d[n];
                else if (this.model_type[n] == 1)
                    ydata = this.m2_k[n] * this.mem_list[i] + this.m2_b[n];
                vertexs[n - 1].getPerf_profile().put(this.mem_list[i], ydata);
            }
        }
    }
}

