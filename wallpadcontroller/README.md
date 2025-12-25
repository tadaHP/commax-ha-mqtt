# Wallpad Controller

## image build


```bash
./gradlew build
docker build -t wallpad-controller .
docker tag wallpad-controller:latest ghcr.io/tadahp/commax-wallpad:latest
docker push ghcr.io/tadahp/commax-wallpad:latest
```