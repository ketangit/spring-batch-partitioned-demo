## Spring Batch Remote Partition demo 
```
Can run locally, Cloud Foundry and Kubenetes  
```

#### Running locally
 ```
* Run from IDE -> PartitionBatchApplication
 ```

#### Local skaffold build and deploy
```
mvn clean package
skaffold build
kubectl apply -k kube\kustomize\overlays\minikube
kubectl delete -k kube\kustomize\overlays\minikube
```

#### Useful docker/kubectl commands
```
docker ps
docker images
docker images| grep "spring-batch-partitioned-demo" | tr -s " " | cut -d " " -f 3 | xargs docker image rm -f

kubectl delete pod --field-selector=status.phase==Succeeded
kubectl delete pod --field-selector=status.phase==Pending
kubectl delete pod --field-selector=status.phase==Failed

kubectl logs -f spring-batch-partitioned-demo-ddc7cb76c
kubectl exec -it spring-batch-partitioned-demo-ddc7cb76c bash

kubectl get all

kubectl delete deployment spring-batch-partitioned-demo
```

#### Notes
 * Modified version of https://github.com/mminella/scaling-demos/tree/master/partitioned-demo
