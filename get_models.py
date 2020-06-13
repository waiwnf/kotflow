import argparse
import os   

import tensorflow as tf
import torch
import torchvision


ASSETS_PATH = os.path.join('app', 'src', 'main', 'assets')


def make_mobilenet_tf(out_path):
    if os.path.exists(out_path):
        print('Tensorflow model already exists at {}.'.format(out_path))
        return
    model = tf.keras.applications.MobileNetV2(input_shape=(224, 224, 3), weights='imagenet')
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    with open(out_path, 'wb') as f:
        f.write(tflite_model)
    print('Wrote Tensorflow model to {}.'.format(out_path))


def make_mobilenet_pytorch(out_path):
    if os.path.exists(out_path):
        print('PyTorch model already exists at {}.'.format(out_path))
        return
    model = torchvision.models.mobilenet_v2(pretrained=True)
    model.eval()
    example = torch.rand(1, 3, 224, 224)
    traced_script_module = torch.jit.trace(model, example)
    traced_script_module.save(out_path)
    print('Wrote PyTorch model to {}.'.format(out_path))


if __name__ == '__main__':
    os.makedirs(ASSETS_PATH, exist_ok=True)
    make_mobilenet_tf(os.path.join(ASSETS_PATH, 'mobilenet-v2.tflite'))
    make_mobilenet_pytorch(os.path.join(ASSETS_PATH, 'mobilenet-v2.pt'))