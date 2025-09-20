# GitOps Bootstrap (Argo CD and Flux)

This folder provides example bootstrap manifests to deploy the Envoy Gateway overlays in `infra/gateway/overlays/{dev,stg,prod}` using either Argo CD or Flux.

> Prerequisites: install Argo CD (namespace `argocd`) or Flux (namespace `flux-system`) in your cluster.

## Argo CD

Apply applications for each environment:
```bash
kubectl apply -n argocd -f infra/gitops/argocd/
```
The Applications sync the Kustomize overlays from this repo (`main` branch) into the `edge` namespace with automated prune/self‑heal enabled.

## Flux

Bootstrap the GitRepository and Kustomizations:
```bash
kubectl apply -f infra/gitops/flux/gitrepository.yaml
kubectl apply -f infra/gitops/flux/kustomization-dev.yaml
kubectl apply -f infra/gitops/flux/kustomization-stg.yaml
kubectl apply -f infra/gitops/flux/kustomization-prod.yaml
```
Flux will reconcile the repo every minute and apply the overlays to `edge`. `gateway-stg` depends on `gateway-dev`; `gateway-prod` depends on `gateway-stg`.

## Notes
- Overlays set hostnames/certs and OIDC URLs per environment; adjust as needed.
- Ensure the `edge` namespace and TLS cert secrets exist, and that Gateway/Envoy CRDs are installed.
- Combine with CI checks and GitOps approval gates for safe promotion.

