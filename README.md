# Serverless-Workflow-Modeling-Optimization
This repository contains source code and experimental results about modeling serverless workflows and optimizing allocated resources.
## Experiments on AWS Lambda

We design 22 serverless functions, their code are contained in package "[src/main/java/AWSLambda/LambdaFunctions](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/AWSLambda/LambdaFunctions)". These functions are orchestrated to compose three serverless workflow, APP10, APP16, APP22. Amazon state language definition of three serverless workflows is included in "[src/main/java/AWSLambda/ServerlessAPPAmazonLanguageDefinition](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/AWSLambda/ServerlessAPPAmazonLanguageDefinition)". Code in  package "[src/main/java/AWSLambda](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/AWSLambda)" includes accessing AWS Lambda via keys of a user account, creating, invoking functions on AWS Lambda, querying execution logs of functions, invoking state machines, querying execution logs of state machines, and calculating the accuracy of  performance and cost model.

The results querying from AWS CloudWatch  of invoking functions and state machines are stored in packages under "[src/main/resources](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/resources)".

## Serverless Workflow

There are 3 serverless workflows containing sequence, choice and parallel structures in our experiments. The code included in package "[src/main/java/serverlessWorkflow](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/serverlessWorkflow)" is related to creating directed-acyclic graph through json files, specific implementation of performance and cost model.

## Our Algorithm  and Compared Optimization Algorithms

We compared our optimization algorithm EASW with three algorithms, DFBA, PRCP and UWC.

The implementation of DFBA is in package "[src/main/java/DFBA](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/DFBA)". The implementation of PRCP is in package "[src/main/java/PRCP](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/PRCP)". The implementation of UWC is in package "[src/main/java/UWC](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/UWC) ". The implementation of EASW is in package "[src/main/java/EASW](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/java/EASW)".

We run program 10 times and store optimization results of two problems, BCCO and PCCO, in package "[src/main/resources/opt_curve_data ](https://github.com/StudentWwg/Serverless-Workflow-Modeling-Optimization/tree/main/src/main/resources/opt_curve_data)".
