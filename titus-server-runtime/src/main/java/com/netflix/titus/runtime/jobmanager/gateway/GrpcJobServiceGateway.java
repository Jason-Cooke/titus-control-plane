/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.titus.runtime.jobmanager.gateway;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.protobuf.Empty;
import com.netflix.titus.api.jobmanager.model.CallMetadata;
import com.netflix.titus.grpc.protogen.Job;
import com.netflix.titus.grpc.protogen.JobAttributesDeleteRequest;
import com.netflix.titus.grpc.protogen.JobAttributesUpdate;
import com.netflix.titus.grpc.protogen.JobCapacityUpdate;
import com.netflix.titus.grpc.protogen.JobChangeNotification;
import com.netflix.titus.grpc.protogen.JobDescriptor;
import com.netflix.titus.grpc.protogen.JobDisruptionBudgetUpdate;
import com.netflix.titus.grpc.protogen.JobId;
import com.netflix.titus.grpc.protogen.JobManagementServiceGrpc;
import com.netflix.titus.grpc.protogen.JobProcessesUpdate;
import com.netflix.titus.grpc.protogen.JobQuery;
import com.netflix.titus.grpc.protogen.JobQueryResult;
import com.netflix.titus.grpc.protogen.JobStatusUpdate;
import com.netflix.titus.grpc.protogen.ObserveJobsQuery;
import com.netflix.titus.grpc.protogen.TaskAttributesDeleteRequest;
import com.netflix.titus.grpc.protogen.TaskAttributesUpdate;
import com.netflix.titus.grpc.protogen.TaskId;
import com.netflix.titus.grpc.protogen.TaskKillRequest;
import com.netflix.titus.grpc.protogen.TaskMoveRequest;
import com.netflix.titus.grpc.protogen.TaskQuery;
import com.netflix.titus.grpc.protogen.TaskQueryResult;
import com.netflix.titus.runtime.connector.GrpcRequestConfiguration;
import com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil;
import com.netflix.titus.runtime.endpoint.metadata.CallMetadataResolver;
import com.netflix.titus.runtime.endpoint.metadata.V3HeaderInterceptor;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;

import static com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil.createMonoVoidRequest;
import static com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil.createRequestCompletable;
import static com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil.createRequestObservable;
import static com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil.createSimpleClientResponseObserver;
import static com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil.createWrappedStub;

/**
 * {@link JobServiceGateway} implementation that connects to TitusMaster over the GRPC channel.
 */
@Singleton
public class GrpcJobServiceGateway implements JobServiceGateway {

    private final JobManagementServiceGrpc.JobManagementServiceStub client;
    private final CallMetadataResolver callMetadataResolver;
    private final GrpcRequestConfiguration configuration;

    @Inject
    public GrpcJobServiceGateway(JobManagementServiceGrpc.JobManagementServiceStub client,
                                 CallMetadataResolver callMetadataResolver,
                                 GrpcRequestConfiguration configuration) {
        this.client = client;
        this.callMetadataResolver = callMetadataResolver;
        this.configuration = configuration;
    }

    @Override
    public Observable<String> createJob(JobDescriptor jobDescriptor, CallMetadata callMetadata) {
        return createRequestObservable(emitter -> {
            StreamObserver<JobId> streamObserver = GrpcUtil.createClientResponseObserver(
                    emitter,
                    jobId -> emitter.onNext(jobId.getId()),
                    emitter::onError,
                    emitter::onCompleted
            );
            V3HeaderInterceptor.attachCallMetadata(client, callMetadata)
                    .withDeadlineAfter(configuration.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
                    .createJob(jobDescriptor, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Completable updateJobCapacity(JobCapacityUpdate jobCapacityUpdate) {
        return createRequestCompletable(emitter -> {
            StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).updateJobCapacity(jobCapacityUpdate, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Completable updateJobProcesses(JobProcessesUpdate jobProcessesUpdate) {
        return createRequestCompletable(emitter -> {
            StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).updateJobProcesses(jobProcessesUpdate, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Completable updateJobStatus(JobStatusUpdate statusUpdate) {
        return createRequestCompletable(emitter -> {
            StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).updateJobStatus(statusUpdate, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Mono<Void> updateJobDisruptionBudget(JobDisruptionBudgetUpdate request) {
        return createMonoVoidRequest(
                emitter -> {
                    StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientMonoResponse(emitter);
                    createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).updateJobDisruptionBudget(request, streamObserver);
                },
                configuration.getRequestTimeoutMs()
        ).ignoreElement().cast(Void.class);
    }

    @Override
    public Mono<Void> updateJobAttributes(JobAttributesUpdate request) {
        return createMonoVoidRequest(
                emitter -> {
                    StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientMonoResponse(emitter);
                    createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).updateJobAttributes(request, streamObserver);
                },
                configuration.getRequestTimeoutMs()
        ).ignoreElement().cast(Void.class);
    }

    @Override
    public Mono<Void> deleteJobAttributes(JobAttributesDeleteRequest request) {
        return createMonoVoidRequest(
                emitter -> {
                    StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientMonoResponse(emitter);
                    createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).deleteJobAttributes(request, streamObserver);
                },
                configuration.getRequestTimeoutMs()
        ).ignoreElement().cast(Void.class);
    }

    @Override
    public Observable<Job> findJob(String jobId) {
        Observable<Job> observable = createRequestObservable(emitter -> {
            StreamObserver<Job> streamObserver = createSimpleClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).findJob(JobId.newBuilder().setId(jobId).build(), streamObserver);
        }, configuration.getRequestTimeoutMs());
        return observable.timeout(configuration.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<JobQueryResult> findJobs(JobQuery jobQuery) {
        return createRequestObservable(emitter -> {
            StreamObserver<JobQueryResult> streamObserver = createSimpleClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).findJobs(jobQuery, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Observable<JobChangeNotification> observeJob(String jobId) {
        return createRequestObservable(emitter -> {
            StreamObserver<JobChangeNotification> streamObserver = createSimpleClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver).observeJob(JobId.newBuilder().setId(jobId).build(), streamObserver);
        });
    }

    @Override
    public Observable<JobChangeNotification> observeJobs(ObserveJobsQuery query) {
        return createRequestObservable(emitter -> {
            StreamObserver<JobChangeNotification> streamObserver = createSimpleClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver).observeJobs(query, streamObserver);
        });
    }

    @Override
    public Completable killJob(String jobId) {
        return createRequestCompletable(emitter -> {
            StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).killJob(JobId.newBuilder().setId(jobId).build(), streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Observable<com.netflix.titus.grpc.protogen.Task> findTask(String taskId) {
        Observable<com.netflix.titus.grpc.protogen.Task> observable = createRequestObservable(emitter -> {
            StreamObserver<com.netflix.titus.grpc.protogen.Task> streamObserver = createSimpleClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).findTask(TaskId.newBuilder().setId(taskId).build(), streamObserver);
        }, configuration.getRequestTimeoutMs());
        return observable.timeout(configuration.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<TaskQueryResult> findTasks(TaskQuery taskQuery) {
        Observable<TaskQueryResult> observable = createRequestObservable(emitter -> {
            StreamObserver<TaskQueryResult> streamObserver = createSimpleClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).findTasks(taskQuery, streamObserver);
        }, configuration.getRequestTimeoutMs());
        return observable.timeout(configuration.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Completable killTask(TaskKillRequest taskKillRequest) {
        return createRequestCompletable(emitter -> {
            StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).killTask(taskKillRequest, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Completable updateTaskAttributes(TaskAttributesUpdate attributesUpdate) {
        return createRequestCompletable(emitter -> {
            StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).updateTaskAttributes(attributesUpdate, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Completable deleteTaskAttributes(TaskAttributesDeleteRequest deleteRequest) {
        return createRequestCompletable(emitter -> {
            StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).deleteTaskAttributes(deleteRequest, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }

    @Override
    public Completable moveTask(TaskMoveRequest taskMoveRequest) {
        return createRequestCompletable(emitter -> {
            StreamObserver<Empty> streamObserver = GrpcUtil.createEmptyClientResponseObserver(emitter);
            createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeoutMs()).moveTask(taskMoveRequest, streamObserver);
        }, configuration.getRequestTimeoutMs());
    }
}
