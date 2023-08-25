# Serverless-Workflow-Modeling-Optimization
This repository contains source code and experimental results about modeling serverless workflows and optimizing allocated resources.

## Serverless Workflow

A serverless workflow is the orchestration of serverless functions. There are [3 serverless workflows](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/resources/serverless_workflow_json_files) containing sequence, choice and parallel structures in our experiments. We design the method of modeling a serverless application according to the price on AWS Lambda to be the foundation of optimization of performance and cost under constraints.

## Research Problem

Resources allocated to a serverless function lead to different performance and cost. Thus, we need to find an optimal solution providing best performance or cost under budget and performance constraints, namely BCPO(Budget-Constrainted Performance Optimization) and PCCO(Performance-Constrainted Cost Optimization).

## Algorithms

We design and implement an evolutionary algorithm [EASW](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/EASW) to optimize allocated resources of all functions in a serverless workflow under a budget constraint or a performance constraint.
Besides, We compared our optimization algorithm EASW with three algorithms, [DFBA](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/DFBA), [PRCP](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/PRCP) and [UWC](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/UWC).

## Experiments on AWS Lambda

We design and employ [22 serverless functions](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/AWSLambda/LambdaFunctions) on AWS Lambda. These functions are orchestrated to compose three serverless workflow, APP10, APP16, APP22. 
We first validate the accuracy of our performance and cost model, and the results indicate that our model can similate the execution process of serverless workflows and obtain performance and cost correctly. 
Based on our performance and cost model, we implement our optimization algorithm EASW and compared algorithms. All constraints are executed 10 times to avoid randomness. Final results prove that our algorithm have a better performance.
