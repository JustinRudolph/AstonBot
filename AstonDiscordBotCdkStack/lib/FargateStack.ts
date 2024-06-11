import { Construct } from 'constructs';
import { CfnOutput, RemovalPolicy, Stack, StackProps, } from 'aws-cdk-lib';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as AWS from 'aws-sdk';

interface FargateStackProps extends StackProps {
    containerPort: number;
    vpcId: string;
    repositoryUri: string;
    appEnv: string;
    albSecurityGroup: ec2.SecurityGroup;
    targetGroup: elbv2.ApplicationTargetGroup;
}

export class FargateStack extends Stack {

    private props: FargateStackProps;
    private vpc: ec2.IVpc;
    private fargateService: ecs.FargateService;
    private cluster: ecs.Cluster;
    private taskDefinition: ecs.TaskDefinition;

    constructor(scope: Construct, id: string, props: FargateStackProps) {
        super(scope, id, props);
        this.props = props;

        // Initialize asynchronously
        this.initialize().then(() => {
            console.log('Initialization complete');
        }).catch(error => {
            console.error('Initialization failed', error);
            throw error;
        });
    }

    private async initialize(): Promise<void> {
        this.vpc = ec2.Vpc.fromLookup(this, 'Vpc', { vpcId: this.props.vpcId });
        this.cluster = new ecs.Cluster(this, this.props.appEnv, { vpc: this.vpc });

        this.taskDefinition = await this.createTaskDefinition();
        this.createFargateService();

        // github actions uses these values to trigger a redeployment
        new CfnOutput(this, 'ClusterName', { value: this.cluster.clusterName });
        new CfnOutput(this, 'ServiceName', { value: this.fargateService.serviceName });
    }

    /**
     * Creates the task definition that is used to launch a service
     * Sets the memory/cpu and where the docker image is stored, adds port mappings
     */
    private async createTaskDefinition(): Promise<ecs.FargateTaskDefinition> {
        const executionRole = new iam.Role(this, 'ExecutionRole', {
            assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
            description: 'Role that the ECS container agent and the Docker daemon can assume',
        });
        executionRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy'));

        // todo: should eventually be more granular
        const taskRole = new iam.Role(this, 'TaskRole', {
            assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
            description: 'Role that the application can use to access AWS services',
        });
        taskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('SecretsManagerReadWrite'));

        const taskDefinition = new ecs.FargateTaskDefinition(this, 'FargateTaskDefinition', {
            memoryLimitMiB: 2048,
            cpu: 1024,
            executionRole: executionRole,
            taskRole: taskRole
        });

        const logGroupName = `/aws/ecs/${this.props.appEnv}`;
        const logGroup = await this.createLogGroup(logGroupName);

        const containerDefinition = taskDefinition.addContainer('Container', {
            image: ecs.ContainerImage.fromRegistry(this.props.repositoryUri),
            logging: ecs.LogDrivers.awsLogs({
                logGroup: logGroup,
                streamPrefix: 'app-logs'
            }),
        });

        containerDefinition.addPortMappings({
            containerPort: this.props.containerPort,
            protocol: ecs.Protocol.TCP,
            name: `${this.props.stackName}-${this.props.containerPort}-tcp`,
            appProtocol: ecs.AppProtocol.http,
        });

        return taskDefinition;
    }

    private async createLogGroup(logGroupName: string): Promise<logs.ILogGroup> {
        const cloudwatchlogs = new AWS.CloudWatchLogs();

        try {
            await cloudwatchlogs.describeLogGroups({ logGroupNamePrefix: logGroupName }).promise();
            console.log(`Log group ${logGroupName} already exists.`);
            return logs.LogGroup.fromLogGroupName(this, 'ExistingLogGroup', logGroupName);
        } catch (error: any) {
            if (error.code === 'ResourceNotFoundException') {
                console.log(`Creating new log group ${logGroupName}.`);
                return new logs.LogGroup(this, 'LogGroup', {
                    logGroupName: logGroupName,
                    removalPolicy: this.props.terminationProtection ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY,
                    retention: this.props.terminationProtection ? logs.RetentionDays.SIX_MONTHS : logs.RetentionDays.ONE_WEEK,
                });
            }
            throw error;
        }
    }

    /**
     * Generate the fargate service from the task definition in the created cluster
     * Create a security group that allows traffic on application port
     */
    private createFargateService(): void {

        this.fargateService = new ecs.FargateService(this, 'FargateService', {
            cluster: this.cluster,
            desiredCount: 1,
            assignPublicIp: true,
            taskDefinition: this.taskDefinition,
            securityGroups: [this.getFargateServiceSecurityGroup()],
            deploymentController: {
                type: ecs.DeploymentControllerType.ECS,
            },
            circuitBreaker: {
                rollback: true, // Automatically roll back on failure
            },
        });

        this.fargateService.attachToApplicationTargetGroup(this.props.targetGroup);
    }

    private getFargateServiceSecurityGroup() {

        const fargateServiceSecurityGroup = new ec2.SecurityGroup(this, 'FargateServiceSecurityGroup', {
            vpc: this.vpc,
            description: 'Allow traffic from ALB on port 443',
            allowAllOutbound: true,
        });

        fargateServiceSecurityGroup.addIngressRule(
            this.props.albSecurityGroup,
            ec2.Port.tcp(443),
            'Allow inbound HTTPS traffic from the ALB'
        );
        return fargateServiceSecurityGroup;
    }
}