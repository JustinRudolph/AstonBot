import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecsPatterns from 'aws-cdk-lib/aws-ecs-patterns';
import * as rds from 'aws-cdk-lib/aws-rds';

export class AstonDiscordBotCdkStackStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Create a VPC
    const vpc = new ec2.Vpc(this, 'MyVpc', {
      maxAzs: 1
    });

    // Create a Cluster
    const cluster = new ecs.Cluster(this, 'MyCluster', {
      vpc: vpc
    });

    // Create a MySQL RDS Instance
    const rdsInstance = new rds.DatabaseInstance(this, 'MyRdsInstance', {
      engine: rds.DatabaseInstanceEngine.mysql({ version: rds.MysqlEngineVersion.VER_8_0 }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.BURSTABLE2, ec2.InstanceSize.MICRO),
      vpc: vpc,
      multiAz: false,
      allocatedStorage: 20,
      maxAllocatedStorage: 100,
      publiclyAccessible: false,
      credentials: rds.Credentials.fromGeneratedSecret('admin'),
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      databaseName: "mydatabase",
    });

    // Create an ECS Task Definition
    const taskDefinition = new ecs.FargateTaskDefinition(this, 'MyTaskDef', {
      memoryLimitMiB: 512,
      cpu: 256
    });

    // Add Container to the Task Definition
    const container = taskDefinition.addContainer('MyContainer', {
      image: ecs.ContainerImage.fromRegistry('discord-bot-app'), // Replace with your Docker image
      logging: new ecs.AwsLogDriver({
        streamPrefix: 'MyApp'
      }),
      environment: {
        'DISCORD_BOT_TOKEN': 'MTI0MDM4ODk2MDk5NjU1NjgxMA.GSUHrA.Yzblrjl17UJQAwfnZx0ErVFThI7TaZCl_nCVOM',
        'DB_HOST': rdsInstance.dbInstanceEndpointAddress,
        'DB_PORT': rdsInstance.dbInstanceEndpointPort,
        'DB_NAME': 'mydatabase',
        'DB_USER': 'admin',
        'DB_PASSWORD': rdsInstance.secret!.secretValueFromJson('password').toString()
      }
    });

    // Create an ECS Service
    const fargateService = new ecsPatterns.ApplicationLoadBalancedFargateService(this, 'MyFargateService', {
      cluster: cluster,
      taskDefinition: taskDefinition,
      publicLoadBalancer: true,
      listenerPort: 443,
      desiredCount: 1,
    });

    // Configure Security Group for the Fargate service to allow inbound traffic
    fargateService.service.connections.securityGroups[0].addIngressRule(
        ec2.Peer.anyIpv4(),
        ec2.Port.tcp(443),
        'Allow Discord Bot traffic'
    );

    // Allow the Fargate service to access the RDS instance
    rdsInstance.connections.allowDefaultPortFrom(fargateService.service);
  }
}
