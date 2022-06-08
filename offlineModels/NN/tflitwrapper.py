import os
import numpy as np
import tensorflow as tf

# IMG_SIZE = 28  # 224
# NUM_FEATURES = 84  # 7 * 7 * 1280
# NUM_CLASSES = 4

NUM_FEATURES = 15
NUM_CLASSES = 6


class TransferLearningModel(tf.Module):
    """TF Transfer Learning model class."""

    def __init__(self, model, learning_rate=0.001):
        """Initializes a transfer learning model instance.

    Args:
      learning_rate: A learning rate for the optimzer.
    """
        self.num_features = NUM_FEATURES
        self.num_classes = NUM_CLASSES

        # trainable weights and bias for softmax
        self.ws = tf.Variable(
            tf.zeros((self.num_features, self.num_classes)),
            name='ws',
            trainable=True)
        self.bs = tf.Variable(
            tf.zeros((1, self.num_classes)), name='bs', trainable=True)

        # base model
        self.base = model
        # loss function and optimizer
        self.loss_fn = tf.keras.losses.CategoricalCrossentropy()
        self.optimizer = tf.keras.optimizers.Adam(learning_rate=learning_rate)

    @tf.function(input_signature=[
        tf.TensorSpec([None, NUM_FEATURES], tf.float32),
    ])
    def load(self, feature):
        """Generates and loads bottleneck features from the given image batch.

    Args:
      feature: A tensor of image feature batch to generate the bottleneck from.

    Returns:
      Map of the bottleneck.
    """
        # x = tf.keras.applications.mobilenet_v2.preprocess_input(tf.multiply(feature, 255))
        bottleneck = tf.reshape(self.base(feature, training=False), (-1, self.num_features))
        return {'bottleneck': bottleneck}

    @tf.function(input_signature=[
        tf.TensorSpec([None, NUM_FEATURES], tf.float32),
        tf.TensorSpec([None, NUM_CLASSES], tf.float32),
    ])
    def train(self, bottleneck, label):
        """Runs one training step with the given bottleneck features and labels.

    Args:
      bottleneck: A tensor of bottleneck features generated from the base model.
      label: A tensor of class labels for the given batch.

    Returns:
      Map of the training loss.
    """
        with tf.GradientTape() as tape:
            logits = tf.matmul(bottleneck, self.ws) + self.bs
            prediction = tf.nn.softmax(logits)
            loss = self.loss_fn(prediction, label)
        gradients = tape.gradient(loss, [self.ws, self.bs])
        self.optimizer.apply_gradients(zip(gradients, [self.ws, self.bs]))
        result = {'loss': loss}
        for grad in gradients:
            result[grad.name] = grad
        return result

    @tf.function(input_signature=[
        tf.TensorSpec([None, NUM_FEATURES], tf.float32)
    ])
    def infer(self, feature):
        """Invokes an inference on the given feature.

    Args:
      feature: A tensor of image feature batch to invoke an inference on.

    Returns:
      Map of the softmax output.
    """
        # x = tf.keras.applications.mobilenet_v2.preprocess_input(tf.multiply(feature, 255))
        bottleneck = tf.reshape(self.base(feature, training=False), (-1, self.num_features))
        logits = tf.matmul(bottleneck, self.ws) + self.bs
        return {'output': tf.nn.softmax(logits)}

    @tf.function(input_signature=[
        tf.TensorSpec(shape=[], dtype=tf.string)
    ])
    def save(self, checkpoint_path):
        """Saves the trainable weights to the given checkpoint file.

    Args:
      checkpoint_path: A file path to save the model.

    Returns:
      Map of the checkpoint file path.
    """
        tensor_names = [self.ws.name, self.bs.name]
        tensors_to_save = [self.ws.read_value(), self.bs.read_value()]
        tf.raw_ops.Save(
            filename=checkpoint_path,
            tensor_names=tensor_names,
            data=tensors_to_save,
            name='save')
        return {'checkpoint_path': checkpoint_path}

    @tf.function(input_signature=[
        tf.TensorSpec(shape=[], dtype=tf.string)
    ])
    def restore(self, checkpoint_path):
        """Restores the serialized trainable weights from the given checkpoint file.

    Args:
      checkpoint_path: A path to a saved checkpoint file.

    Returns:
      Map of restored weight and bias.
    """
        restored_tensors = {}
        restored = tf.raw_ops.Restore(
            file_pattern=checkpoint_path,
            tensor_name=self.ws.name,
            dt=np.float32,
            name='restore')
        self.ws.assign(restored)
        restored_tensors['ws'] = restored
        restored = tf.raw_ops.Restore(
            file_pattern=checkpoint_path,
            tensor_name=self.bs.name,
            dt=np.float32,
            name='restore')
        self.bs.assign(restored)
        restored_tensors['bs'] = restored
        return restored_tensors

    @tf.function(input_signature=[])
    def initialize_weights(self):
        """Initializes the weights and bias of the head model.

    Returns:
      Map of initialized weight and bias.
    """
        self.ws.assign(tf.random.uniform((self.num_features, self.num_classes)))
        self.bs.assign(tf.random.uniform((1, self.num_classes)))
        return {'ws': self.ws, 'bs': self.bs}
