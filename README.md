Basic playground for neural net deployment on Android.

For now, I am focusing on the performance of the basic inference loop.
Actual inference results are ignored. The timings per each 100 frames are printed to a debug log.

Lots of basic things are not handled either for now: proper captured image orientation, lifecycle management etc etc.

Before building the Android app, get the actual models:
```
pip install -r requirements.txt
CUDA_VISIBLE_DEVICES="" python get_models.py
```

Switch between TFLIte and PyTorch model by commenting / uncommenting the corresponding init `CameraFragment.onCreateView()`:
```
model = TfModel(...)
or
model = PyTorchModel(...)
```